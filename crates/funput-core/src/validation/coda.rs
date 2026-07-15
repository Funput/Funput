//! Coda inventory and the nucleus/coda helpers shared by syllable validation
//! and rhyme reachability. Everything here is allocation-free except
//! [`toneless_rhyme`], which builds the boundary-time rhyme string.

use crate::unicode::marks::{Tone, tone_on_vowel, vowel_stem};
use crate::validation::parse::SyllableParts;

pub(crate) const VALID_CODAS: &[&str] = &["", "c", "ch", "m", "n", "ng", "nh", "p", "t"];

/// Stop (oral plosive) codas. A syllable ending in one of these may only carry
/// the sắc or nặng tone — a Vietnamese phonotactic rule. This is what tells
/// English `text` (→ `tẽt`, ngã + `t`) apart from real syllables like `tét`.
pub(crate) const STOP_CODAS: &[&str] = &["c", "ch", "p", "t"];

/// Longest valid Vietnamese coda (`ch` / `ng` / `nh`).
pub(crate) const MAX_CODA: usize = 2;

/// The tone carried by the nucleus (at most one toned vowel), if any.
pub(crate) fn nucleus_tone(mut nucleus: impl Iterator<Item = char>) -> Option<Tone> {
    nucleus.find_map(tone_on_vowel)
}

/// The coda lowercased into a fixed buffer, or `None` when it is longer than
/// any Vietnamese coda. A lone `k` is normalized to `c`: Central Highlands
/// (Tây Nguyên) toponyms write the final /k/ stop as `k` (`Đắk`, `Lắk`), which
/// is allophonic with final `c` — while a multi-letter coda (`ck`) stays
/// untouched, so English `rock`/`back` are unaffected.
pub(crate) fn normalized_coda(parts: &SyllableParts) -> Option<([char; MAX_CODA], usize)> {
    let mut coda = ['\0'; MAX_CODA];
    let mut len = 0;
    for ch in parts.coda_chars() {
        if len == MAX_CODA {
            return None;
        }
        coda[len] = ch.to_ascii_lowercase();
        len += 1;
    }
    if len == 1 && coda[0] == 'k' {
        coda[0] = 'c';
    }
    Some((coda, len))
}

/// True if `coda` (lowercase chars) is an entry of `list`.
pub(crate) fn coda_in(list: &[&str], coda: &[char]) -> bool {
    list.iter()
        .any(|entry| entry.chars().eq(coda.iter().copied()))
}

/// Toneless rhyme (vần) = nucleus with tones stripped (shape kept) + coda,
/// lowercase: `tiền` → `iên`, `trường` → `ương`.
pub(crate) fn toneless_rhyme(parts: &SyllableParts, coda: &[char]) -> String {
    let mut rhyme = String::with_capacity(8);
    for ch in parts.nucleus_chars() {
        let stem = vowel_stem(ch).unwrap_or(ch);
        rhyme.extend(stem.to_lowercase());
    }
    rhyme.extend(coda.iter().copied());
    rhyme
}
