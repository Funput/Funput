//! The conversion driver, and the window a codec reads the source through.
//!
//! The loop is deliberately dull: decode one unit, encode it, advance. All the
//! charset knowledge sits in [`crate::charset::codecs`], and all the letter
//! knowledge in [`crate::charset::pivot`], so this file stays the same shape no
//! matter how many charsets exist.

use super::codecs;
use super::pivot::Atom;
use super::{Charset, Conversion};

/// A read window onto the source, positioned at a unit boundary.
///
/// A codec reads as far ahead as its encoding needs and reports how much it took.
/// TCVN3 only ever needs the first char; VNI's base-plus-mark pair will want a
/// second, and adds the accessor for it then.
pub(super) struct Cursor<'a> {
    rest: &'a str,
}

impl<'a> Cursor<'a> {
    /// The char at the cursor. Never fails: a cursor is only built on a non-empty
    /// remainder.
    pub(super) fn first(&self) -> char {
        self.rest
            .chars()
            .next()
            .expect("cursor built on non-empty text")
    }
}

/// What a codec made of the text at the cursor.
pub(super) struct Decoded {
    pub(super) atom: Atom,
    /// How many chars this reading consumed. Must be at least 1, or the driver
    /// would not advance.
    pub(super) consumed: usize,
    /// False when the source charset does not define this unit at all. The atom is
    /// still usable — it carries the character through untouched — but the reading
    /// is a guess, and the caller deserves to know.
    pub(super) recognized: bool,
}

/// Convert `text` from one charset to another, counting every character that did
/// not survive exactly.
///
/// A character is counted once, whichever end failed it: the source charset does
/// not define it, or the target cannot spell it. Both are things the user has to
/// look at, and the commonest cause of the first one is picking the wrong source
/// charset — which makes a high count the signal that they did.
pub(super) fn transcode(text: &str, from: Charset, to: Charset) -> Conversion {
    let mut out = String::with_capacity(text.len());
    let mut unmapped = 0;
    let mut rest = text;

    while !rest.is_empty() {
        let decoded = codecs::decode(from, &Cursor { rest });
        let written_exactly = codecs::encode(to, decoded.atom, &mut out);
        if !decoded.recognized || !written_exactly {
            unmapped += 1;
        }
        rest = advance(rest, decoded.consumed);
    }

    Conversion {
        text: out,
        unmapped,
    }
}

/// Step `count` chars forward, clamping at the end. A codec that reports more than
/// it could see leaves the loop finishing early rather than panicking.
fn advance(rest: &str, count: usize) -> &str {
    match rest.char_indices().nth(count.max(1)) {
        Some((offset, _)) => &rest[offset..],
        None => "",
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn advance_steps_over_whole_chars() {
        assert_eq!(advance("phủ", 1), "hủ");
        assert_eq!(advance("phủ", 2), "ủ"); // multi-byte char, single step
        assert_eq!(advance("phủ", 3), "");
        assert_eq!(
            advance("phủ", 99),
            "",
            "over-reporting clamps, never panics"
        );
    }

    /// A codec reporting zero would spin the driver forever; the floor is what
    /// makes the loop's termination a property of this file rather than of every
    /// codec that will ever be written.
    #[test]
    fn advance_never_stands_still() {
        assert_eq!(advance("ab", 0), "b");
    }

    #[test]
    fn unicode_to_unicode_is_an_exact_identity() {
        let result = transcode("Chào bạn — 42₫", Charset::Unicode, Charset::Unicode);
        assert_eq!(result.text, "Chào bạn — 42₫");
        assert_eq!(result.unmapped, 0);
    }

    #[test]
    fn empty_text_converts_to_empty_text() {
        let result = transcode("", Charset::Unicode, Charset::Tcvn3);
        assert!(result.text.is_empty());
        assert_eq!(result.unmapped, 0);
    }
}
