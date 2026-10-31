//! Matching a syllable typed so far against a whole one, by the marks typed.
//!
//! Telex and VNI put the tone last, and often the vowel shape too: `đươ` comes before
//! `được`, `duo` before `dưới`. Byte or NFC prefixes never see those, because `ơ` and
//! `ợ` are different characters. Here a typed character matches a listed one when it
//! is that character, or that character without its tone, or without any mark at all
//! (`đ` as `d`). A mark that *was* typed still has to be right: `thà` reaches `thành`,
//! not `thầy`.

use unicode_normalization::char::{compose, decompose_canonical};

/// The five tone marks, as canonical decomposition writes them.
const TONES: [char; 5] = ['\u{0300}', '\u{0301}', '\u{0303}', '\u{0309}', '\u{0323}'];

/// Whether `syllable` is what `typed` could still become. Both lowercase NFC: the
/// caller normalizes the prefix once (`normalize::exact_into`), not per syllable.
pub(crate) fn completes(typed: &str, syllable: &str) -> bool {
    let mut letters = syllable.chars();
    typed
        .chars()
        .all(|mark| letters.next().is_some_and(|c| matches(mark, c)))
}

fn matches(typed: char, listed: char) -> bool {
    if typed == listed {
        return true;
    }
    let (toneless, bare) = stripped(listed);
    typed == toneless || typed == bare
}

/// `c` without its tone, and `c` without any mark. A Vietnamese letter carries at
/// most one shape mark and one tone, so one kept mark is all there is to recompose.
fn stripped(c: char) -> (char, char) {
    let mut base = None;
    let mut shape = None;
    decompose_canonical(c, |part| {
        if base.is_none() {
            base = Some(part);
        } else if !TONES.contains(&part) {
            shape = Some(part);
        }
    });
    let base = base.unwrap_or(c);
    let toneless = shape.and_then(|mark| compose(base, mark)).unwrap_or(base);
    let bare = if base == 'đ' { 'd' } else { base };
    (toneless, bare)
}

#[cfg(test)]
mod tests {
    use super::completes;

    #[test]
    fn a_mark_not_yet_typed_still_matches() {
        for (prefix, syllable) in [
            ("đươ", "được"),
            ("đươ", "đường"),
            ("duo", "được"),
            ("duo", "dưới"),
            ("duo", "đuôi"),
            ("thà", "thành"),
        ] {
            assert!(completes(prefix, syllable), "{prefix} → {syllable}");
        }
    }

    #[test]
    fn a_mark_that_was_typed_has_to_be_right() {
        for (prefix, syllable) in [
            ("thà", "thầy"),
            ("đươ", "đuôi"),
            ("đu", "dư"),
            ("ă", "ân"),
            ("dươc", "dưới"),
            ("được", "đư"),
        ] {
            assert!(!completes(prefix, syllable), "{prefix} ↛ {syllable}");
        }
    }
}
