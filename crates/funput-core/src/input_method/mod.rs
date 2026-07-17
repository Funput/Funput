//! Key classification per input method.
//!
//! Each method (VNI, Telex, …) maps a keystroke to a shared [`KeyAction`]; the
//! composition pipeline in [`crate::composition`] is method-agnostic and only
//! ever sees [`KeyAction`]s. Adding a method means adding a classifier module
//! here — nothing downstream changes.
//!
//! Classifiers take the current syllable buffer because Telex digraphs (`aa`,
//! `dd`, `w` after a vowel) depend on context; VNI ignores the buffer.

pub mod telex;
pub mod vni;

use crate::unicode::marks::Tone;
use crate::unicode::shapes::VowelShape;

/// Exact Telex vowel stem targeted by a free-position circumflex.
///
/// A compact enum keeps [`KeyAction`] at its original small size on the hot path.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CircumflexStem {
    A,
    E,
    O,
}

impl From<CircumflexStem> for char {
    fn from(value: CircumflexStem) -> Self {
        value.as_char()
    }
}

impl CircumflexStem {
    #[inline]
    pub(crate) fn from_key(key: char) -> Option<Self> {
        match key.to_ascii_lowercase() {
            'a' => Some(Self::A),
            'e' => Some(Self::E),
            'o' => Some(Self::O),
            _ => None,
        }
    }

    #[inline]
    pub(crate) fn as_char(self) -> char {
        match self {
            Self::A => 'a',
            Self::E => 'e',
            Self::O => 'o',
        }
    }
}

/// What a keystroke means, independent of input method.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum KeyAction {
    /// Đ stroke (`d` → `đ`).
    Stroke,
    /// Tone mark (sắc / huyền / hỏi / ngã / nặng).
    Tone(Tone),
    /// Vowel shape (mũ / móc / trần).
    Shape(VowelShape),
    /// A circumflex requested by a repeated Telex vowel away from its target.
    ///
    /// Carries the exact ASCII stem so composition never guesses between several
    /// shapeable vowels in the nucleus (`a`, `e`, and `o` all accept a circumflex).
    FreeCircumflex(CircumflexStem),
    /// Continue a `w` intent represented by a literal marker in the buffer.
    DeferredW,
    /// Remove the tone mark from the syllable, keeping shapes (VNI `0`, Telex `z`).
    RemoveTone,
    /// Ordinary character — appended as-is.
    Normal,
}
