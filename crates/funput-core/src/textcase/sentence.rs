//! Viết hoa đầu câu.
//!
//! Capitalises the first letter of each sentence and **leaves the rest alone**.
//! Lowercasing the rest first was considered and rejected: it would flatten
//! `TP. HCM`, proper nouns, and anything capitalised on purpose, and a user who
//! wants that can press lowercase and then this.
//!
//! A sentence begins at the start of the text, after a newline, and after one of
//! `.` `!` `?` `…` **followed by whitespace**. Two rules fall out of that shape
//! rather than being written down separately: `1.5` keeps its `.` because a digit
//! follows it rather than a space, and a run of `...` or `?!` ends one sentence
//! rather than several.
//!
//! A newline counts even with no punctuation before it, because the text this
//! transform is pointed at is usually a list, a subtitle file, or notes — places
//! where nobody punctuates the ends of lines.
//!
//! **Where the first letter is.** Openers are skipped, so `"xin chào"` and
//! `(xin chào)` both get their `x`. Digits are not: a sentence that opens with a
//! number has already begun, and skipping past it would turn `3 con mèo` into
//! `3 Con mèo`. So the search stops at the first letter *or* digit, and only a
//! letter is changed.
//!
//! **The known limitation.** An abbreviation's full stop is indistinguishable from
//! the end of a sentence — `v.v. nhé` becomes `V.v. Nhé`, `TS. nguyễn` becomes
//! `TS. Nguyễn`. A list of Vietnamese abbreviations was considered and left out of
//! `docs/features/text-case.md` on purpose: no list is ever complete, and its
//! mistakes are harder to predict than this one rule, which a user sees in the
//! preview. The tests below pin the behaviour so it stays a decision.

use super::Options;

/// The characters that can end a sentence.
const TERMINATORS: [char; 4] = ['.', '!', '?', '…'];

/// `xin chào. hôm nay trời đẹp.` → `Xin chào. Hôm nay trời đẹp.`
pub(super) fn capitalize(text: &str, _: Options) -> String {
    let mut out = String::with_capacity(text.len());
    // The start of the text is the start of a sentence.
    let mut awaiting = true;
    // A terminator has been seen and is waiting for the whitespace that confirms it.
    let mut terminated = false;

    for c in text.chars() {
        if awaiting && c.is_alphabetic() {
            out.extend(c.to_uppercase());
            awaiting = false;
            terminated = false;
            continue;
        }
        out.push(c);
        match c {
            '\n' => {
                awaiting = true;
                terminated = false;
            }
            c if TERMINATORS.contains(&c) => terminated = true,
            c if c.is_whitespace() => {
                awaiting = awaiting || terminated;
                terminated = false;
            }
            c => {
                // A digit ends the search for a first letter; punctuation does not.
                awaiting = awaiting && !c.is_alphanumeric();
                terminated = false;
            }
        }
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
