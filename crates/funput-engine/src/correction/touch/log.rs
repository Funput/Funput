//! The quantised record of where the fingers landed, one entry per raw key.
//!
//! A prefix buffer, not a ring: correction only ever asks about the word that just
//! ended, so a word past the key cap is dropped outright instead of keeping its tail.
//! Distances are Q8.8 integers because the log is reachable from `Session`, which
//! derives `Eq`.

use crate::correction::score;
use crate::correction::touch::{MAX_ALTERNATES, from_q8};

/// Longest word correction will look at. Past this the replay search stops paying
/// for itself, and a very long token is rarely a mistyped syllable.
pub(crate) const MAX_WORD_KEYS: usize = 10;

/// A neighbouring key the same touch could have meant.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub(crate) struct Alternate {
    pub(crate) key: char,
    pub(crate) distance: u16,
}

/// One keystroke's touch evidence as the engine keeps it.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub(crate) struct StoredTouch {
    pub(crate) typed: char,
    pub(crate) typed_distance: u16,
    pub(crate) alternates: [Alternate; MAX_ALTERNATES],
    pub(crate) count: u8,
}

impl StoredTouch {
    pub(crate) fn alternates(&self) -> &[Alternate] {
        &self.alternates[..usize::from(self.count)]
    }

    /// Match the case of the key the engine actually recorded, so replaying an
    /// alternate reproduces `Nhà` from `Nhsf` rather than dropping the capital.
    fn recase(&mut self, raw_key: char) {
        if !raw_key.is_uppercase() {
            return;
        }
        self.typed = self.typed.to_ascii_uppercase();
        for alternate in &mut self.alternates {
            alternate.key = alternate.key.to_ascii_uppercase();
        }
    }
}

/// The touch behind every key of the live word, in typing order.
///
/// `usable` is the honesty flag: anything that rewrites the raw keys behind the
/// engine's back clears it, and [`TouchLog::matches`] then refuses the word. The
/// failure mode is a missing correction, never a wrong one.
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct TouchLog {
    entries: [StoredTouch; MAX_WORD_KEYS],
    len: u8,
    usable: bool,
}

impl TouchLog {
    pub(crate) fn len(&self) -> usize {
        usize::from(self.len)
    }

    pub(crate) fn entries(&self) -> &[StoredTouch] {
        &self.entries[..self.len()]
    }

    pub(crate) fn reset(&mut self) {
        self.len = 0;
        self.usable = true;
    }

    pub(crate) fn invalidate(&mut self) {
        self.usable = false;
    }

    pub(crate) fn push(&mut self, raw_key: char, mut touch: StoredTouch) {
        touch.recase(raw_key);
        if self.len() == MAX_WORD_KEYS || touch.typed != raw_key {
            self.invalidate();
            return;
        }
        self.entries[self.len()] = touch;
        self.len += 1;
    }

    /// Whether the log lines up with the raw keys key for key. Checked once per word
    /// boundary, on a word that is already a correction candidate.
    pub(crate) fn matches(&self, keys: &str) -> bool {
        self.usable
            && self.len() == keys.chars().count()
            && self
                .entries()
                .iter()
                .zip(keys.chars())
                .all(|(entry, key)| entry.typed == key)
    }

    /// The score of the word exactly as it was typed.
    pub(crate) fn base_score(&self) -> f32 {
        score::base_score(self.entries().iter().map(|e| from_q8(e.typed_distance)))
    }
}

impl Default for TouchLog {
    fn default() -> Self {
        Self {
            entries: [StoredTouch::default(); MAX_WORD_KEYS],
            len: 0,
            usable: true,
        }
    }
}
