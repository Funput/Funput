//! Where a sentence begins.
//!
//! One scanner, two readers. [`textcase`](crate::textcase) walks a whole paragraph
//! and uppercases each sentence's first letter; a keyboard walks only the text
//! behind the caret and asks [`starts_sentence`] whether to light its Shift key.
//! Both need the same answer to "what ended that sentence?", so the answer lives
//! here rather than once per caller — the same argument that put [`crate::charset`]
//! in this crate instead of in the windows that draw it.
//!
//! A sentence begins at the start of the text, after a newline, and after one of
//! `.` `!` `?` `…` **followed by whitespace**. Two rules fall out of that shape
//! rather than being written down separately: `1.5` keeps its `.` because a digit
//! follows it rather than a space, and a run of `...` or `?!` ends one sentence
//! rather than several. Closing quotes and brackets between the terminator and the
//! space are transparent, so `nói "Xin chào." rồi` starts a sentence at `rồi`.
//!
//! A newline counts even with no punctuation before it, because the text this
//! serves is often a list, a subtitle file, or notes — places where nobody
//! punctuates the ends of lines.
//!
//! **Openers** are skipped when looking for the first letter, so `"xin chào"` and
//! `(xin chào)` both point at their `x`. Digits are not: a sentence that opens with
//! a number has already begun, and skipping past it would turn `3 con mèo` into
//! `3 Con mèo`.
//!
//! **Abbreviations** are guarded only when the caller asks. See [`Rules`].

mod scan;

#[cfg(test)]
mod tests;

pub(crate) use scan::Scanner;

/// Which reading of a full stop the caller wants.
///
/// The two callers disagree on purpose. Sentence case runs over text the user is
/// looking at in a preview, where `v.v. nhé` → `V.v. Nhé` is a visible, explicable
/// miss and a list of Vietnamese abbreviations would be neither complete nor
/// predictable — that decision is written down in `docs/features/text-case.md`.
/// A keyboard has no preview: it commits a capital the moment the user types, so it
/// takes the cheaper guard and accepts that `TS. ` still reads as an ending.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Rules {
    /// Whether `.` directly after a run of letters that itself follows a `.` is read
    /// as an abbreviation rather than an ending.
    ///
    /// Catches `v.v. `, `U.S. `, `a.m. `; misses `TS. `, which has no earlier dot to
    /// give it away. This is the rule Android's own keyboard applies.
    pub guard_abbreviations: bool,
}

impl Rules {
    /// What a keyboard uses: guard abbreviations, because a wrong capital is already
    /// committed by the time the user sees it.
    pub const TYPING: Self = Self {
        guard_abbreviations: true,
    };

    /// What a bulk transform uses: every full stop ends a sentence.
    pub const TRANSFORM: Self = Self {
        guard_abbreviations: false,
    };
}

/// Whether the caret sitting after `before` is at the start of a sentence.
///
/// `before` is the text to the left of the caret; an empty one is the start of the
/// document, which is the start of a sentence.
///
/// # Examples
///
/// ```
/// use funput_core::sentence::{starts_sentence, Rules};
///
/// assert!(starts_sentence("", Rules::TYPING));
/// assert!(starts_sentence("Xin chào. ", Rules::TYPING));
/// // No space yet, so the sentence has not ended.
/// assert!(!starts_sentence("Xin chào.", Rules::TYPING));
/// // An abbreviation, not an ending.
/// assert!(!starts_sentence("giấy tờ v.v. ", Rules::TYPING));
/// ```
pub fn starts_sentence(before: &str, rules: Rules) -> bool {
    let mut scanner = Scanner::new(rules);
    for c in before.chars() {
        scanner.push(c);
    }
    scanner.awaiting_sentence()
}

/// Whether the caret sitting after `before` is at the start of a word.
///
/// Anything that is not a letter or a digit ends a word, so this is true at the
/// start of the document and after a space, a hyphen, or an opening bracket.
///
/// # Examples
///
/// ```
/// use funput_core::sentence::starts_word;
///
/// assert!(starts_word(""));
/// assert!(starts_word("Xin "));
/// assert!(!starts_word("Xin chà"));
/// ```
pub fn starts_word(before: &str) -> bool {
    before
        .chars()
        .next_back()
        .is_none_or(|c| !c.is_alphanumeric())
}
