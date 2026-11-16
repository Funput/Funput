//! Vietnamese composition inside one of Funput's own text fields.
//!
//! The global keyboard hook cannot compose into Funput's own windows (Windows
//! Settings is skipped on purpose, and winit ignores synthetic input), so such a
//! field feeds each real keystroke through a [`FieldComposer`] driving its own
//! `funput-engine`, and shows the text that comes back.
//!
//! Unlike the hook, the composer can see the whole field, so it does not have to
//! assume the caret sits at the end. It models the text as `before` + the word being
//! composed + `after`, and checks the field against that model before every key: a
//! click, a drag, a caret key or an undo shows up as text or offsets it did not
//! expect, and the model is rebuilt from what the field really holds.

mod sync;
mod view;

use funput_config::Settings;
use funput_core::InputMethod;
use funput_engine::Engine;

pub use view::FieldView;
use view::is_text;

/// The field's new text, and where its caret goes, as a UTF-8 byte offset.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FieldEdit {
    pub text: String,
    pub cursor: usize,
}

/// One field's composition. Shared by every field of a window: [`Self::reset`] on
/// focus-in, and the model is rebuilt from the field on the first key.
pub struct FieldComposer {
    engine: Engine,
    method: InputMethod,
    /// Text before the word being composed, which the engine holds in its buffer.
    before: String,
    /// Text after the caret, untouched by typing.
    after: String,
    /// Whether `before`, the buffer and `after` still describe the field.
    synced: bool,
}

impl FieldComposer {
    pub fn new() -> Self {
        let mut engine = Engine::new();
        // A config field keeps what the user composes (no English auto-restore).
        engine.update_config(|c| c.smart_restore = false);
        Self {
            engine,
            method: InputMethod::Telex,
            before: String::new(),
            after: String::new(),
            synced: false,
        }
    }

    /// Start over on focus-in, applying the user's spelling choices (method, tone
    /// placement, extra onsets) so the field composes the way global typing does.
    /// The next key rebuilds the model from the field.
    pub fn reset(&mut self, settings: &Settings) {
        self.method = settings.method.core();
        self.engine.set_method(self.method);
        self.engine.update_config(|c| {
            c.tone_style = settings.tone_style.core();
            c.syllable_rules = c
                .syllable_rules
                .with_extra_onsets(settings.extra_onsets.core());
        });
        self.engine.clear();
        self.synced = false;
    }

    /// Type `c` at the caret, over the selection if there is one. `None` when `c`
    /// is no text (a modifier, an F-key): the field should handle that key itself.
    pub fn key(&mut self, field: FieldView<'_>, c: char) -> Option<FieldEdit> {
        if !is_text(c) {
            return None;
        }
        self.sync(field);
        if c.is_whitespace() || (c.is_ascii_punctuation() && !self.is_advanced_shortcut(c)) {
            // A word boundary: the composed word and the separator become plain text.
            self.before.push_str(self.engine.buffer());
            self.before.push(c);
            self.engine.clear();
        } else {
            self.engine.process_char(c);
            if self.engine.buffer().is_empty() {
                // The engine passed the key through (a digit starting a word is a
                // number, not Vietnamese), so it is plain text right away.
                self.before.push(c);
            }
        }
        Some(self.edit())
    }

    /// Backspace: delete the selection, or else the character before the caret.
    pub fn backspace(&mut self, field: FieldView<'_>) -> FieldEdit {
        let deletes_selection = field.has_selection();
        self.sync(field);
        // With a selection, rebuilding the model already deleted it.
        if !deletes_selection {
            if !self.engine.buffer().is_empty() {
                self.engine.on_backspace();
            } else if self.before.pop().is_some() {
                self.reopen_word();
            }
        }
        self.edit()
    }

    fn is_advanced_shortcut(&self, c: char) -> bool {
        self.method == InputMethod::TelexAdvanced && matches!(c, '[' | ']')
    }
}

impl Default for FieldComposer {
    fn default() -> Self {
        Self::new()
    }
}
