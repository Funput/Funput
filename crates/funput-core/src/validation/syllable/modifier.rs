//! Should a modifier keystroke (tone / shape / stroke) apply to this buffer?
//!
//! Hot path: everything works on the borrowed [`SyllableParts`] view, so no
//! allocation happens here.

use crate::orthography::glide;
use crate::validation::coda::{VALID_CODAS, coda_in, normalized_coda};
use crate::validation::parse::{SyllableParts, is_valid_onset, parse_syllable};

use super::spelling::violates_ckg_spelling;

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

/// Which modifier is asking. The two differ on one point only: a `qu`/`gi` glide
/// with no nucleus yet can hold a **tone** but never a **shape** — see
/// [`empty_nucleus`].
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(super) enum ModifierKind {
    Tone,
    Shape,
}

pub(super) fn validate_parts(parts: &SyllableParts<'_>, kind: ModifierKind) -> ModifierValidation {
    if parts.invalid_onset || !is_valid_onset(parts.onset) {
        return ModifierValidation::PassThrough;
    }
    if parts.nucleus_chars().next().is_none() {
        return empty_nucleus(parts.onset, kind);
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

/// Nothing to modify yet — with one exception.
///
/// A `qu` onset holds its `u` (the glide) rather than releasing it as a nucleus,
/// so `qu` + a tone key would otherwise drop the key on the floor. Let the tone
/// park on the glide instead; [`crate::orthography::reposition_existing_tone`]
/// moves it the moment the real nucleus arrives (`qu` + `s` → `qú`, + `a` →
/// `quá`). This is exactly what `gi` already does via `gí` → `giá`.
///
/// A **shape** gets no such licence: horning the glide would produce `qư`.
fn empty_nucleus(onset: &str, kind: ModifierKind) -> ModifierValidation {
    match kind {
        ModifierKind::Tone if glide::in_onset(onset).is_some() => ModifierValidation::Allow,
        _ => ModifierValidation::Ignored,
    }
}

/// Validate tone key (1–5) against the current buffer.
pub fn validate_tone(buffer: &str) -> ModifierValidation {
    validate_parts(&parse_syllable(buffer), ModifierKind::Tone)
}

/// Validate shape key (6–8) against the current buffer.
pub fn validate_shape(buffer: &str) -> ModifierValidation {
    validate_parts(&parse_syllable(buffer), ModifierKind::Shape)
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
