//! Properties that must hold for text no fixture would think to write down —
//! arbitrary byte soup, half-words, unassigned codes.

use funput_core::charset::{Charset, convert};
use proptest::prelude::*;

/// Anything a legacy document could hold: one char per byte, the whole range.
const LEGACY_TEXT: &str = "[\\x{20}-\\x{FF}]{0,64}";

proptest! {
    /// Conversion is total. Whatever the text is — and a user who picks the wrong
    /// source charset feeds it text that is *not* what it claims — it comes back
    /// as text, not as a panic.
    #[test]
    fn conversion_never_panics(text in LEGACY_TEXT) {
        for from in [Charset::Unicode, Charset::Tcvn3] {
            for to in [Charset::Unicode, Charset::Tcvn3] {
                let _ = convert(&text, from, to);
            }
        }
    }

    /// The contract `unmapped` states, checked rather than asserted in prose: when
    /// no character was approximated at either end, the conversion is exactly
    /// reversible. A table entry carrying two letters would break this.
    ///
    /// Both counts have to be zero, and the first one is the interesting half —
    /// arbitrary bytes are mostly *not* TCVN3, and an unrecognized byte re-encodes
    /// to whatever its Latin-1 letter is worth, not to itself.
    #[test]
    fn an_exact_conversion_is_reversible(text in LEGACY_TEXT) {
        let unicode = convert(&text, Charset::Tcvn3, Charset::Unicode);
        let back = convert(&unicode.text, Charset::Unicode, Charset::Tcvn3);
        if unicode.unmapped == 0 && back.unmapped == 0 {
            prop_assert_eq!(&back.text, &text);
        }
    }

    /// TCVN3 spells every letter in one code, so a conversion to or from it moves
    /// no character boundaries. (This is a property of TCVN3, not of the module:
    /// VNI-Windows spells some letters in two, and will not have it.)
    #[test]
    fn tcvn3_conversion_preserves_the_character_count(text in LEGACY_TEXT) {
        let decoded = convert(&text, Charset::Tcvn3, Charset::Unicode);
        prop_assert_eq!(decoded.text.chars().count(), text.chars().count());

        let encoded = convert(&text, Charset::Unicode, Charset::Tcvn3);
        prop_assert_eq!(encoded.text.chars().count(), text.chars().count());
    }

    /// Nothing is ever dropped: `unmapped` counts characters that need attention,
    /// never characters that went missing.
    #[test]
    fn no_character_is_ever_dropped(text in "\\PC{0,64}") {
        let out = convert(&text, Charset::Unicode, Charset::Tcvn3);
        prop_assert_eq!(out.text.chars().count(), text.chars().count());
        prop_assert!(out.unmapped <= text.chars().count());
    }
}
