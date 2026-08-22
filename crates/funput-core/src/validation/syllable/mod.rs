//! Syllable-structure validation.
//!
//! [`modifier`] decides whether a modifier keystroke should apply; the checks
//! here answer the coarser question of whether a buffer *is* a Vietnamese
//! syllable — leniently mid-typing ([`is_valid`]) or strictly at a word boundary
//! ([`is_complete_syllable`]). The strict verdicts all read one [`status`] parse.

mod modifier;
mod spelling;
mod status;

use crate::unicode::shapes::shape_on_vowel;
use crate::validation::parse::parse_syllable;
use crate::validation::reachability::{has_shaped_rhyme_prefix, is_definitely_invalid_parts};

use modifier::{ModifierKind, validate_parts};
use status::{SyllableStatus, classify};

pub use modifier::{ModifierValidation, validate_shape, validate_stroke, validate_tone};

/// Validate a newly shaped candidate in one parse: orthographic structure, the
/// exact shaped-rhyme prefix, and tone/coda reachability must all agree.
pub(crate) fn is_viable_shape_candidate(buffer: &str) -> bool {
    let parts = parse_syllable(buffer);
    matches!(
        validate_parts(&parts, ModifierKind::Shape),
        ModifierValidation::Allow
    ) && has_shaped_rhyme_prefix(&parts)
        && !is_definitely_invalid_parts(&parts)
}

/// Returns true if the syllable structure is valid for transform.
///
/// **Lenient** (mid-typing): a single trailing consonant is accepted because the
/// user may still be typing (e.g. `mix` → allow, so `mĩx` can compose). For a
/// finished word use [`is_complete_syllable`].
pub fn is_valid(buffer: &str) -> bool {
    matches!(
        validate_parts(&parse_syllable(buffer), ModifierKind::Shape),
        ModifierValidation::Allow
    )
}

/// Returns true if `buffer` is a *complete* valid Vietnamese syllable.
///
/// **Strict**: the coda must be a real Vietnamese final (`c ch m n ng nh p t`),
/// and a **stop coda** (`p t c ch`) only with the sắc or nặng tone (phonotactics).
/// No "still typing" leniency. Use this at a word boundary — the engine restores
/// the raw word when a finished word is *not* a complete syllable: `cảd` (card),
/// `côl` (cool), `tẽt` (text).
pub fn is_complete_syllable(buffer: &str) -> bool {
    matches!(classify(buffer), SyllableStatus::Complete)
}

/// Returns true if `buffer` is a lone shaped vowel: `ă`, `â`, `ắ`, `ậ` — one
/// character, no onset, no coda, carrying mũ / móc / trần.
///
/// These are **not** complete syllables and [`is_complete_syllable`] rightly says
/// so: `ă` and `â` never stand as an open rhyme in Vietnamese, they only appear
/// before a coda (`ăn`, `âm`). But a *word* that is nothing but a shaped vowel is
/// still unambiguous intent — no English word spells one, and naming the letter
/// itself is a real thing users type. Callers deciding whether to restore raw
/// keystrokes at a word boundary use this to keep the vowel; callers judging
/// Vietnamese spelling must not.
pub fn is_bare_shaped_vowel(buffer: &str) -> bool {
    let mut chars = buffer.chars();
    let Some(vowel) = chars.next() else {
        return false;
    };
    chars.next().is_none() && shape_on_vowel(vowel).is_some()
}

/// Returns true if a committed `buffer` may be re-opened as a live composition
/// (`Engine::adopt`, after Backspace puts the caret back on it).
///
/// Looser than [`is_complete_syllable`] in exactly one place: a syllable still
/// **awaiting a diacritic** counts — a missing tone (`chuc` + `s` → `chúc`), a
/// missing vowel shape (`dien` + `e` → `diên`), or both (`nuoc` + `w` → `nươc`).
/// Those words are the whole point of re-opening: they are what the engine itself
/// leaves on screen when the user commits before finishing the word. Still strict
/// enough to keep English words and URLs literal (`hello`, `text`, `tẽt`, `die`).
pub fn is_reopenable_syllable(buffer: &str) -> bool {
    !matches!(classify(buffer), SyllableStatus::Invalid)
}

#[cfg(test)]
mod tests;
