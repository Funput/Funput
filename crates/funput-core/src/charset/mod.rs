//! Converting Vietnamese text between Unicode and the legacy encodings.
//!
//! Vietnamese government documents still circulate in TCVN3, drawn by the
//! `.VnTime` fonts. This turns them into Unicode, and back.
//!
//! # Text in, text out
//!
//! [`convert`] takes a `&str`, not bytes, and that is not a convenience — it is
//! what the input actually is. Word stores a TCVN3 document as Unicode code points
//! `U+0020..=U+00FF` and leaves the *font* to draw the Vietnamese glyphs, so text
//! arriving from the clipboard is already "bytes as chars". Converting it means
//! reading those code points back as TCVN3 codes. [`decode_bytes`] is the other
//! door, for text that really is bytes because it came from a file.
//!
//! # How it is put together
//!
//! Everything pivots through precomposed Unicode: `source → Atom → target`. A
//! charset therefore never has to know about any other charset, and adding one
//! costs two mappings rather than one per charset already present. See
//! [`codecs`](self::codecs) for what a new charset owes, and
//! `docs/features/charset.md` for the design in full.
//!
//! ```
//! use funput_core::charset::{Charset, convert};
//!
//! let out = convert("Việt Nam", Charset::Unicode, Charset::Tcvn3);
//! assert_eq!(convert(&out.text, Charset::Tcvn3, Charset::Unicode).text, "Việt Nam");
//! assert_eq!(out.unmapped, 0);
//! ```

mod codecs;
mod detect;
mod pivot;
mod transcode;

pub use codecs::{decode_bytes, is_byte_oriented};
pub use detect::{detect, detect_bytes};
// `Conversion` is defined beside the loop that fills in its two counters.
pub use transcode::Conversion;

/// A Vietnamese character encoding.
///
/// `#[non_exhaustive]`: VIQR, VISCII and the rest are out of scope today but not
/// forever, and adding one should not break callers. Match with a wildcard arm.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[non_exhaustive]
pub enum Charset {
    /// Precomposed Unicode (NFC) — the pivot, and what every modern system uses.
    Unicode,
    /// TCVN3, also called ABC: the `.VnTime` encoding of Vietnamese government
    /// documents. One byte per letter, and no code for an uppercase toned vowel.
    Tcvn3,
    /// VNI-Windows, the `VNI-Times` encoding. Spells most letters as a base byte
    /// plus a mark byte, so a letter is not a character and conversion moves
    /// character boundaries. Named for the encoding rather than `Vni`, which would
    /// sit confusingly beside [`crate::InputMethod::Vni`] — a way of *typing*, not
    /// a way of storing.
    VniWindows,
    /// Unicode tổ hợp, UniKey's convention: the shaped vowel stays precomposed and
    /// only the **tone** rides as a combining mark, so `ậ` is `â` plus `U+0323`.
    ///
    /// Deliberately not called `Decomposed`: this is not NFD, which would take the
    /// shape apart too. Reading accepts NFD anyway — see the codec — but writing
    /// only ever produces this form.
    UnicodeCombining,
}

impl Charset {
    /// What to call this charset in front of a user.
    ///
    /// The encoding's own name rather than interface copy — `TCVN3 (ABC)` reads the
    /// same in any language, and these are the names UniKey uses, which is what
    /// Vietnamese users already know them by. Keeping them here is what stops a menu
    /// on Windows and one on Linux from drifting apart.
    pub const fn name(self) -> &'static str {
        match self {
            Self::Unicode => "Unicode dựng sẵn",
            Self::Tcvn3 => "TCVN3 (ABC)",
            Self::VniWindows => "VNI-Windows",
            Self::UnicodeCombining => "Unicode tổ hợp",
        }
    }
}

/// Every charset, in the order an interface should offer them.
///
/// A caller cannot build this list for itself: `Charset` is `#[non_exhaustive]`, so
/// code outside this crate must write a wildcard arm and would silently miss a
/// variant added later. [`detect`] scores exactly this list, so a charset a user can
/// choose is one the tool can also recognise.
pub const ALL: [Charset; 4] = [
    Charset::Unicode,
    Charset::Tcvn3,
    Charset::VniWindows,
    Charset::UnicodeCombining,
];

/// Adding a variant must break the build here rather than drop it out of every menu.
///
/// [`Charset::name`] already forces a name for each one; this catches the other
/// half — a variant that has a name but never made it into [`ALL`].
const _: () = {
    const fn slot(charset: Charset) -> u32 {
        match charset {
            Charset::Unicode => 0,
            Charset::Tcvn3 => 1,
            Charset::VniWindows => 2,
            Charset::UnicodeCombining => 3,
        }
    }
    let (mut listed, mut i) = (0u32, 0);
    while i < ALL.len() {
        listed |= 1 << slot(ALL[i]);
        i += 1;
    }
    assert!(
        listed == (1 << ALL.len()) - 1,
        "a charset is missing from ALL"
    );
};

/// Convert text from one charset to another.
///
/// `from` must be what the text actually is; guessing it is [`detect`]'s job, and
/// not one this function does.
pub fn convert(text: &str, from: Charset, to: Charset) -> Conversion {
    transcode::transcode(text, from, to)
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Two menus showing the same name for different charsets, or a blank entry,
    /// are the failures a shared list is supposed to make impossible.
    #[test]
    fn every_charset_has_a_distinct_name() {
        let mut names: Vec<&str> = ALL.iter().map(|c| c.name()).collect();
        assert!(names.iter().all(|n| !n.is_empty()));

        assert_eq!(names.len(), ALL.len());
        names.sort_unstable();
        names.dedup();
        assert_eq!(names.len(), ALL.len(), "two charsets share a name");
    }

    /// The two Unicode charsets are the pair a user actually has to choose between,
    /// so neither may be called just "Unicode".
    #[test]
    fn the_two_unicode_charsets_are_told_apart_by_name() {
        assert_ne!(Charset::Unicode.name(), Charset::UnicodeCombining.name());
        assert!(Charset::Unicode.name().contains("dựng sẵn"));
        assert!(Charset::UnicodeCombining.name().contains("tổ hợp"));
    }

    /// Anything offered in a menu has to be something the tool can also recognise —
    /// otherwise "Tự động nhận diện" could never return what the user picked.
    #[test]
    fn every_listed_charset_can_be_converted_to_and_from() {
        for charset in ALL {
            let there = convert("Việt Nam", Charset::Unicode, charset);
            assert_eq!(
                convert(&there.text, charset, Charset::Unicode).text,
                "Việt Nam",
                "{charset:?}"
            );
        }
    }
}
