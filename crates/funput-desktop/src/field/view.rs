//! What a text field reports about itself, and the keys worth composing into it.

/// A text field as its toolkit sees it: the text, and the caret and selection
/// anchor as UTF-8 byte offsets. The two offsets are equal when nothing is selected;
/// the anchor may sit on either side of the caret.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct FieldView<'a> {
    pub text: &'a str,
    pub cursor: usize,
    pub anchor: usize,
}

impl<'a> FieldView<'a> {
    /// A field with the caret at `cursor` and nothing selected.
    pub fn caret(text: &'a str, cursor: usize) -> Self {
        Self {
            text,
            cursor,
            anchor: cursor,
        }
    }

    /// The selected byte range, ordered and snapped onto character boundaries.
    /// Empty when nothing is selected. Offsets past the end clamp to it, and one
    /// that splits a character moves back to that character's start, so slicing
    /// with the result can never panic.
    pub(super) fn selection(&self) -> (usize, usize) {
        let cursor = floor_char_boundary(self.text, self.cursor);
        let anchor = floor_char_boundary(self.text, self.anchor);
        (cursor.min(anchor), cursor.max(anchor))
    }

    pub(super) fn has_selection(&self) -> bool {
        let (start, end) = self.selection();
        start != end
    }
}

fn floor_char_boundary(text: &str, offset: usize) -> usize {
    let mut offset = offset.min(text.len());
    while !text.is_char_boundary(offset) {
        offset -= 1;
    }
    offset
}

/// Whether `c` is real typed text (a letter, digit, space, punctuation, accented or
/// CJK character) rather than a control char or a toolkit special-key codepoint.
/// Slint reports modifier keys, arrows and F-keys in the Unicode Private Use Areas.
pub(super) fn is_text(c: char) -> bool {
    if c.is_control() {
        return false;
    }
    let u = c as u32;
    let private_use = (0xE000..=0xF8FF).contains(&u)
        || (0xF_0000..=0xF_FFFD).contains(&u)
        || (0x10_0000..=0x10_FFFD).contains(&u);
    !private_use
}
