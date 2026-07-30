//! One-pass, allocation-free scan of the nucleus vowel cluster.

use crate::orthography::glide;
use crate::unicode::marks::{is_vowel, vowel_stem};
use crate::unicode::shapes::shape_on_vowel;

/// Summary of the nucleus vowel cluster (a contiguous vowel run, so its char
/// positions are simply `start..start + len`).
pub(super) struct ClusterScan {
    /// Char index of the first cluster vowel (onset glide already dropped).
    pub(super) start: usize,
    pub(super) len: usize,
    /// First two cluster vowels (after the glide drop).
    pub(super) first: char,
    pub(super) second: Option<char>,
    /// Char index of the last shaped (mũ/móc/trần) vowel in the cluster.
    pub(super) last_shaped: Option<usize>,
    /// A consonant follows the cluster.
    pub(super) has_coda: bool,
}

pub(super) fn scan_cluster(buffer: &str) -> Option<ClusterScan> {
    let mut onset = buffer; // everything before the cluster
    let mut start = 0;
    let mut len = 0;
    let mut vowels = [None; 3]; // first three cluster vowels
    let mut last_shaped = None;
    let mut has_coda = false;

    for (i, (offset, ch)) in buffer.char_indices().enumerate() {
        if is_vowel(ch) {
            if len == 0 {
                start = i;
                onset = &buffer[..offset];
            }
            if len < vowels.len() {
                vowels[len] = Some(ch);
            }
            if shape_on_vowel(ch).is_some() {
                last_shaped = Some(i);
            }
            len += 1;
        } else if len > 0 {
            has_coda = true;
            break;
        }
    }

    // Drop the onset glide of `qu`/`gi` when a real nucleus vowel follows
    // (e.g. `qua` → tone on `a`, `gia` → tone on `a`), but keep it when it is
    // the only vowel (e.g. `gì`, `gìn`). The glide may itself carry a transient
    // tone here (`gí` + `a`), which is exactly what [`glide::after`] sees through;
    // it is never *shaped*, so `last_shaped` is unaffected either way.
    if len >= 2 && glide::after(onset, vowels[0]?).is_some() {
        start += 1;
        len -= 1;
        vowels = [vowels[1], vowels[2], None];
    }

    Some(ClusterScan {
        start,
        len,
        first: vowels[0]?,
        second: vowels[1],
        last_shaped,
        has_coda,
    })
}

/// True for the open glide-initial diphthongs where "kiểu mới" moves the tone onto
/// the second (main) vowel: `oa`, `oe`, `uy` — the only syllables on which the
/// modern and traditional styles disagree. Compared on the stem so a vowel that
/// already carries a tone (reposition) still matches (`hoà` → `o`, `à`→stem `a`).
pub(super) fn modern_open_pair_takes_second(first: char, second: char) -> bool {
    let f = vowel_stem(first).unwrap_or(first).to_ascii_lowercase();
    let s = vowel_stem(second).unwrap_or(second).to_ascii_lowercase();
    matches!((f, s), ('o', 'a') | ('o', 'e') | ('u', 'y'))
}

/// Which vowel in the cluster receives the tone (0-based within cluster) — the
/// **traditional** ("kiểu cũ") rule. Only consulted for clusters with no shaped
/// vowel (those take priority in [`super::tone_vowel_index`]):
///
/// - 1 vowel → on it.
/// - 2 vowels, **open** (no final consonant) → first vowel: `hòa`, `ùy`, `mía`.
/// - 2 vowels **+ final consonant**, or 3 vowels → second vowel: `toán`, `ngoài`.
pub(super) fn tone_offset_in_cluster(cluster_len: usize, has_coda: bool) -> usize {
    match cluster_len {
        1 => 0,
        2 if has_coda => 1,
        2 => 0,
        _ => 1, // triphthong → middle vowel
    }
}
