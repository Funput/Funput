//! Syllable-structure validation.
//!
//! [`modifier`] decides whether a modifier keystroke should apply; the checks
//! here answer the coarser question of whether a buffer *is* a Vietnamese
//! syllable — leniently mid-typing ([`is_valid`]) or strictly at a word boundary
//! ([`is_complete_syllable`]).

mod modifier;
mod spelling;

use crate::orthography::glide;
use crate::unicode::marks::Tone;
use crate::validation::coda::{
    STOP_CODAS, VALID_CODAS, coda_in, normalized_coda, nucleus_tone, toneless_rhyme,
};
use crate::validation::parse::{is_valid_onset, parse_syllable};
use crate::validation::reachability::{has_shaped_rhyme_prefix, is_definitely_invalid_parts};
use crate::validation::rhyme::is_valid_rhyme;

use modifier::{ModifierKind, validate_parts};
use spelling::violates_ckg_spelling;

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
    let parts = parse_syllable(buffer);
    let Some((coda, coda_len)) = normalized_coda(&parts) else {
        return false;
    };
    let coda = &coda[..coda_len];

    let structure_ok = !parts.invalid_onset
        && is_valid_onset(parts.onset)
        // A tone parked on the `qu`/`gi` glide is a mid-composition transient, not
        // a finished syllable: `qúy` is a misspelling of `quý`.
        && !glide::onset_holds_tone(parts.onset)
        && parts.nucleus_chars().next().is_some()
        && !violates_ckg_spelling(parts.onset, &parts)
        && coda_in(VALID_CODAS, coda);
    if !structure_ok {
        return false;
    }

    // The nucleus+coda must be a real Vietnamese rhyme (Level 2): keeps `việt`,
    // `trường` … but reverts structurally-ok-but-nonexistent rhymes.
    if !is_valid_rhyme(&toneless_rhyme(&parts, coda)) {
        return false;
    }

    // Phonotactics: a stop coda only allows sắc / nặng. Flags `tẽt` (English
    // "text"), `bèct`, etc. as not-a-syllable so the engine restores the raw word.
    if coda_in(STOP_CODAS, coda) {
        return matches!(
            nucleus_tone(parts.nucleus_chars()),
            Some(Tone::Sac | Tone::Nang)
        );
    }
    true
}

#[cfg(test)]
mod tests;
