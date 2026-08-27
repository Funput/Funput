//! The conversion driver, and the window a codec reads the source through.
//!
//! The loop is deliberately dull: decode one unit, encode it, advance. All the
//! charset knowledge sits in [`crate::charset::codecs`], and all the letter
//! knowledge in [`crate::charset::pivot`], so this file stays the same shape no
//! matter how many charsets exist.

use super::Charset;
use super::codecs;
use super::pivot::Atom;

/// A read window onto the source, positioned at a unit boundary.
///
/// A codec reads as far ahead as its spelling needs and reports how much it took.
/// TCVN3 spells every letter in one char; VNI-Windows takes a base and a mark;
/// Unicode tổ hợp can take a base and two combining marks.
pub(super) struct Cursor<'a> {
    rest: &'a str,
}

impl Cursor<'_> {
    /// The char at the cursor. Never fails: a cursor is only built on a non-empty
    /// remainder.
    pub(super) fn first(&self) -> char {
        self.rest
            .chars()
            .next()
            .expect("cursor built on non-empty text")
    }

    /// Everything after [`Cursor::first`].
    ///
    /// An iterator rather than an index, because "take up to two more" is what the
    /// codecs actually do — and because the moment lookahead is numbered, one
    /// reader counts from the cursor and the next counts from the char after it.
    /// Running out is `None`, never a sentinel: a codec that folds "no more text"
    /// into some zero value will match a truncated tail against a longer spelling
    /// and swallow whatever follows.
    pub(super) fn following(&self) -> std::str::Chars<'_> {
        let mut chars = self.rest.chars();
        chars.next();
        chars
    }

    /// The char after the cursor, when there is one.
    pub(super) fn second(&self) -> Option<char> {
        self.following().next()
    }
}

/// Converted text, and what happened to it on the way.
///
/// Both counts are in characters, and neither counts *lost* text: conversion
/// always writes something for every character, so the output holds as much as the
/// input did. A character lands in at most one of them.
///
/// **`unmapped`** — something was lost or guessed, so the output is not certainly
/// equivalent to the input. Either the target cannot spell it (a `₫`, which no
/// legacy charset has a code for; an uppercase toned vowel, which is TCVN3's own
/// gap rather than a general one) or the source never defined it, making the
/// reading a guess. Text read with the wrong source charset is mostly unrecognized
/// units, so a count near the length of the input is the clearest signal there is
/// that the user picked the wrong bảng mã.
///
/// **`normalized`** — understood beyond doubt, nothing lost, only the spelling
/// changed. Reading NFC text as Unicode tổ hợp lands here: every letter comes
/// through intact, just written the charset's own way. Keeping it out of
/// `unmapped` is what lets an interface say "nothing was lost" and mean it.
#[derive(Debug, Clone, PartialEq, Eq)]
#[non_exhaustive]
pub struct Conversion {
    pub text: String,
    pub unmapped: usize,
    pub normalized: usize,
}

/// How faithfully a codec managed to read the text at the cursor.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(super) enum Reading {
    /// The source spelled this the way the charset spells it.
    Exact,
    /// Understood beyond doubt, but written another way — re-encoding will use the
    /// charset's own spelling instead. Nothing is lost; only the spelling changes.
    Rewritten,
    /// The charset does not define this unit. Carrying the character through is a
    /// guess, and the caller deserves to know it was one.
    Unknown,
}

/// What a codec made of the text at the cursor.
pub(super) struct Decoded {
    pub(super) atom: Atom,
    /// How many chars this reading consumed. Must be at least 1, or the driver
    /// would not advance.
    pub(super) consumed: usize,
    pub(super) reading: Reading,
}

/// Convert `text` from one charset to another, counting what happened to each
/// character along the way.
///
/// A character lands in at most one bucket, and loss outranks rewriting: if the
/// target could not spell it exactly, that is what the user needs to see, however
/// the source spelled it.
pub(super) fn transcode(text: &str, from: Charset, to: Charset) -> Conversion {
    let mut out = String::with_capacity(text.len());
    let mut unmapped = 0;
    let mut normalized = 0;
    let mut rest = text;

    while !rest.is_empty() {
        let decoded = codecs::decode(from, &Cursor { rest });
        let written_exactly = codecs::encode(to, decoded.atom, &mut out);
        match (decoded.reading, written_exactly) {
            (_, false) | (Reading::Unknown, _) => unmapped += 1,
            (Reading::Rewritten, true) => normalized += 1,
            (Reading::Exact, true) => {}
        }
        rest = advance(rest, decoded.consumed);
    }

    Conversion {
        text: out,
        unmapped,
        normalized,
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
    fn following_stops_at_the_end_rather_than_repeating() {
        let cursor = Cursor { rest: "ab" };
        assert_eq!(cursor.following().collect::<String>(), "b");
        assert_eq!(Cursor { rest: "a" }.following().next(), None);
    }

    #[test]
    fn unicode_to_unicode_is_an_exact_identity() {
        let result = transcode("Chào bạn — 42₫", Charset::Unicode, Charset::Unicode);
        assert_eq!(result.text, "Chào bạn — 42₫");
        assert_eq!(result.unmapped, 0);
        assert_eq!(result.normalized, 0);
    }

    #[test]
    fn empty_text_converts_to_empty_text() {
        let result = transcode("", Charset::Unicode, Charset::Tcvn3);
        assert!(result.text.is_empty());
        assert_eq!(result.unmapped, 0);
    }
}
