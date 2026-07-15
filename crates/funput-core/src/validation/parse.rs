//! Split a syllable chunk into onset + vowel nucleus + coda.
//!
//! Runs on the keystroke hot path, so parsing borrows the buffer and never
//! allocates: the onset is a prefix slice; nucleus and coda are filtering
//! iterators over the remainder.

use crate::unicode::marks::is_vowel;

/// Parsed view of a single syllable chunk. The nucleus and coda are
/// *interleaved* selections of the post-onset text — a vowel after a consonant
/// still counts as nucleus (`mixa` → nucleus `ia`, coda `x`) — so they are
/// exposed as iterators rather than slices.
#[derive(Debug, Clone, Copy)]
pub struct SyllableParts<'a> {
    pub onset: &'a str,
    rest: &'a str,
    /// Leading consonants do not form a valid Vietnamese onset.
    pub invalid_onset: bool,
}

impl<'a> SyllableParts<'a> {
    /// The vowel nucleus, in buffer order.
    pub fn nucleus_chars(&self) -> impl Iterator<Item = char> + Clone + 'a {
        self.rest.chars().filter(|&c| is_vowel(c))
    }

    /// The consonant coda, in buffer order.
    pub fn coda_chars(&self) -> impl Iterator<Item = char> + Clone + 'a {
        self.rest.chars().filter(|&c| !is_vowel(c))
    }
}

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
        || VALID_ONSETS.iter().any(|o| onset.eq_ignore_ascii_case(o))
        || ETHNIC_ONSETS.iter().any(|o| onset.eq_ignore_ascii_case(o))
}

/// Parse one syllable chunk into onset, vowel nucleus, and coda.
pub fn parse_syllable(buffer: &str) -> SyllableParts<'_> {
    let (onset, rest, invalid_onset) = match_onset(buffer);
    SyllableParts {
        onset,
        rest,
        invalid_onset,
    }
}

/// Byte offset just past the first `n` chars of `s`, or `None` if `s` is shorter.
fn after_n_chars(s: &str, n: usize) -> Option<usize> {
    let mut chars = s.chars();
    for _ in 0..n {
        chars.next()?;
    }
    Some(s.len() - chars.as_str().len())
}

fn match_onset(buffer: &str) -> (&str, &str, bool) {
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
        // `gìn`), so fall back to the shorter `g` onset.
        if prefix.eq_ignore_ascii_case("gi") && !rest.chars().next().is_some_and(is_vowel) {
            continue;
        }

        return (prefix, rest, false);
    }

    if is_vowel(first) {
        return ("", buffer, false);
    }

    ("", buffer, true)
}

#[cfg(test)]
mod tests {
    use super::*;

    /// (onset, nucleus, coda, invalid_onset) with the iterators materialized.
    fn parts(buffer: &str) -> (String, String, String, bool) {
        let p = parse_syllable(buffer);
        (
            p.onset.into(),
            p.nucleus_chars().collect(),
            p.coda_chars().collect(),
            p.invalid_onset,
        )
    }

    fn ok(onset: &str, nucleus: &str, coda: &str) -> (String, String, String, bool) {
        (onset.into(), nucleus.into(), coda.into(), false)
    }

    #[test]
    fn parse_syllable_cases() {
        assert_eq!(parts("tr"), ok("tr", "", ""));
        assert_eq!(parts("ng"), ok("ng", "", ""));
        assert_eq!(parts("ma"), ok("m", "a", ""));
        assert_eq!(parts("text"), ok("t", "e", "xt"));
        assert_eq!(parts("mix"), ok("m", "i", "x"));
        assert_eq!(parts("trung"), ok("tr", "u", "ng"));
        assert_eq!(parts("đ"), ok("đ", "", ""));
        // `gi` releases its `i` as the nucleus when no vowel follows.
        assert_eq!(parts("gi"), ok("g", "i", ""));
        assert_eq!(parts("gia"), ok("gi", "a", ""));
        // No valid onset → the leading consonants flag the chunk.
        assert_eq!(parts("zt"), ("".into(), "".into(), "zt".into(), true));
        assert_eq!(parts(""), ok("", "", ""));
    }
}
