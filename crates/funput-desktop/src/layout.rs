//! Which keyboard layouts Vietnamese can be typed on.
//!
//! A hook shell types *over* the focused app: it swallows a keystroke and sends
//! back a replacement. That is fine on a plain Latin layout and ruinous on one
//! that is already transforming the keystroke itself — a CJK IME is mid-conversion
//! when Funput's Backspaces arrive, and a Cyrillic or Thai layout never produces
//! the letters Telex/VNI are written in to begin with.
//!
//! So a layout is *foreign* when Vietnamese cannot be typed on it, and the shell
//! suspends itself for as long as one is focused (see `ShellState::apply_for_layout`).
//!
//! The input here is a Windows `HKL` as a plain `u32`, which keeps this file free
//! of OS imports and testable against real handles. Its layout:
//!
//! ```text
//! 0xE0200411   high word = device / IME id     low word = LANGID
//!   ^^          0xE… marks a TSF text service (an IME)
//! ```
//!
//! Nothing here is platform-specific beyond that encoding, and nothing is stored:
//! the answer is recomputed from the handle each time it changes.

/// Primary language ids (`LANGID & 0x3FF`) written in a script Telex and VNI have
/// no letters in. Sorted by id. Latin-script languages are deliberately absent —
/// a French or Turkish layout still types `a`-`z`, so Vietnamese still works on it.
///
/// CJK is here as well as behind the IME check in [`is_foreign_layout`]: the two
/// catch it at different moments — the handle says "IME" only while one is loaded,
/// while the language holds for the bare keyboard layout too.
const NON_LATIN_LANGUAGES: &[u16] = &[
    0x01, // Arabic
    0x02, // Bulgarian
    0x04, // Chinese
    0x08, // Greek
    0x0D, // Hebrew
    0x11, // Japanese
    0x12, // Korean
    0x19, // Russian
    0x1E, // Thai
    0x20, // Urdu
    0x22, // Ukrainian
    0x23, // Belarusian
    0x28, // Tajik
    0x29, // Persian
    0x2B, // Armenian
    0x37, // Georgian
    0x39, // Hindi
    0x40, // Kyrgyz
    0x44, // Tatar
    0x45, // Bengali
    0x46, // Punjabi
    0x47, // Gujarati
    0x49, // Tamil
    0x4A, // Telugu
    0x4B, // Kannada
    0x4C, // Malayalam
    0x4E, // Marathi
    0x50, // Mongolian
    0x51, // Tibetan
    0x53, // Khmer
    0x54, // Lao
    0x5E, // Amharic
    0x61, // Nepali
    0x65, // Divehi
    0x80, // Uyghur
];

/// Vietnamese itself, which is never foreign however it is reached.
const LANG_VIETNAMESE: u16 = 0x2A;

/// Whether `hkl` is a layout Funput must keep its hands off.
///
/// A zero handle means the foreground window could not be resolved. That reads as
/// "not foreign" — leaving the user's own VI/EN choice standing is the only safe
/// answer when we cannot tell what they are typing on.
pub fn is_foreign_layout(hkl: u32) -> bool {
    if hkl == 0 {
        return false;
    }
    let primary = (hkl & 0x3FF) as u16;
    if primary == LANG_VIETNAMESE {
        return false;
    }
    // A TSF text service — Microsoft IME for Japanese, Korean, or either Chinese.
    // The nibble is the documented marker, and it survives the IME being switched
    // to half-width alphanumeric, which is exactly when a keystroke still belongs
    // to the IME's composition even though it looks like plain ASCII.
    if hkl >> 28 == 0xE {
        return true;
    }
    NON_LATIN_LANGUAGES.contains(&primary)
}

#[cfg(test)]
mod tests {
    use super::is_foreign_layout;

    #[test]
    fn a_latin_layout_is_where_vietnamese_is_typed() {
        assert!(!is_foreign_layout(0x0409_0409)); // en-US
        assert!(!is_foreign_layout(0x040C_040C)); // fr-FR
        assert!(!is_foreign_layout(0x041F_041F)); // tr-TR
    }

    /// Alternative layouts put their own id in the high word. Only the language
    /// half decides, so Dvorak and the US-International layout stay usable.
    #[test]
    fn an_alternative_latin_layout_is_still_latin() {
        assert!(!is_foreign_layout(0xF001_0409)); // US-Dvorak
        assert!(!is_foreign_layout(0x0001_0409)); // US-International
    }

    #[test]
    fn the_vietnamese_layout_is_never_foreign() {
        assert!(!is_foreign_layout(0x042A_042A));
    }

    #[test]
    fn a_cjk_ime_is_foreign() {
        assert!(is_foreign_layout(0xE020_0411)); // Microsoft IME (Japanese)
        assert!(is_foreign_layout(0xE020_0412)); // Microsoft IME (Korean)
        assert!(is_foreign_layout(0xE001_0804)); // Microsoft Pinyin
        assert!(is_foreign_layout(0xE012_0404)); // Microsoft Bopomofo
    }

    /// The plain `ja-JP` keyboard handle, which is what a Japanese *layout* without
    /// its IME loaded reports. Caught by the language list, not the IME nibble.
    #[test]
    fn a_non_latin_keyboard_is_foreign_without_an_ime() {
        assert!(is_foreign_layout(0x0411_0411)); // Japanese
        assert!(is_foreign_layout(0x0419_0419)); // Russian
        assert!(is_foreign_layout(0x041E_041E)); // Thai
        assert!(is_foreign_layout(0x0408_0408)); // Greek
    }

    #[test]
    fn an_unresolved_window_changes_nothing() {
        assert!(!is_foreign_layout(0));
    }
}
