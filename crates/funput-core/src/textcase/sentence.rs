//! Viết hoa đầu câu.
//!
//! Capitalises the first letter of each sentence and **leaves the rest alone**.
//! Lowercasing the rest first was considered and rejected: it would flatten
//! `TP. HCM`, proper nouns, and anything capitalised on purpose, and a user who
//! wants that can press lowercase and then this.
//!
//! Where a sentence begins is [`crate::sentence`]'s answer, not this module's: a
//! keyboard asks the same question of the text behind its caret, and two copies of
//! the rules would drift. What stays here is the walk that rewrites the text.
//!
//! **Where the first letter is.** Openers are skipped, so `"xin chào"` and
//! `(xin chào)` both get their `x`. Digits are not: a sentence that opens with a
//! number has already begun, and skipping past it would turn `3 con mèo` into
//! `3 Con mèo`. So the search stops at the first letter *or* digit, and only a
//! letter is changed.
//!
//! **The known limitation.** This transform reads every full stop as an ending, so
//! an abbreviation is caught in the crossfire — `v.v. nhé` becomes `V.v. Nhé`,
//! `TS. nguyễn` becomes `TS. Nguyễn`. [`Rules::TYPING`] guards the first of those
//! and this transform deliberately does not take that guard: a list of Vietnamese
//! abbreviations was considered and left out of `docs/features/text-case.md` on
//! purpose, and the partial rule would trade one predictable miss for two. A user
//! sees this one in the preview. The tests below pin the behaviour so it stays a
//! decision.

use super::Options;
use crate::sentence::{Rules, Scanner};

/// `xin chào. hôm nay trời đẹp.` → `Xin chào. Hôm nay trời đẹp.`
pub(super) fn capitalize(text: &str, _: Options) -> String {
    let mut out = String::with_capacity(text.len());
    let mut scanner = Scanner::new(Rules::TRANSFORM);

    for c in text.chars() {
        if scanner.opens_sentence(c) {
            out.extend(c.to_uppercase());
        } else {
            out.push(c);
        }
        scanner.push(c);
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    fn opts() -> Options {
        Options::default()
    }

    #[test]
    fn the_documented_example() {
        assert_eq!(
            capitalize("xin chào. hôm nay trời đẹp.", opts()),
            "Xin chào. Hôm nay trời đẹp."
        );
    }

    #[test]
    fn the_rest_of_the_sentence_is_untouched() {
        assert_eq!(capitalize("xin CHÀO. hôm nay", opts()), "Xin CHÀO. Hôm nay");
        assert_eq!(capitalize("gửi về TP. hcm", opts()), "Gửi về TP. Hcm");
    }

    #[test]
    fn a_newline_is_a_boundary_without_punctuation() {
        assert_eq!(capitalize("một\nhai\n\nba", opts()), "Một\nHai\n\nBa");
    }

    #[test]
    fn an_opener_is_skipped_but_a_digit_is_not() {
        assert_eq!(capitalize("\"xin chào\"", opts()), "\"Xin chào\"");
        assert_eq!(capitalize("(xin chào)", opts()), "(Xin chào)");
        assert_eq!(capitalize("— xin chào", opts()), "— Xin chào");
        assert_eq!(
            capitalize("3 con mèo. 5 con chó.", opts()),
            "3 con mèo. 5 con chó."
        );
    }

    /// The quote is transparent, not a boundary: `xin` stays mid-sentence and only
    /// `rồi`, on the far side of the full stop, is a new one.
    #[test]
    fn a_closing_quote_does_not_hide_the_ending_behind_it() {
        assert_eq!(
            capitalize("anh ấy nói \"xin chào.\" rồi đi", opts()),
            "Anh ấy nói \"xin chào.\" Rồi đi"
        );
        assert_eq!(capitalize("(xong.) tiếp", opts()), "(Xong.) Tiếp");
    }

    #[test]
    fn a_terminator_needs_whitespace_after_it() {
        assert_eq!(capitalize("giá 1.5 triệu", opts()), "Giá 1.5 triệu");
        assert_eq!(capitalize("thật?! không tin", opts()), "Thật?! Không tin");
        assert_eq!(capitalize("rồi… thôi", opts()), "Rồi… Thôi");
    }

    /// Not a bug list: the module doc says why a rule beats a list of abbreviations.
    #[test]
    fn an_abbreviation_reads_as_the_end_of_a_sentence() {
        assert_eq!(capitalize("giấy tờ v.v. nhé", opts()), "Giấy tờ v.v. Nhé");
        assert_eq!(capitalize("TS. nguyễn", opts()), "TS. Nguyễn");
    }

    #[test]
    fn the_combining_form_capitalizes_its_base_letter() {
        assert_eq!(
            capitalize("vie\u{323}\u{302}t nam", opts()),
            "Vie\u{323}\u{302}t nam"
        );
    }

    #[test]
    fn text_with_no_letters_is_left_alone() {
        for text in ["", "… !? 1.5", "🎉", "こんにちは"] {
            assert_eq!(capitalize(text, opts()), text);
        }
    }
}
