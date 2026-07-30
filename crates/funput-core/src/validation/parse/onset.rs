//! Onset (âm đầu) inventory and matching.
//!
//! Matching is longest-first and **tone-blind on the glide**: while composing, a
//! tone key pressed before the nucleus parks itself on the `qu`/`gi` glide
//! (`gi` + `s` → `gí`), and that transient must still parse as the same onset —
//! otherwise `gío` reads as `g` + rhyme `io`, which no Vietnamese syllable has,
//! and the engine restores the raw keystrokes mid-word.

use crate::orthography::glide::{self, Glide};
use crate::unicode::marks::is_vowel;

const VALID_ONSETS: &[&str] = &[
    "b", "c", "ch", "d", "g", "gh", "gi", "h", "k", "kh", "l", "m", "n", "ng", "ngh", "nh", "p",
    "ph", "qu", "r", "s", "t", "th", "tr", "v", "x",
];

/// Consonant-cluster onsets that occur only in Central Highlands (Tây Nguyên)
/// toponyms borrowed from Ê Đê / Jarai / Bahnar / M'Nông (`Pleiku`, `Krông`,
/// `Glong`, `Blơr`, `Drăng`). Not native Vietnamese onsets — kept separate so the
/// inventory above stays "pure Vietnamese". A valid rhyme is still required after
/// the onset, so this barely affects English auto-restore.
const ETHNIC_ONSETS: &[&str] = &["bl", "br", "dr", "gl", "gr", "kl", "kr", "pl", "pr"];

/// True if `onset` is a valid Vietnamese onset (`đ` included, any case), or a
/// Tây Nguyên toponym cluster ([`ETHNIC_ONSETS`]).
pub(crate) fn is_valid_onset(onset: &str) -> bool {
    onset.is_empty()
        || onset == "đ"
        || onset == "Đ"
        || glide::in_onset(onset).is_some()
        || VALID_ONSETS.iter().any(|o| onset.eq_ignore_ascii_case(o))
        || ETHNIC_ONSETS.iter().any(|o| onset.eq_ignore_ascii_case(o))
}

/// Split `buffer` into (onset, rest, invalid_onset).
pub(super) fn match_onset(buffer: &str) -> (&str, &str, bool) {
    let Some(first) = buffer.chars().next() else {
        return ("", buffer, false);
    };
    if first == 'đ' || first == 'Đ' {
        let (onset, rest) = buffer.split_at(first.len_utf8());
        return (onset, rest, false);
    }

    // Longest onset first (3 chars: `ngh`), then shorter.
    for len in (1..=3).rev() {
        let Some(split) = after_n_chars(buffer, len) else {
            continue;
        };
        let (prefix, rest) = buffer.split_at(split);
        if prefix.is_empty() || !is_valid_onset(prefix) {
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
