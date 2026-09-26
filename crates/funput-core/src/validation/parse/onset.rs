//! Onset (âm đầu) inventory and matching.
//!
//! Matching is longest-first and **tone-blind on the glide**: while composing, a
//! tone key pressed before the nucleus parks itself on the `qu`/`gi` glide
//! (`gi` + `s` → `gí`), and that transient must still parse as the same onset —
//! otherwise `gío` reads as `g` + rhyme `io`, which no Vietnamese syllable has,
//! and the engine restores the raw keystrokes mid-word.

use crate::orthography::glide::{self, Glide};
use crate::unicode::marks::is_vowel;
use crate::validation::ethnic::cluster_kind;

/// The native onsets. Tây Nguyên name clusters (`kr`, `kp`, `đr`) are kept apart
/// in [`crate::validation::ethnic`], so this inventory stays "pure Vietnamese".
const VALID_ONSETS: &[&str] = &[
    "b", "c", "ch", "d", "g", "gh", "gi", "h", "k", "kh", "l", "m", "n", "ng", "ngh", "nh", "p",
    "ph", "qu", "r", "s", "t", "th", "tr", "v", "x",
];

/// True if `onset` is a valid Vietnamese onset (`đ` included, any case), or a
/// Tây Nguyên name cluster.
pub(crate) fn is_valid_onset(onset: &str) -> bool {
    is_native(onset)
        || cluster_kind(onset).is_some()
        // Last: plain `qu`/`gi` already matched above, so this only rescues the
        // toned transient (`qú`, `gí`) and stays off the common path.
        || glide::in_onset(onset).is_some()
}

/// A native onset as spelled, `đ` included, in any case.
fn is_native(onset: &str) -> bool {
    onset.is_empty()
        || onset == "đ"
        || onset == "Đ"
        || VALID_ONSETS.iter().any(|o| onset.eq_ignore_ascii_case(o))
}

/// Split `buffer` into (onset, rest, invalid_onset).
pub(super) fn match_onset(buffer: &str) -> (&str, &str, bool) {
    let Some(first) = buffer.chars().next() else {
        return ("", buffer, false);
    };
    // An onset is a run of consonants — the `qu`/`gi` glides aside, whose vowel
    // belongs to the onset — so a prefix reaching past the run (`tiê`, `ti` of
    // `tiếng`) cannot be one. Counting the run once skips those tries.
    let consonants = buffer.chars().take(3).take_while(|&c| !is_vowel(c)).count();
    let longest = if matches!(first, 'q' | 'Q' | 'g' | 'G') {
        3
    } else {
        consonants.max(1)
    };

    // Longest onset first (3 chars: `ngh`, `kđr`), then shorter.
    for len in (1..=longest).rev() {
        let Some(split) = after_n_chars(buffer, len) else {
            continue;
        };
        let (prefix, rest) = buffer.split_at(split);
        let valid = is_native(prefix)
            || (len <= consonants && cluster_kind(prefix).is_some())
            || glide::in_onset(prefix).is_some();
        if prefix.is_empty() || !valid {
            continue;
        }

        // In `gi`, the `i` is part of the onset only when another vowel follows
        // (`gia`, `giết`). When `i` is the lone vowel it is the nucleus (`gì`,
        // `gìn`), so fall back to the shorter `g` onset. `qu` has no such case:
        // no Vietnamese syllable is `q` + a `u` nucleus.
        if glide::in_onset(prefix) == Some(Glide::Gi) && !rest.chars().next().is_some_and(is_vowel)
        {
            continue;
        }

        return (prefix, rest, false);
    }

    if is_vowel(first) {
        return ("", buffer, false);
    }

    ("", buffer, true)
}

/// Byte offset just past the first `n` chars of `s`, or `None` if `s` is shorter.
fn after_n_chars(s: &str, n: usize) -> Option<usize> {
    let mut chars = s.chars();
    for _ in 0..n {
        chars.next()?;
    }
    Some(s.len() - chars.as_str().len())
}
