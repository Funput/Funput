//! Building one letter out of a base and the marks that follow it.
//!
//! # Why this is not a fold
//!
//! The obvious implementation applies each mark to a `char` in turn. It is wrong,
//! and quietly: [`crate::unicode::shapes::apply_shape_to_vowel`] goes through
//! `base_vowel`, which strips the **tone** as well as the shape — the crate's own
//! test for it is called `apply_shape_to_vowel_strips_tone`.
//!
//! Canonical NFD puts the tone first whenever it is nặng, because `U+0323` has a
//! lower combining class than the circumflex and breve: `ậ` is `a` `U+0323`
//! `U+0302`. Folding that would produce `ạ`, then throw the tone away and hand
//! back `â`. It would hit `ặ ậ ệ ộ` — that is `Việt`, `một`, `cộng`, `nặng`.
//!
//! So marks go into slots and the letter is composed **once**, at the end. Order
//! stops mattering, which is the right answer anyway: NFD's own order flips
//! depending on which tone is involved.
//!
//! # A full slot ends the letter
//!
//! A second mark of the same kind is *not* absorbed. `apply_tone` replaces rather
//! than refuses, so absorbing one would silently drop the first: `a` `U+0301`
//! `U+0300` would become `à`, three characters read as one, and the sắc gone.
//! Refusing leaves the second mark to be read on its own, which is what it is.

use crate::unicode::marks::{self, Tone};
use crate::unicode::shapes::{self, VowelShape};

use super::marks::Mark;

/// A letter under construction: a base, and at most one shape and one tone.
pub(super) struct Letter {
    base: char,
    shape: Option<VowelShape>,
    tone: Option<Tone>,
    /// Whether each part arrived as a combining mark rather than already being
    /// part of the base character. Together they decide whether the source used
    /// this charset's own spelling — see [`Letter::is_canonical`].
    shape_from_mark: bool,
    tone_from_mark: bool,
}

impl Letter {
    /// Take a character apart into slots. A non-vowel keeps its slots empty and so
    /// accepts no marks at all, which is how `n` + sắc and `đ` + nặng come out as
    /// a letter followed by an orphan.
    pub(super) fn seed(c: char) -> Self {
        let Some(base) = shapes::base_vowel(c) else {
            return Self {
                base: c,
                shape: None,
                tone: None,
                shape_from_mark: false,
                tone_from_mark: false,
            };
        };
        Self {
            base,
            shape: shapes::shape_on_vowel(c),
            tone: marks::tone_on_vowel(c),
            shape_from_mark: false,
            tone_from_mark: false,
        }
    }

    /// Try to put `mark` in its slot. `false` when the slot is taken or the letter
    /// cannot carry that mark — either way the caller stops and leaves it behind.
    pub(super) fn absorb(&mut self, mark: Mark) -> bool {
        match mark {
            Mark::Shape(shape) => {
                if self.shape.is_some() || shapes::apply_shape(self.base, shape).is_none() {
                    return false;
                }
                self.shape = Some(shape);
                self.shape_from_mark = true;
            }
            Mark::Tone(tone) => {
                if self.tone.is_some() || marks::apply_tone(self.stem(), tone).is_none() {
                    return false;
                }
                self.tone = Some(tone);
                self.tone_from_mark = true;
            }
        }
        true
    }

    /// The letter with its shape but no tone.
    fn stem(&self) -> char {
        match self.shape {
            Some(shape) => shapes::apply_shape(self.base, shape)
                .expect("absorb checks the shape before taking it"),
            None => self.base,
        }
    }

    /// The finished letter.
    pub(super) fn compose(&self) -> char {
        match self.tone {
            Some(tone) => marks::apply_tone(self.stem(), tone)
                .expect("absorb checks the tone before taking it"),
            None => self.stem(),
        }
    }

    /// Whether the source spelled this letter the way this charset spells it:
    /// shape precomposed into the base, tone riding as its own mark.
    ///
    /// A precomposed `ấ` fails on the tone; full NFD `a` `U+0302` `U+0301` fails on
    /// the shape. Both are read correctly and rewritten — they are not losses.
    pub(super) fn is_canonical(&self) -> bool {
        !self.shape_from_mark && (self.tone.is_none() || self.tone_from_mark)
    }
}
