//! Bỏ dấu tiếng Việt.
//!
//! Three kinds of character and one switch. A vowel loses its tone and its shape
//! (`ế` → `e`, `ơ` → `o`, `Ữ` → `U`); a combining mark is dropped, which is how the
//! same word spelled in NFD arrives at the same answer; `đ` is asked about, because
//! it is a letter of the alphabet and not a `d` carrying something. Everything else
//! is passed through untouched.
//!
//! **Why there is no table here.** [`shapes::base_vowel`] already strips tone and
//! shape while keeping case, over the whole precomposed inventory — it is what the
//! typing engine consults to decide whether a shape key should switch a vowel. A
//! `family → ASCII` table would be a second copy of that answer, and the second
//! copy is the one that goes stale.
//!
//! **Two things it cannot do, both inherent.** `é` is one code point whether it was
//! typed for Vietnamese or for French, so `café` becomes `cafe`; a transform that
//! works on letters cannot know which language meant them. And a combining mark is
//! dropped whoever put it there, so NFD `ñ` comes out as `n` — while precomposed
//! `ñ`, `ü`, `ç`, `ß` and anything outside the inventory keep everything they have.
//! Both are tested below so that they stay decisions rather than surprises.

use crate::unicode::{combining, marks, shapes};

use super::Options;

/// `Tiếng Việt rất đẹp` → `Tieng Viet rat dep`.
pub(super) fn strip(text: &str, options: Options) -> String {
    // Never longer than the input in bytes: every replacement is ASCII, and the
    // only other outcomes are a passthrough and a deletion.
    let mut out = String::with_capacity(text.len());
    for c in text.chars() {
        if combining::is_mark(c) {
            continue;
        }
        if let Some(base) = shapes::base_vowel(c) {
            out.push(base);
        } else if let Some(d) = marks::unstroke_d(c).filter(|_| options.d_to_ascii) {
            out.push(d);
        } else {
            out.push(c);
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    /// `Tiếng Việt` with every tone and shape as its own code point.
    const COMBINING: &str = "Tie\u{302}\u{301}ng Vie\u{323}\u{302}t";

    fn opts() -> Options {
        Options::default()
    }

    fn keeping_d() -> Options {
        Options {
            d_to_ascii: false,
            ..Options::default()
        }
    }

    #[test]
    fn the_documented_example() {
        assert_eq!(strip("Tiếng Việt rất đẹp", opts()), "Tieng Viet rat dep");
    }

    #[test]
    fn the_whole_inventory_becomes_ascii() {
        let all = "aăâeêioôơuưy áắấéếíóốớúứý ạặậẹệịọộợụựỵ ÁẮẤÉẾÍÓỐỚÚỨÝ đĐ";
        let bare = strip(all, opts());
        assert!(bare.is_ascii(), "{bare}");
        assert_eq!(
            bare,
            "aaaeeiooouuy aaaeeiooouuy aaaeeiooouuy AAAEEIOOOUUY dD"
        );
    }

    #[test]
    fn case_survives() {
        assert_eq!(strip("ĐẸP Ữ Ế", opts()), "DEP U E");
    }

    #[test]
    fn the_stroke_is_a_switch_and_the_tones_are_not() {
        assert_eq!(strip("đẹp", keeping_d()), "đep");
        assert_eq!(strip("ĐẸP", keeping_d()), "ĐEP");
    }

    #[test]
    fn both_unicode_forms_reach_the_same_answer() {
        assert_eq!(strip(COMBINING, opts()), strip("Tiếng Việt", opts()));
        assert_eq!(strip(COMBINING, opts()), "Tieng Viet");
    }

    /// Not a bug list — the module doc explains why a letter-level transform cannot
    /// do better, and these hold the behaviour still.
    #[test]
    fn what_it_cannot_separate() {
        assert_eq!(strip("café", opts()), "cafe");
        assert_eq!(strip("n\u{303}", opts()), "n");
        assert_eq!(strip("ñ ü ç ß", opts()), "ñ ü ç ß");
    }

    #[test]
    fn everything_else_is_passed_through() {
        for text in ["こんにちは 🎉", "1.5 — TP. HCM", ""] {
            assert_eq!(strip(text, opts()), text);
        }
    }

    #[test]
    fn stripping_twice_changes_nothing() {
        let once = strip("Tiếng Việt rất đẹp", opts());
        assert_eq!(strip(&once, opts()), once);
    }
}
