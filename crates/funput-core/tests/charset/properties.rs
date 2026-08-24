//! Properties that must hold for text no fixture would think to write down —
//! arbitrary byte soup, half-words, unassigned codes, marks with nothing to
//! attach to.

use funput_core::charset::{Charset, convert, detect, detect_bytes};
use proptest::prelude::*;

/// Anything a legacy document could hold: one char per byte, the whole range.
const LEGACY_TEXT: &str = "[\\x{20}-\\x{FF}]{0,64}";

/// Bases, shaped vowels, and the eight combining marks. A separate generator is
/// needed because every mark sits above `U+00FF`, so `LEGACY_TEXT` cannot produce
/// one and any property run over it would be checking nothing.
const COMBINING_TEXT: &str =
    "[a-zA-Zâăêôơưđ \\x{300}\\x{301}\\x{303}\\x{309}\\x{323}\\x{302}\\x{306}\\x{31B}]{0,48}";

/// The byte-oriented charsets — the ones whose output a file can hold one byte at
/// a time.
const LEGACY: [Charset; 2] = [Charset::Tcvn3, Charset::VniWindows];

/// Every charset that is not the pivot. Wider than [`LEGACY`]: Unicode tổ hợp is
/// a real charset with its own spelling, it is just not made of bytes.
const NON_PIVOT: [Charset; 3] = [
    Charset::Tcvn3,
    Charset::VniWindows,
    Charset::UnicodeCombining,
];

/// Every charset. Unlike the in-crate `CANDIDATES` array this one **cannot** be
/// guarded by a match: `Charset` is `#[non_exhaustive]`, so code outside the crate
/// is required to write a wildcard arm and a fifth variant would slip past it.
/// The compile-time guard lives in `charset::detect` instead.
const ALL: [Charset; 4] = [
    Charset::Unicode,
    Charset::Tcvn3,
    Charset::VniWindows,
    Charset::UnicodeCombining,
];

proptest! {
    /// Conversion is total. Whatever the text is — and a user who picks the wrong
    /// source charset feeds it text that is *not* what it claims — it comes back
    /// as text, not as a panic.
    #[test]
    fn conversion_never_panics(text in LEGACY_TEXT, combining in COMBINING_TEXT) {
        for source in [&text, &combining] {
            for from in ALL {
                for to in ALL {
                    let _ = convert(source, from, to);
                }
            }
        }
    }

    /// The contract `unmapped` states, checked rather than asserted in prose: when
    /// no character was approximated at either end, the conversion is exactly
    /// reversible. A table entry carrying two letters would break this.
    ///
    /// It is also the regression net for VNI's lenient mixed-case reading. A
    /// decoder that took `61 D9` as `á` with `Reading::Exact` would re-encode it
    /// as `61 F9` and fail here; counting it is what keeps the guard honest.
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

    /// The same contract for Unicode tổ hợp, where `normalized` carries the weight:
    /// a source spelled the charset's own way must come back byte for byte.
    ///
    /// This is the regression net for "a full slot ends the letter". A decoder that
    /// let a second tone mark overwrite the first would read `a` `U+0301` `U+0300`
    /// as `à` with nothing reported, and re-encoding gives `a` `U+0300` — one
    /// character shorter than it started.
    #[test]
    fn an_exact_combining_conversion_is_reversible(text in COMBINING_TEXT) {
        let unicode = convert(&text, Charset::UnicodeCombining, Charset::Unicode);
        let back = convert(&unicode.text, Charset::Unicode, Charset::UnicodeCombining);
        if unicode.unmapped == 0 && unicode.normalized == 0 && back.unmapped == 0 {
            prop_assert_eq!(&back.text, &text);
        }
    }

    /// TCVN3 spells every letter in one code, so a conversion to or from it moves
    /// no character boundaries. This is a property of TCVN3, not of the module —
    /// VNI-Windows spells most letters in two, and Unicode tổ hợp spells a toned
    /// one in two as well. Their codec tests state the opposite for them.
    #[test]
    fn tcvn3_conversion_preserves_the_character_count(text in LEGACY_TEXT) {
        let decoded = convert(&text, Charset::Tcvn3, Charset::Unicode);
        prop_assert_eq!(decoded.text.chars().count(), text.chars().count());

        let encoded = convert(&text, Charset::Unicode, Charset::Tcvn3);
        prop_assert_eq!(encoded.text.chars().count(), text.chars().count());
    }

    /// Nothing is ever dropped. `>=` rather than `==` is the honest statement of
    /// that, and the one the module actually promises: encoding can make text
    /// longer, and only TCVN3 keeps the count.
    #[test]
    fn no_character_is_ever_dropped(text in "\\PC{0,64}") {
        for charset in NON_PIVOT {
            let out = convert(&text, Charset::Unicode, charset);
            prop_assert!(out.text.chars().count() >= text.chars().count());
            prop_assert!(out.unmapped + out.normalized <= text.chars().count());
        }
    }

    /// Decoding can only ever join characters, never invent them: two may become
    /// one letter, but one never becomes two. A decoder that mistook the end of the
    /// text for a zero byte, or that emitted a letter *and* left its mark behind,
    /// would break this.
    #[test]
    fn decoding_never_invents_characters(text in LEGACY_TEXT, combining in COMBINING_TEXT) {
        for (charset, source) in [
            (Charset::Tcvn3, &text),
            (Charset::VniWindows, &text),
            (Charset::UnicodeCombining, &combining),
        ] {
            let out = convert(source, charset, Charset::Unicode);
            prop_assert!(
                out.text.chars().count() <= source.chars().count(),
                "{:?} grew {:?} into {:?}", charset, source, out.text
            );
        }
    }

    /// A legacy document has to be storable as bytes. Any character in the output
    /// above `U+00FF` could not have come from the charset, so it must have been
    /// counted — otherwise `decode_bytes` would hand back text a file cannot hold.
    ///
    /// Deliberately over [`LEGACY`] and not [`NON_PIVOT`]: Unicode tổ hợp is not
    /// byte-oriented and writes `ơ` (`U+01A1`) with nothing to report, so it fails
    /// this and rightly so. Please do not "fix" the inconsistency.
    #[test]
    fn a_legacy_charset_writes_only_bytes_or_reports_it(text in "\\PC{0,64}") {
        for charset in LEGACY {
            let out = convert(&text, Charset::Unicode, charset);
            let wide = out.text.chars().filter(|c| *c > '\u{FF}').count();
            prop_assert!(wide <= out.unmapped, "{:?} wrote {} unreported wide chars", charset, wide);
        }
    }

    /// The strict half of "lenient in, strict out": whatever Unicode tổ hợp writes
    /// is in its own spelling, so reading it back reports no rewriting. Nothing in
    /// the fixed cases proves this over arbitrary text.
    #[test]
    fn combining_output_is_always_canonical(text in "\\PC{0,64}") {
        let out = convert(&text, Charset::Unicode, Charset::UnicodeCombining);
        if out.unmapped == 0 {
            let reread = convert(&out.text, Charset::UnicodeCombining, Charset::UnicodeCombining);
            prop_assert_eq!(reread.normalized, 0, "wrote a spelling it would rewrite");
        }
    }

    /// Detection is total: whatever the text is, it answers rather than panicking.
    #[test]
    fn detect_never_panics(text in LEGACY_TEXT, combining in COMBINING_TEXT, wide in "\\PC{0,64}") {
        for source in [&text, &combining, &wide] {
            let _ = detect(source);
        }
    }

    /// Same for the byte door, on bytes that were never text at all.
    #[test]
    fn detect_bytes_never_panics(bytes in prop::collection::vec(any::<u8>(), 0..256)) {
        let _ = detect_bytes(&bytes);
    }

    /// Every charset spells ASCII the same way, so ASCII can never separate them.
    /// This is what makes an English document — or a plain one — come back `None`,
    /// and it locks that guarantee against any future change to the scoring.
    #[test]
    fn no_charset_is_detected_from_pure_ascii(text in "[ -~]{0,64}") {
        prop_assert_eq!(detect(&text), None, "{:?}", text);
    }
}
