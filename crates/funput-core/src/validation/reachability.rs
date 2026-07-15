//! Eager English-restore: can the buffer still become a Vietnamese syllable?
//!
//! Runs on every composed keystroke, so the check is allocation-free: the
//! deshaped rhyme-so-far goes into a stack buffer and is prefix-matched against
//! the cached deshaped rhyme inventory.

use std::sync::LazyLock;

use crate::unicode::marks::{Tone, vowel_stem};
use crate::validation::coda::{STOP_CODAS, coda_in, normalized_coda, nucleus_tone};
use crate::validation::parse::parse_syllable;
use crate::validation::rhyme;

/// Longer than every deshaped rhyme (max is 4 chars, e.g. `uong`), so a query
/// that overflows this buffer can never be a rhyme prefix.
const MAX_QUERY: usize = 8;

/// Plain base of a vowel — tone **and** shape stripped (`ớ`→`o`, `ẽ`→`e`,
/// `ư`→`u`). Non-vowels pass through lowercased.
fn plain_base(c: char) -> char {
    let stem = vowel_stem(c).unwrap_or(c);
    match char::to_lowercase(stem).next().unwrap_or(stem) {
        'ă' | 'â' => 'a',
        'ê' => 'e',
        'ô' | 'ơ' => 'o',
        'ư' => 'u',
        other => other,
    }
}

/// The rhyme inventory with tone/shape stripped, deshaped and sorted once.
/// Lets [`is_definitely_invalid`] test reachability in O(log n) without
/// re-deshaping all ~166 rhymes on every keystroke.
static DESHAPED_RHYMES: LazyLock<Vec<String>> = LazyLock::new(|| {
    let mut deshaped: Vec<String> = rhyme::all()
        .iter()
        .map(|r| r.chars().map(plain_base).collect())
        .collect();
    deshaped.sort_unstable();
    deshaped
});

fn starts_with(rhyme: &str, prefix: &[char]) -> bool {
    let mut chars = rhyme.chars();
    prefix.iter().all(|&p| chars.next() == Some(p))
}

/// True if some rhyme starts with `prefix`. The inventory is sorted, so all
/// candidates form a contiguous range beginning at the first entry ≥ `prefix`;
/// checking that single entry suffices.
fn any_rhyme_with_prefix(prefix: &[char]) -> bool {
    let rhymes = &*DESHAPED_RHYMES;
    let first = rhymes
        .partition_point(|r| r.chars().cmp(prefix.iter().copied()) == std::cmp::Ordering::Less);
    rhymes.get(first).is_some_and(|r| starts_with(r, prefix))
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
    if parts.nucleus_chars().next().is_none() {
        return false; // still building the onset
    }
    let Some((coda, coda_len)) = normalized_coda(&parts) else {
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

    if !any_rhyme_with_prefix(&query[..len]) {
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
