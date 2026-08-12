//! Eager English-restore: can the buffer still become a Vietnamese syllable?
//!
//! Runs on every composed keystroke, so the check is allocation-free: the
//! deshaped rhyme-so-far goes into a stack buffer and is prefix-matched against
//! the deshaped rhyme inventory ([`rhyme::has_deshaped_prefix`]).

use crate::unicode::marks::{Tone, vowel_stem};
use crate::validation::coda::{STOP_CODAS, coda_in, normalized_coda, nucleus_tone};
use crate::validation::parse::{SyllableParts, parse_syllable};
use crate::validation::rhyme::{self, plain_base};

/// Longer than every deshaped rhyme (max is 4 chars, e.g. `uong`), so a query
/// that overflows this buffer can never be a rhyme prefix.
const MAX_QUERY: usize = 8;

/// Stem with tone removed but vowel shape preserved (`ồ` → `ô`).
fn shaped_base(c: char) -> char {
    let stem = vowel_stem(c).unwrap_or(c);
    char::to_lowercase(stem).next().unwrap_or(stem)
}

/// True when `buffer` can **no longer** become a valid Vietnamese syllable by
/// typing more — used for *eager* English restore (flip back to the raw
/// keystrokes the instant a word is unrecoverable, without waiting for a boundary):
/// `tẽt`→`text` on the closing `t`, `caé`→`case` on the `e`, `luuỷ`→`luxury`.
///
/// Conservative. The rhyme so far is compared **deshaped** against the deshaped
/// rhyme inventory, so a plain vowel still awaiting its shape stays alive (`ưo`
/// matches `ươ…`, so typing `nước` via `nuwowcs` is never interrupted). Dead when:
/// 1. The deshaped nucleus+coda is not a prefix of any rhyme: `ae`, `uuy`, `ad`.
/// 2. A **stop** coda already carries a *wrong* tone — huyền / hỏi / ngã: `tẽt`.
///    (A stop coda with no tone yet stays alive — the tone follows the coda.)
pub fn is_definitely_invalid(buffer: &str) -> bool {
    let parts = parse_syllable(buffer);
    is_definitely_invalid_parts(&parts)
}

pub(crate) fn is_definitely_invalid_parts(parts: &SyllableParts<'_>) -> bool {
    if parts.nucleus_chars().next().is_none() {
        return false; // still building the onset
    }
    let Some((coda, coda_len)) = normalized_coda(parts) else {
        return true; // longer than any Vietnamese coda — no rhyme can match
    };
    let coda = &coda[..coda_len];

    let mut query = ['\0'; MAX_QUERY];
    let mut len = 0;
    for ch in parts.nucleus_chars().chain(coda.iter().copied()) {
        if len == MAX_QUERY {
            return true; // longer than any rhyme — dead end
        }
        query[len] = plain_base(ch);
        len += 1;
    }

    if !rhyme::has_deshaped_prefix(&query[..len]) {
        return true;
    }

    if coda_in(STOP_CODAS, coda) {
        return matches!(
            nucleus_tone(parts.nucleus_chars()),
            Some(Tone::Huyen | Tone::Hoi | Tone::Nga)
        );
    }
    false
}

/// Whether the candidate's actual shaped rhyme is a prefix of a Vietnamese rhyme.
/// Unlike eager English restore, this deliberately does not deshape vowels: a new
/// circumflex must prove that its precise result (`ôe`, not plain `oe`) is reachable.
pub(crate) fn has_shaped_rhyme_prefix(parts: &SyllableParts<'_>) -> bool {
    if parts.nucleus_chars().next().is_none() {
        return false;
    }
    let Some((coda, coda_len)) = normalized_coda(parts) else {
        return false;
    };

    let mut query = ['\0'; MAX_QUERY];
    let mut len = 0;
    for ch in parts
        .nucleus_chars()
        .chain(coda[..coda_len].iter().copied())
    {
        if len == MAX_QUERY {
            return false;
        }
        query[len] = shaped_base(ch);
        len += 1;
    }
    rhyme::has_prefix(&query[..len])
}
