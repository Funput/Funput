//! The extra initial consonants a user admits beyond Vietnamese spelling — UniKey's
//! "Cho phép phụ âm đầu Z, F, W, J", one switch per letter.
//!
//! Stored the way the interchange document spells it (`"zj"`, see
//! `platforms/CONFIG_FORMAT.md`), so `settings.json` and an exported file agree. The
//! bits inside [`ExtraOnsetLetters`] are private and never written anywhere: they are
//! neither the core's own bits nor the FFI's `ONSET_*` wire values.

use funput_core::ExtraOnsets;
use serde::{Deserialize, Serialize};

/// One consonant Vietnamese spelling lacks but teencode, loanwords and some names
/// open a syllable with (`zô`, `fải`, `wá`, `jờ`).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum OnsetLetter {
    Z,
    F,
    W,
    J,
}

impl OnsetLetter {
    /// Every letter, in the order a settings screen lists them — UniKey's "Z, F, W, J".
    pub const ALL: [Self; 4] = [Self::Z, Self::F, Self::W, Self::J];

    /// The letter as typed, lowercase.
    pub fn symbol(self) -> char {
        match self {
            Self::Z => 'z',
            Self::F => 'f',
            Self::W => 'w',
            Self::J => 'j',
        }
    }

    /// The letter `c` spells, in either case.
    pub fn from_symbol(c: char) -> Option<Self> {
        let c = c.to_ascii_lowercase();
        Self::ALL.into_iter().find(|letter| letter.symbol() == c)
    }

    /// The onset the engine admits for this letter.
    pub fn core(self) -> ExtraOnsets {
        match self {
            Self::Z => ExtraOnsets::Z,
            Self::F => ExtraOnsets::F,
            Self::W => ExtraOnsets::W,
            Self::J => ExtraOnsets::J,
        }
    }

    fn bit(self) -> u8 {
        1 << self as u8
    }
}

/// The letters a user admits as initial consonants. Empty — the default — admits
/// none, which is native Vietnamese spelling.
///
/// Persisted as the letters spelled out in [`OnsetLetter::ALL`] order (`"zj"`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Serialize, Deserialize)]
#[serde(from = "String", into = "String")]
pub struct ExtraOnsetLetters(u8);

impl ExtraOnsetLetters {
    /// No extra letter: native spelling only.
    pub const NONE: Self = Self(0);
    /// Every letter — what UniKey's single switch turns on.
    pub const ALL: Self = Self(0b1111);

    /// Whether `letter` is admitted.
    pub fn contains(self, letter: OnsetLetter) -> bool {
        self.0 & letter.bit() != 0
    }

    /// These letters with `letter` admitted (`on`) or not.
    #[must_use]
    pub fn with(self, letter: OnsetLetter, on: bool) -> Self {
        if on {
            Self(self.0 | letter.bit())
        } else {
            Self(self.0 & !letter.bit())
        }
    }

    /// Whether no letter is admitted.
    pub fn is_empty(self) -> bool {
        self.0 == 0
    }

    /// The admitted letters, in [`OnsetLetter::ALL`] order.
    pub fn letters(self) -> impl Iterator<Item = OnsetLetter> {
        OnsetLetter::ALL
            .into_iter()
            .filter(move |&letter| self.contains(letter))
    }

    /// The onsets the engine admits for these letters.
    pub fn core(self) -> ExtraOnsets {
        self.letters()
            .fold(ExtraOnsets::NONE, |set, letter| set.union(letter.core()))
    }

    /// The letters spelled out (`"zj"`; `""` for none) — the stored and exported form.
    pub fn id(self) -> String {
        self.letters().map(OnsetLetter::symbol).collect()
    }

    /// Read a stored or imported value. Any order and case; a letter this build does
    /// not know is skipped rather than failing the rest — the forward-compatibility
    /// rule in `CONFIG_FORMAT.md`.
    pub fn from_id(id: &str) -> Self {
        id.chars()
            .filter_map(OnsetLetter::from_symbol)
            .fold(Self::NONE, |set, letter| set.with(letter, true))
    }
}

impl From<String> for ExtraOnsetLetters {
    fn from(id: String) -> Self {
        Self::from_id(&id)
    }
}

impl From<ExtraOnsetLetters> for String {
    fn from(letters: ExtraOnsetLetters) -> Self {
        letters.id()
    }
}

#[cfg(test)]
mod tests;
