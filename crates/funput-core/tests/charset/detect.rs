//! Detection, through the public API only.

use funput_core::charset::{Charset, convert, detect};

use crate::cases::DETECT_PHRASES;

const CANDIDATES: [Charset; 4] = [
    Charset::Unicode,
    Charset::Tcvn3,
    Charset::VniWindows,
    Charset::UnicodeCombining,
];

/// Whether a phrase carries a tone anywhere.
///
/// Asked through the module's own signal rather than by inspecting characters:
/// Unicode tổ hợp reports a rewrite once per toned letter, so a phrase with no
/// tones converts to it unchanged and with nothing to report.
fn has_a_tone(phrase: &str) -> bool {
    convert(phrase, Charset::Unicode, Charset::UnicodeCombining).normalized > 0
}

/// The real use case, stated as a test: take Vietnamese text, write it the way a
/// charset writes it, and the detector has to name that charset again. No spelling
/// is hand-written anywhere — the fixtures are Unicode and the test does the rest.
#[test]
fn text_written_in_a_charset_is_detected_as_that_charset() {
    for phrase in DETECT_PHRASES {
        for charset in CANDIDATES {
            let encoded = convert(phrase, Charset::Unicode, charset);
            if encoded.unmapped > 0 {
                continue; // the charset cannot spell this phrase; nothing to detect
            }
            if charset == Charset::UnicodeCombining && !has_a_tone(phrase) {
                // With no tone to separate, this is byte-for-byte the Unicode
                // spelling. A tie is the honest answer — see the test below.
                continue;
            }
            assert_eq!(
                detect(&encoded.text),
                Some(charset),
                "{phrase:?} written as {charset:?}"
            );
        }
    }
}

/// The two Unicode charsets spell toneless text identically, so nothing can tell
/// them apart — and nothing needs to, since either answer converts correctly.
#[test]
fn toneless_text_does_not_separate_the_two_unicode_charsets() {
    // Shaped vowels but no tones: `Đ` and `ô` stay precomposed in both charsets.
    let toneless = "Đông Nam kinh doanh";
    assert!(!has_a_tone(toneless), "no tone marks in this phrase");
    assert_eq!(
        convert(toneless, Charset::Unicode, Charset::UnicodeCombining).text,
        toneless
    );
}

/// Every phrase in every charset, and detection never claims a charset that cannot
/// actually read the text back.
#[test]
fn a_detected_charset_reads_the_text_without_loss() {
    for phrase in DETECT_PHRASES {
        for charset in CANDIDATES {
            let encoded = convert(phrase, Charset::Unicode, charset);
            if encoded.unmapped > 0 {
                continue;
            }
            if let Some(guess) = detect(&encoded.text) {
                let read = convert(&encoded.text, guess, Charset::Unicode);
                assert_eq!(
                    read.unmapped, 0,
                    "{phrase:?} as {charset:?}, read as {guess:?}"
                );
            }
        }
    }
}
