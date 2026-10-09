//! Which spellings count as a Vietnamese syllable, as a value the caller chooses.
//!
//! Every syllable check reads one [`SyllableRules`]. The free functions at the
//! crate root ([`crate::is_complete_syllable`] and friends) judge by
//! [`SyllableRules::STANDARD`]; the methods here judge by any rules. A new
//! relaxation is one more field on this struct — the checks already carry it.

mod onsets;

pub use onsets::ExtraOnsets;

use crate::InputMethod;
use crate::validation::parse::parse_syllable;
use crate::validation::reachability::is_definitely_invalid_parts;
use crate::validation::syllable::{ModifierValidation, SyllableStatus, classify, validate_shape};

/// The spelling a buffer is judged against.
///
/// Native Vietnamese spelling (with the Tây Nguyên place names) is
/// [`SyllableRules::STANDARD`], which is also the default. Non-exhaustive: start
/// from `STANDARD` and widen it with the `with_*` methods.
///
/// ```
/// use funput_core::{ExtraOnsets, SyllableRules};
///
/// let teencode = SyllableRules::STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ);
/// assert!(teencode.is_complete_syllable("zô"));
/// assert!(!SyllableRules::STANDARD.is_complete_syllable("zô"));
/// ```
#[non_exhaustive]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Default)]
pub struct SyllableRules {
    /// Onsets admitted on top of the native ones (`zô`, `jờ`, `fải`, `wá`).
    pub extra_onsets: ExtraOnsets,
}

impl SyllableRules {
    /// Native Vietnamese spelling — what every crate-root check uses.
    pub const STANDARD: Self = Self {
        extra_onsets: ExtraOnsets::NONE,
    };

    /// These rules, admitting `extra_onsets` as well.
    #[inline]
    #[must_use]
    pub const fn with_extra_onsets(mut self, extra_onsets: ExtraOnsets) -> Self {
        self.extra_onsets = extra_onsets;
        self
    }

    /// [`crate::is_valid`], judged by these rules.
    #[inline]
    pub fn is_valid(self, buffer: &str) -> bool {
        validate_shape(buffer, self) == ModifierValidation::Allow
    }

    /// [`crate::is_complete_syllable`], judged by these rules.
    #[inline]
    pub fn is_complete_syllable(self, buffer: &str) -> bool {
        classify(buffer, self) == SyllableStatus::Complete
    }

    /// [`crate::is_reopenable_syllable`], judged by these rules.
    #[inline]
    pub fn is_reopenable_syllable(self, buffer: &str) -> bool {
        classify(buffer, self) != SyllableStatus::Invalid
    }

    /// [`crate::is_definitely_invalid`], judged by these rules.
    #[inline]
    pub fn is_definitely_invalid(self, buffer: &str) -> bool {
        is_definitely_invalid_parts(&parse_syllable(buffer, self), false)
    }

    /// [`crate::is_definitely_invalid_in`], judged by these rules.
    #[inline]
    pub fn is_definitely_invalid_in(self, buffer: &str, method: InputMethod) -> bool {
        is_definitely_invalid_parts(&parse_syllable(buffer, self), !method.is_telex_family())
    }
}

#[cfg(test)]
mod tests;
