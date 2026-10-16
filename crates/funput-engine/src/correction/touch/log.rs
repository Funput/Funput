//! The quantised record of where the fingers landed, one entry per raw key.
//!
//! A prefix buffer, not a ring: correction only ever asks about the word that just
//! ended, so a word past the key cap is dropped outright instead of keeping its tail.
//! A key that arrived without a report still takes its slot, marked unreported, so
//! the log keeps lining up with the raw keys and the search can fall back to the
//! static layout for that key alone. Distances are Q8.8 integers because the log is
//! reachable from `Session`, which derives `Eq`.

use crate::correction::touch::MAX_ALTERNATES;

/// Longest word correction will look at. Past this the replay search stops paying
/// for itself, and a very long token is rarely a mistyped syllable. Twelve covers
/// the longest Telex spellings with a doubled key or a stray modifier in them.
pub(crate) const MAX_WORD_KEYS: usize = 12;

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
    /// Whether the host said where the finger landed. An unreported entry holds
    /// the key alone, and the search reads the layout for it.
    pub(crate) reported: bool,
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

    /// Record the key that just joined the raw keys, with the host's touch for it
    /// when there was one. A touch that names a different key than the engine
    /// recorded spoils the log: it would describe a word that was not typed.
    pub(crate) fn push(&mut self, raw_key: char, touch: Option<StoredTouch>) {
        let mut touch = touch.unwrap_or(StoredTouch {
            typed: raw_key,
            ..StoredTouch::default()
        });
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
