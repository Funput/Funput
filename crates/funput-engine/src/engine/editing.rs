use funput_core::is_reopenable_syllable;

use crate::compose::{diff, flip};
use crate::{Engine, ImeResult};

impl Engine {
    /// Re-open an already-committed word as the live composition, so the next
    /// keystroke edits it — the host uses this when the caret lands back on a word
    /// (Android: Backspace over the space after `chào`, then `s` gives `cháo`).
    ///
    /// Returns whether `text` was taken. Only a Vietnamese syllable is — including
    /// one still missing a diacritic, whether the tone its stop coda requires
    /// (`chuc` + `s` → `chúc`) or its vowel shape (`dien` + `e` → `diên`), which is
    /// how a word looks when the user commits it before finishing it. English words
    /// and URLs never become editable; the host should leave the document untouched
    /// when this is `false`.
    ///
    /// The raw keystrokes that produced `text` are gone, so they are seeded from the
    /// text itself — the same state `on_backspace` leaves behind. Composition only
    /// ever reads the buffer, so tone and shape edits work from here; there is no raw
    /// form to flip back to, which makes the flip hotkey a no-op on an adopted word.
    pub fn adopt(&mut self, text: &str) -> bool {
        if !self.session.enabled || text.is_empty() || !is_reopenable_syllable(text) {
            return false;
        }
        // clear() + push_str refills the session strings in place, so seeding them
        // costs nothing; the syllable check above is the only allocation here (it
        // builds a rhyme string), the same one every word boundary already pays.
        self.session.clear();
        self.session.buffer.push_str(text);
        self.session.keys.push_str(text);
        self.session.vn_form.push_str(text);
        true
    }

    /// Synchronize engine state after a Backspace passed through to the host app.
    pub fn on_backspace(&mut self) -> ImeResult {
        if !self.session.enabled {
            return ImeResult::none();
        }
        self.session.buffer.pop();
        self.session.keys = self.session.buffer.clone();
        ImeResult::none()
    }

    /// Flip the live word between its Vietnamese and raw-keystroke forms.
    pub fn flip_composing(&mut self) -> ImeResult {
        match flip::flip(
            &self.session.buffer,
            &self.session.keys,
            &self.session.vn_form,
        ) {
            Some((new_buffer, override_)) => {
                let (backspace, output) = diff::diff(&self.session.buffer, &new_buffer);
                self.session.buffer = new_buffer;
                self.session.restore_override = Some(override_);
                ImeResult::send(backspace, output)
            }
            None => ImeResult::none(),
        }
    }
}
