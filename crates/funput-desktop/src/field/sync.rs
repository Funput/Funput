//! Keeping [`FieldComposer`]'s model of the field true: rebuilt from the field
//! whenever it shows something the composer did not make.

use super::{FieldComposer, FieldEdit, FieldView};
use crate::retone::{char_before, word_start};

impl FieldComposer {
    /// Rebuild the model from `field` unless it shows exactly what the composer
    /// last returned. A selection is deleted here: whatever comes next replaces it.
    pub(super) fn sync(&mut self, field: FieldView<'_>) {
        if self.shows(field) {
            return;
        }
        let (start, end) = field.selection();
        self.before.clear();
        self.before.push_str(&field.text[..start]);
        self.after.clear();
        self.after.push_str(&field.text[end..]);
        self.engine.clear();
        self.synced = true;
        self.reopen_word();
    }

    /// Whether `field` is the text and caret of the last [`FieldEdit`].
    fn shows(&self, field: FieldView<'_>) -> bool {
        let buffer = self.engine.buffer();
        let caret = self.before.len() + buffer.len();
        self.synced
            && field.cursor == caret
            && field.anchor == caret
            && field.text.len() == caret + self.after.len()
            && field.text.starts_with(&self.before)
            && field.text[self.before.len()..].starts_with(buffer)
            && field.text.ends_with(&self.after)
    }

    /// Hand the word ending at the caret back to the engine, so a tone key typed
    /// after a click into `cơm` gives `cờm` the way typing in any app does. A word
    /// the engine will not take (`hello`) stays plain text.
    pub(super) fn reopen_word(&mut self) {
        let start = word_start(&self.before);
        let before = char_before(&self.before, start);
        if start < self.before.len() && self.engine.adopt_after(&self.before[start..], before) {
            self.before.truncate(start);
        }
    }

    /// The field as the model now describes it, caret after the word being composed.
    pub(super) fn edit(&self) -> FieldEdit {
        let buffer = self.engine.buffer();
        FieldEdit {
            text: [self.before.as_str(), buffer, self.after.as_str()].concat(),
            cursor: self.before.len() + buffer.len(),
        }
    }
}
