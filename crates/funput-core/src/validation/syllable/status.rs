//! Where a finished buffer stands against Vietnamese syllable structure.
//!
//! One parse answers all three of the boundary questions in [`super`]: onset and
//! coda must be legal, the rhyme must exist in the inventory, and a stop coda
//! must carry a tone Vietnamese allows. The public predicates are thin readings
//! of the [`SyllableStatus`] this returns.

use crate::orthography::glide;
use crate::unicode::marks::Tone;
use crate::validation::coda::{
    STOP_CODAS, VALID_CODAS, coda_in, normalized_coda, nucleus_tone, toneless_rhyme,
};
use crate::validation::parse::{is_valid_onset, parse_syllable};
use crate::validation::rhyme::{is_valid_rhyme, matches_deshaped};

use super::spelling::violates_ckg_spelling;

/// How a finished buffer sits against Vietnamese syllable structure.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub(super) enum SyllableStatus {
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

pub(super) fn classify(buffer: &str) -> SyllableStatus {
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
