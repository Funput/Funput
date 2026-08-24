//! Properties that must hold for text no fixture would think to write down —
//! arbitrary byte soup, half-words, unassigned codes, marks with nothing to
//! attach to.

use funput_core::charset::{Charset, convert};
use proptest::prelude::*;

/// Anything a legacy document could hold: one char per byte, the whole range.
const LEGACY_TEXT: &str = "[\\x{20}-\\x{FF}]{0,64}";

const LEGACY: [Charset; 2] = [Charset::Tcvn3, Charset::VniWindows];

proptest! {
    /// Conversion is total. Whatever the text is — and a user who picks the wrong
    /// source charset feeds it text that is *not* what it claims — it comes back
    /// as text, not as a panic.
    #[test]
    fn conversion_never_panics(text in LEGACY_TEXT) {
        for from in [Charset::Unicode, Charset::Tcvn3, Charset::VniWindows] {
            for to in [Charset::Unicode, Charset::Tcvn3, Charset::VniWindows] {
                let _ = convert(&text, from, to);
            }
        }
    }

    /// The contract `unmapped` states, checked rather than asserted in prose: when
    /// no character was approximated at either end, the conversion is exactly
    /// reversible. A table entry carrying two letters would break this.
    ///
    /// It is also the regression net for VNI's lenient mixed-case reading. A
    /// decoder that took `61 D9` as `á` with `recognized = true` would re-encode
    /// it as `61 F9` and fail here; counting it is what keeps the guard honest.
    #[test]
    fn an_exact_conversion_is_reversible(text in LEGACY_TEXT) {
        for charset in LEGACY {
            let unicode = convert(&text, charset, Charset::Unicode);
            let back = convert(&unicode.text, Charset::Unicode, charset);
            if unicode.unmapped == 0 && back.unmapped == 0 {
                prop_assert_eq!(&back.text, &text, "through {:?}", charset);
            }
        }
    }

    /// TCVN3 spells every letter in one code, so a conversion to or from it moves
    /// no character boundaries. This is a property of TCVN3, not of the module —
    /// VNI-Windows spells most letters in two, and `a_letter_can_be_two_characters`
    /// in its codec tests states the opposite for it.
    #[test]
    fn tcvn3_conversion_preserves_the_character_count(text in LEGACY_TEXT) {
        let decoded = convert(&text, Charset::Tcvn3, Charset::Unicode);
        prop_assert_eq!(decoded.text.chars().count(), text.chars().count());

        let encoded = convert(&text, Charset::Unicode, Charset::Tcvn3);
        prop_assert_eq!(encoded.text.chars().count(), text.chars().count());
    }

    /// Nothing is ever dropped. `>=` rather than `==` is the honest statement of
    /// that, and the one the module actually promises: encoding to VNI can make
    /// text longer, and only TCVN3 keeps the count.
    #[test]
    fn no_character_is_ever_dropped(text in "\\PC{0,64}") {
        for charset in LEGACY {
            let out = convert(&text, Charset::Unicode, charset);
            prop_assert!(out.text.chars().count() >= text.chars().count());
            prop_assert!(out.unmapped <= text.chars().count());
        }
    }

    /// Decoding a legacy charset can only ever join characters, never invent them:
    /// two bytes may become one letter, but one byte never becomes two. A decoder
    /// that mistook a missing second char for a zero byte would break this by
    /// consuming past the end.
    #[test]
    fn decoding_never_invents_characters(text in LEGACY_TEXT) {
        for charset in LEGACY {
            let out = convert(&text, charset, Charset::Unicode);
            prop_assert!(
                out.text.chars().count() <= text.chars().count(),
                "{:?} grew {:?} into {:?}", charset, text, out.text
            );
        }
    }

    /// A legacy document has to be storable as bytes. Any character in the output
    /// above `U+00FF` could not have come from the charset, so it must have been
    /// counted — otherwise `decode_bytes` would hand back text a file cannot hold.
    #[test]
    fn a_legacy_charset_writes_only_bytes_or_reports_it(text in "\\PC{0,64}") {
        for charset in LEGACY {
            let out = convert(&text, Charset::Unicode, charset);
            let wide = out.text.chars().filter(|c| *c > '\u{FF}').count();
            prop_assert!(wide <= out.unmapped, "{:?} wrote {} unreported wide chars", charset, wide);
        }
    }
}
