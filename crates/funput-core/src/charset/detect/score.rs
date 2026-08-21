//! How well one candidate charset explains the text.
//!
//! The measure is Vietnamese spelling: decode the text as the candidate, split it
//! into words, and count how many are real syllables. A wrong charset turns
//! Vietnamese letters into other letters, and the result stops being spellable
//! almost everywhere — that is the whole signal.

use core::cmp::Reverse;

use crate::charset::Conversion;
use crate::is_complete_syllable;

/// A candidate's showing, ordered so that a larger value is a better explanation.
///
/// The order of the fields is the order of the comparison, and each rung only
/// matters when the ones above it tie:
///
/// 1. `valid` — the real evidence.
/// 2. `Reverse(invalid)` — a denominator. Three syllables out of three beats three
///    out of forty.
/// 3. `Reverse(unmapped)` — the source charset could not place a character.
/// 4. `Reverse(normalized)` — it could, but the source spelled it another way.
///
/// The last rung is what separates the two Unicode charsets on ordinary
/// precomposed text: both read it correctly and score the same syllables, but
/// Unicode tổ hợp reports a rewrite for every toned letter.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub(super) struct Score {
    valid: usize,
    invalid: Reverse<usize>,
    unmapped: Reverse<usize>,
    normalized: Reverse<usize>,
}

impl Score {
    /// Whether enough of the text parses as Vietnamese to be worth believing.
    ///
    /// A majority rule rather than a fixed floor: a fixed one either rejects
    /// legitimately short input — `"Việt Nam"` is two words — or sits too low to
    /// refuse anything. This stops binary data and prose that is not Vietnamese at
    /// all, where a token parses now and then by accident.
    pub(super) fn is_plausible(self) -> bool {
        self.valid > 0 && self.valid > self.invalid.0
    }
}

/// Score a decoding of the text.
pub(super) fn score(out: &Conversion) -> Score {
    let mut valid = 0;
    let mut invalid = 0;
    for word in words(&out.text) {
        if is_complete_syllable(word) {
            valid += 1;
        } else {
            invalid += 1;
        }
    }
    Score {
        valid,
        invalid: Reverse(invalid),
        unmapped: Reverse(out.unmapped),
        normalized: Reverse(out.normalized),
    }
}

/// Split text into candidate words: runs of ASCII letters and non-ASCII.
///
/// Both obvious rules are wrong, each in its own way.
///
/// **Splitting on whitespace and trimming non-letters off the ends** throws away
/// the evidence. TCVN3 gives its commonest letters Latin-1 *symbols* rather than
/// letters — `ả` is `0xB6` (`¶`), `á` is `0xB8` (`¸`), `ơ` is `0xAC` (`¬`) — so
/// `hoả` misread as Unicode is `ho¶`, and trimming leaves `ho`, a perfectly good
/// syllable. The wrong candidate scores by discarding the byte that identified the
/// right one. It also loses the first Vietnamese word of every line of a `gõ tắt`
/// file, whose format is `trigger:expansion` with no space.
///
/// **Splitting on everything that is not a letter** tears `Viê` `t` apart at the
/// combining mark and kills the Unicode tổ hợp candidate outright.
///
/// So: break on ASCII non-letters, keep every non-ASCII character inside the word.
/// Digits, `:`, `/` and punctuation separate; marks and legacy symbols do not.
fn words(text: &str) -> impl Iterator<Item = &str> {
    text.split(|c: char| !(c.is_ascii_alphabetic() || is_word_body(c)))
        .filter(|word| !word.is_empty())
}

/// Whether a non-ASCII char belongs inside a word rather than between two.
///
/// A byte-order mark is neither: it carries no text and would poison the word it
/// landed in.
fn is_word_body(c: char) -> bool {
    !c.is_ascii() && c != '\u{FEFF}'
}

#[cfg(test)]
mod tests {
    use super::*;

    fn split(text: &str) -> Vec<&str> {
        words(text).collect()
    }

    #[test]
    fn a_word_keeps_the_legacy_symbol_that_identifies_its_charset() {
        // `hoả` in TCVN3 misread as Unicode. Trimming the `¶` would leave `ho`,
        // which is a syllable, and hand the wrong candidate a free point.
        assert_eq!(split("ho\u{B6}"), vec!["ho\u{B6}"]);
        assert!(!is_complete_syllable("ho\u{B6}"));
    }

    #[test]
    fn a_word_keeps_its_combining_marks() {
        assert_eq!(split("Viê\u{323}t Nam"), vec!["Viê\u{323}t", "Nam"]);
    }

    /// The `gõ tắt` file format is `trigger:expansion`. Splitting on whitespace
    /// alone would hand the scorer `vn:việt` and lose the Vietnamese word inside.
    #[test]
    fn ascii_punctuation_separates_words() {
        assert_eq!(split("vn:việt nam"), vec!["vn", "việt", "nam"]);
        assert_eq!(split("\"Việt\", (Nam)"), vec!["Việt", "Nam"]);
    }

    /// Non-ASCII punctuation stays *inside* the word, and it has to: `«` is
    /// `U+00AB`, and `0xAB` is how TCVN3 spells `ô`. There is no way to tell a
    /// quotation mark from a letter without already knowing the charset, which is
    /// the question being asked. Keeping it is harmless — the word then fails to
    /// parse under every candidate alike, so it decides nothing either way.
    #[test]
    fn non_ascii_punctuation_cannot_be_stripped() {
        assert_eq!(split("«Việt»"), vec!["«Việt»"]);
    }

    #[test]
    fn digits_are_not_part_of_a_word() {
        assert_eq!(split("1945 2024 30/4"), Vec::<&str>::new());
        assert_eq!(split("Ha Noi 1945"), vec!["Ha", "Noi"]);
    }

    #[test]
    fn a_byte_order_mark_does_not_join_the_word_after_it() {
        assert_eq!(split("\u{FEFF}Việt"), vec!["Việt"]);
    }

    #[test]
    fn a_majority_of_words_has_to_parse() {
        let plausible = Score {
            valid: 2,
            invalid: Reverse(0),
            unmapped: Reverse(0),
            normalized: Reverse(0),
        };
        assert!(plausible.is_plausible(), "two words, both Vietnamese");

        let noise = Score {
            valid: 1,
            invalid: Reverse(40),
            unmapped: Reverse(0),
            normalized: Reverse(0),
        };
        assert!(!noise.is_plausible(), "one accident in forty words");
    }
}
