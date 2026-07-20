//! Syllable-structure validation for modifier keys (tone / shape / stroke).
//!
//! Decides whether a modifier should apply, be ignored, or pass through as a
//! literal key (non-Vietnamese structure the engine restores later). Hot path:
//! everything works on the borrowed [`SyllableParts`] view — the only
//! allocation left is the rhyme string built at a word boundary.

mod spelling;

use crate::unicode::marks::Tone;
use crate::validation::coda::{
    STOP_CODAS, VALID_CODAS, coda_in, normalized_coda, nucleus_tone, toneless_rhyme,
};
use crate::validation::parse::{SyllableParts, is_valid_onset, parse_syllable};
use crate::validation::reachability::{has_shaped_rhyme_prefix, is_definitely_invalid_parts};
use crate::validation::rhyme::is_valid_rhyme;

use spelling::violates_ckg_spelling;

/// Result of validating a modifier keystroke against the current buffer.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ModifierValidation {
    /// Apply Vietnamese transform.
    Allow,
    /// No valid target — discard key.
    Ignored,
    /// Non-Vietnamese structure — append key literally (engine restores later).
    PassThrough,
}

fn validate_modifier(buffer: &str) -> ModifierValidation {
    let parts = parse_syllable(buffer);
    validate_parts(&parts)
}

fn validate_parts(parts: &SyllableParts<'_>) -> ModifierValidation {
    if parts.invalid_onset || !is_valid_onset(parts.onset) {
        return ModifierValidation::PassThrough;
    }
    if parts.nucleus_chars().next().is_none() {
        return ModifierValidation::Ignored;
    }
    if violates_ckg_spelling(parts.onset, parts) {
        return ModifierValidation::PassThrough;
    }

    // Two or more trailing consonants can't form a Vietnamese coda → likely an
    // English word, pass the key through. A single trailing consonant is allowed
    // (the user may still be typing, e.g. "mix" → "mĩx").
    if parts.coda_chars().nth(1).is_some() {
        match normalized_coda(parts) {
            Some((coda, len)) if coda_in(VALID_CODAS, &coda[..len]) => {}
            _ => return ModifierValidation::PassThrough,
        }
    }
    ModifierValidation::Allow
}

/// Validate a newly shaped candidate in one parse: orthographic structure, the
/// exact shaped-rhyme prefix, and tone/coda reachability must all agree.
pub(crate) fn is_viable_shape_candidate(buffer: &str) -> bool {
    let parts = parse_syllable(buffer);
    matches!(validate_parts(&parts), ModifierValidation::Allow)
        && has_shaped_rhyme_prefix(&parts)
        && !is_definitely_invalid_parts(&parts)
}

/// Validate tone key (1–5) against the current buffer.
pub fn validate_tone(buffer: &str) -> ModifierValidation {
    validate_modifier(buffer)
}

/// Validate shape key (6–8) against the current buffer.
pub fn validate_shape(buffer: &str) -> ModifierValidation {
    validate_modifier(buffer)
}

/// Validate stroke key (9) against the current buffer. The stroke applies whenever
/// there is a `d`/`D` to convert anywhere in the buffer (`dang` + `9` → `đang`),
/// not only when it is the trailing char.
pub fn validate_stroke(buffer: &str) -> ModifierValidation {
    if buffer.contains(['d', 'D']) {
        ModifierValidation::Allow
    } else {
        ModifierValidation::Ignored
    }
}

/// Returns true if the syllable structure is valid for transform.
///
/// **Lenient** (mid-typing): a single trailing consonant is accepted because the
/// user may still be typing (e.g. `mix` → allow, so `mĩx` can compose). For a
/// finished word use [`is_complete_syllable`].
pub fn is_valid(buffer: &str) -> bool {
    matches!(validate_modifier(buffer), ModifierValidation::Allow)
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
