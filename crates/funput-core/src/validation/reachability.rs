//! Eager English-restore: can the buffer still become a Vietnamese syllable?
//!
//! Runs on every composed keystroke, so the check is allocation-free: the
//! deshaped rhyme-so-far goes into a stack buffer and is prefix-matched against
//! the deshaped rhyme inventory ([`rhyme::has_deshaped_prefix`]).

use crate::InputMethod;
use crate::unicode::marks::{Tone, vowel_stem};
use crate::validation::coda::{STOP_CODAS, VALID_CODAS, coda_in, normalized_coda, nucleus_tone};
use crate::validation::ethnic;
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
/// 1. The deshaped nucleus+coda is not a prefix of any rhyme: `ae`, `uuy`, `ad` —
///    unless a Tây Nguyên name spells it (`Kpă`, `Dliê`; see [`ethnic`]).
/// 2. A **stop** coda already carries a *wrong* tone — huyền / hỏi / ngã: `tẽt`.
///    (A stop coda with no tone yet stays alive — the tone follows the coda.)
/// 3. A consonant sits between the onset and a vowel: `cno`, `ona`. Modifier keys
///    only reshape vowels (and `d`), so no later key can move it back out.
///
/// This is the reading every method can afford; [`is_definitely_invalid_in`]
/// widens it where the method allows.
pub fn is_definitely_invalid(buffer: &str) -> bool {
    is_definitely_invalid_parts(&parse_syllable(buffer), false)
}

/// [`is_definitely_invalid`] for a word typed in `method`. VNI also keeps alive a
/// word ending in a final only place names use — `Chư Păh`, `Ea Nuôl`, `Blơr`
/// (see [`ethnic`]) — where in Telex those finals are English `cash`, `cool` and
/// the hỏi key. English still reaches them in VNI through a digit glued to a word
/// (`bar1` → `bár`); telling that apart takes the keystrokes, so it is the
/// caller's call which reading a keystroke gets.
pub fn is_definitely_invalid_in(buffer: &str, method: InputMethod) -> bool {
    is_definitely_invalid_parts(&parse_syllable(buffer), !method.is_telex_family())
}

/// `name_finals`: whether the `h`/`l`/`r` finals of place names count as alive.
pub(crate) fn is_definitely_invalid_parts(parts: &SyllableParts<'_>, name_finals: bool) -> bool {
    if parts.nucleus_chars().next().is_none() {
        return false; // still building the onset
    }
    if !parts.is_well_ordered() {
        return true;
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

    if !rhyme::has_deshaped_prefix(&query[..len]) && !is_name_rhyme(parts, coda, name_finals) {
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

/// Whether a rhyme the inventory rejects still stands in a Tây Nguyên name: after
/// a distinct cluster (`Kpă`), or — when `name_finals` — before a name final
/// (`Păh`). Out of line and cold: real Vietnamese never gets here, and keeping it
/// out of [`is_definitely_invalid_parts`] keeps the per-keystroke path compact.
#[cold]
#[inline(never)]
fn is_name_rhyme(parts: &SyllableParts<'_>, coda: &[char], name_finals: bool) -> bool {
    (ethnic::admits(parts.onset) && coda_in(VALID_CODAS, coda))
        || (name_finals
            && ethnic::closes_with_name_final(
                parts.nucleus_chars().map(plain_base),
                coda,
                rhyme::has_deshaped_prefix,
            ))
}

/// Whether the candidate's actual shaped rhyme is a prefix of a Vietnamese rhyme.
/// Unlike eager English restore, this deliberately does not deshape vowels: a new
/// circumflex must prove that its precise result (`ôe`, not plain `oe`) is reachable.
/// A name's cluster onset does not widen this: which vowel takes the shape stays a
/// Vietnamese-spelling decision.
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
