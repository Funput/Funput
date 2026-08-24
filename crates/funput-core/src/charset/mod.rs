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
pub mod document;
mod identity;
mod pivot;
mod transcode;

pub use codecs::{decode_bytes, encode_bytes, is_byte_oriented};
pub use detect::{detect, detect_bytes};
pub use identity::{ALL, Charset};
// `Conversion` is defined beside the loop that fills in its two counters.
pub use transcode::Conversion;

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
