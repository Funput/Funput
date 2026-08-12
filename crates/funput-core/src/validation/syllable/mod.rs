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
use crate::validation::rhyme::{is_valid_rhyme, matches_deshaped};

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

/// How a finished buffer sits against Vietnamese syllable structure.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum SyllableStatus {
    /// Not a Vietnamese syllable, and no diacritic can make it one: `cảd` (card),
    /// `côl` (cool), `tẽt` (text).
    Invalid,
    /// Structurally a syllable, but still short of the **diacritics** the word
    /// needs — which is exactly how a word looks when the user commits it before
    /// finishing it. Three ways that happens:
    /// - a **stop coda** (`p t c ch`) with no tone, where Vietnamese requires sắc
    ///   or nặng: `chuc` → `chúc`, `tich` → `tích`;
    /// - a rhyme that only exists shaped: `dien` → `diên`, `thuo` → `thuơ`;
    /// - both at once: `nuoc` → `nước`.
    AwaitingDiacritic,
    /// A real Vietnamese syllable as it stands: `chào`, `việt`, `ma`, `tét`.
    Complete,
}

fn classify(buffer: &str) -> SyllableStatus {
    let parts = parse_syllable(buffer);
    let Some((coda, coda_len)) = normalized_coda(&parts) else {
        return SyllableStatus::Invalid;
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
        return SyllableStatus::Invalid;
    }

    // The nucleus+coda must be a real Vietnamese rhyme (Level 2): keeps `việt`,
    // `trường` … but reverts structurally-ok-but-nonexistent rhymes. A rhyme that
    // exists only *shaped* is that same rhyme with its shape keys still to come —
    // `ien` is `iên` minus the circumflex — so it is unfinished, not wrong.
    let rhyme = toneless_rhyme(&parts, coda);
    let status = if is_valid_rhyme(&rhyme) {
        SyllableStatus::Complete
    } else if matches_deshaped(&rhyme) {
        SyllableStatus::AwaitingDiacritic
    } else {
        return SyllableStatus::Invalid;
    };

    // Phonotactics: a stop coda only allows sắc / nặng. A wrong tone (huyền / hỏi /
    // ngã) is what catches English `text` (→ `tẽt`) or `coot` (→ `côt`); no tone at
    // all is the un-toned form of a real word, which a tone key can still complete.
    if coda_in(STOP_CODAS, coda) {
        return match nucleus_tone(parts.nucleus_chars()) {
            Some(Tone::Sac | Tone::Nang) => status,
            None => SyllableStatus::AwaitingDiacritic,
            Some(_) => SyllableStatus::Invalid,
        };
    }
    status
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
