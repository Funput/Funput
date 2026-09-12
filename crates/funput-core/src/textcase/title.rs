//! Viết Hoa Đầu Mỗi Từ.
//!
//! Capitalises the first letter of each word and lowercases the rest of it, with one
//! exception: a word that is already all-caps is left exactly as it is, so `TP. HCM`
//! does not become `Tp. Hcm`. That exception is [`Options::keep_all_caps`], on by
//! default and switchable, because a document shouting in capitals is the one case
//! where a user does want them flattened.
//!
//! No list of little words. Vietnamese has no title-case convention that keeps `của`
//! or `và` lowercase — that is an English rule — so every word is capitalised and
//! nothing has to guess.
//!
//! **What separates two words: whitespace, or ASCII punctuation.** `bàn-phím` becomes
//! `Bàn-Phím` and `e-mail` becomes `E-Mail`, which is what every tool of this kind
//! does and what users expect. The apostrophe is the one exception, so `don't` stays
//! `Don't` rather than becoming `Don'T`.
//!
//! Keeping the separators ASCII buys a guarantee worth more than the edge cases it
//! costs: **a word can never be split inside a grapheme cluster**, because every
//! combining mark is non-ASCII. `Tiếng Việt` spelled in NFD stays two words, where a
//! rule of "split on anything that is not a letter" would cut each vowel off from its
//! own tone mark and capitalise the fragment after it. The cost is that unspaced
//! non-ASCII punctuation does not split a word — `xin…chào` capitalises only the `x` —
//! which is cosmetic, and rare in the text this transform is pointed at.
//!
//! Because a word may open with something that has no case, the search is for the
//! first *letter* in the word: `"xin` and `(xin` get their `x`, and `5g` becomes `5G`.
//!
//! **The known limitation** is the mirror of the all-caps exception: a word with a
//! capital inside it loses it, so `iPhone` becomes `Iphone`. Preserving interior
//! capitals would mean not lowercasing the rest of a word, which is the whole job.

use super::Options;

/// `bàn phím tiếng Việt` → `Bàn Phím Tiếng Việt`.
pub(super) fn capitalize(text: &str, options: Options) -> String {
    let mut out = String::with_capacity(text.len());
    let mut word = String::new();
    for c in text.chars() {
        if separates(c) {
            flush(&mut word, &mut out, options);
            out.push(c);
        } else {
            word.push(c);
        }
    }
    flush(&mut word, &mut out, options);
    out
}

/// Whether `c` ends the word before it. ASCII only — see the module doc.
fn separates(c: char) -> bool {
    c.is_whitespace() || (c.is_ascii_punctuation() && c != '\'')
}

/// Write `word` in its title-case form and empty it.
fn flush(word: &mut String, out: &mut String, options: Options) {
    if word.is_empty() {
        return;
    }
    if options.keep_all_caps && is_all_caps(word) {
        out.push_str(word);
    } else {
        let mut seen_letter = false;
        for c in word.chars() {
            match (seen_letter, c.is_alphabetic()) {
                (false, true) => {
                    out.extend(c.to_uppercase());
                    seen_letter = true;
                }
                (true, _) => out.extend(c.to_lowercase()),
                // Before the first letter — digits, a leading mark — nothing to case.
                (false, false) => out.push(c),
            }
        }
    }
    word.clear();
}

/// Whether `word` holds at least one letter and none of them are lowercase.
///
/// A script with no case at all (Japanese, digits alone) answers yes for the letters
/// it has, which costs nothing: the alternative branch would leave it unchanged too.
fn is_all_caps(word: &str) -> bool {
    let mut letters = word.chars().filter(|c| c.is_alphabetic()).peekable();
    letters.peek().is_some() && !letters.any(char::is_lowercase)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn opts() -> Options {
        Options::default()
    }

    fn flattening() -> Options {
        Options {
            keep_all_caps: false,
            ..Options::default()
        }
    }

    #[test]
    fn the_documented_example() {
        assert_eq!(
            capitalize("bàn phím tiếng Việt", opts()),
            "Bàn Phím Tiếng Việt"
        );
    }

    #[test]
    fn an_all_caps_word_is_left_alone_unless_the_switch_is_off() {
        assert_eq!(capitalize("gửi về TP. HCM", opts()), "Gửi Về TP. HCM");
        assert_eq!(capitalize("gửi về TP. HCM", flattening()), "Gửi Về Tp. Hcm");
    }

    #[test]
    fn the_rest_of_a_word_is_lowercased() {
        assert_eq!(capitalize("xIN cHÀO", opts()), "Xin Chào");
        assert_eq!(capitalize("ĐẸP quá", flattening()), "Đẹp Quá");
    }

    #[test]
    fn ascii_punctuation_splits_words_and_the_apostrophe_does_not() {
        assert_eq!(
            capitalize("bàn-phím tiếng việt", opts()),
            "Bàn-Phím Tiếng Việt"
        );
        assert_eq!(capitalize("e-mail", opts()), "E-Mail");
        assert_eq!(capitalize("don't", opts()), "Don't");
        assert_eq!(capitalize("\"xin chào\"", opts()), "\"Xin Chào\"");
        assert_eq!(capitalize("(xin chào)", opts()), "(Xin Chào)");
    }

    #[test]
    fn the_search_is_for_the_first_letter_not_the_first_character() {
        assert_eq!(capitalize("5g covid19", opts()), "5G Covid19");
        assert_eq!(capitalize("giá 1.5 triệu", opts()), "Giá 1.5 Triệu");
    }

    /// The guarantee the ASCII-only separator rule buys: NFD text keeps its words.
    #[test]
    fn a_word_is_never_split_inside_a_grapheme_cluster() {
        assert_eq!(
            capitalize("tie\u{302}\u{301}ng vie\u{323}\u{302}t", opts()),
            "Tie\u{302}\u{301}ng Vie\u{323}\u{302}t"
        );
    }

    /// Not a bug list: the module doc says why interior capitals cannot survive a
    /// transform whose job is to lowercase the rest of the word.
    #[test]
    fn an_interior_capital_is_lost() {
        assert_eq!(capitalize("iPhone", opts()), "Iphone");
    }

    #[test]
    fn text_with_no_letters_is_left_alone() {
        for text in ["", "… !? 1.5", "🎉", "こんにちは"] {
            assert_eq!(capitalize(text, opts()), text);
        }
    }
}
