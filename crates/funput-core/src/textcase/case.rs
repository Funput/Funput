//! UPPERCASE and lowercase.
//!
//! Thin on purpose. `str::to_uppercase` is already the right answer for
//! Vietnamese, and the reasons are worth writing down once rather than
//! rediscovering them in a shell:
//!
//! - It is **locale-independent** — full Unicode case mapping, no tailoring. The
//!   famous counter-example is Turkish dotless `ı`, and there is no Vietnamese
//!   equivalent: every letter here maps the same way in every locale.
//! - It works on the **string**, not on each `char`. Case mapping is allowed to
//!   change how many characters a word takes, so a `chars().map()` version would be
//!   wrong in general even though it happens to work on the Vietnamese inventory.
//! - It handles **both Unicode forms**. Precomposed `ế` has an uppercase mapping of
//!   its own; in the combining form the mark is caseless and simply rides along on
//!   the uppercased base letter. Either way the output keeps the form it arrived
//!   in, which is what a user who pasted one of them expects to get back.
//!
//! So what is actually here is the decision to use them, and the tests that hold
//! that decision to the Vietnamese inventory.

use super::Options;

/// `Xin chào Việt Nam` → `XIN CHÀO VIỆT NAM`.
pub(super) fn upper(text: &str, _: Options) -> String {
    text.to_uppercase()
}

/// `Xin Chào Việt Nam` → `xin chào việt nam`.
pub(super) fn lower(text: &str, _: Options) -> String {
    text.to_lowercase()
}

#[cfg(test)]
mod tests {
    use super::*;

    /// The combining spelling of `Tiếng Việt`: every tone and shape as its own
    /// code point, the way NFD text from a macOS filesystem or a web form arrives.
    const COMBINING: &str = "Tie\u{302}\u{301}ng Vie\u{323}\u{302}t";
    const COMBINING_UPPER: &str = "TIE\u{302}\u{301}NG VIE\u{323}\u{302}T";
    const COMBINING_LOWER: &str = "tie\u{302}\u{301}ng vie\u{323}\u{302}t";

    fn opts() -> Options {
        Options::default()
    }

    #[test]
    fn the_documented_examples() {
        assert_eq!(upper("Xin chào Việt Nam", opts()), "XIN CHÀO VIỆT NAM");
        assert_eq!(lower("Xin Chào Việt Nam", opts()), "xin chào việt nam");
    }

    #[test]
    fn every_vowel_and_the_stroke_survive_a_round_trip() {
        let all = "aăâeêioôơuưy áắấéếíóốớúứý ạặậẹệịọộợụựỵ đ";
        assert_eq!(lower(&upper(all, opts()), opts()), all);
    }

    #[test]
    fn stroke_d_has_an_uppercase() {
        assert_eq!(upper("đẹp", opts()), "ĐẸP");
        assert_eq!(lower("ĐẸP", opts()), "đẹp");
    }

    #[test]
    fn the_combining_form_is_not_precomposed_on_the_way_out() {
        assert_eq!(upper(COMBINING, opts()), COMBINING_UPPER);
        assert_eq!(lower(COMBINING, opts()), COMBINING_LOWER);
        // The point of the test: the marks stay separate. A caller who pasted NFD
        // gets NFD back, so nothing downstream has to re-detect the form.
        assert_ne!(upper(COMBINING, opts()), "TIẾNG VIỆT");
    }

    #[test]
    fn text_that_has_no_case_is_left_alone() {
        for text in ["こんにちは", "🎉 1.5 —", ""] {
            assert_eq!(upper(text, opts()), text);
            assert_eq!(lower(text, opts()), text);
        }
    }

    #[test]
    fn both_are_idempotent() {
        let text = "Xin chào Việt Nam";
        assert_eq!(upper(&upper(text, opts()), opts()), upper(text, opts()));
        assert_eq!(lower(&lower(text, opts()), opts()), lower(text, opts()));
    }
}
