//! What the keyboard hook calls on every keystroke.
//!
//! These are the hot path — they run inside the low-level hook, before the
//! focused app sees the key — so they do no I/O and take no allocation beyond what
//! the engine itself needs.

use funput_engine::{ImeResult, KeySource};

use super::ShellState;
use crate::Caret;

impl ShellState {
    /// Feed one character to the engine, tagged with its physical [`KeySource`] so
    /// a numpad digit stays a literal number instead of acting as a VNI modifier.
    ///
    /// The two `tail` calls bracket the engine: the shadow needs the composition as
    /// it stood *before* the key (a word boundary wipes it, and that is exactly the
    /// text that just became committed) and the engine's verdict *after*.
    pub fn process_key(&mut self, c: char, source: KeySource) -> ImeResult {
        self.tail.before_key(self.engine.buffer());
        let result = self.engine.process_key(c, source);
        self.tail.after_key(c, &result, self.engine.buffer());
        result
    }

    /// Backspace while Vietnamese mode is on. The physical key always passes
    /// through, so the app deletes its own visible char; this only keeps the shell
    /// in step with it.
    ///
    /// Mid-word that means shortening the composition. With nothing composing, the
    /// character about to disappear is a committed one, and when its removal leaves
    /// the caret at the end of a finished Vietnamese word the engine re-opens that
    /// word — so `phủ` + Space + Backspace + `s` gives `phú` instead of `phủs`. The
    /// engine refuses anything that is not a Vietnamese syllable, which keeps English
    /// words and URLs literal. A refusal keeps the shadow, so Backspace goes on
    /// eating the word and re-opens it as soon as what is left *is* a syllable —
    /// `dungh` + Space + ⌫⌫ lands on `dung`, and the next tone key reaches it.
    pub fn on_backspace(&mut self) {
        if !self.engine.buffer().is_empty() {
            self.engine.on_backspace();
            return;
        }
        let Some(word) = self.tail.backspace() else {
            return;
        };
        let adopted = self.engine.adopt(word);
        self.tail.resolve(adopted);
    }

    /// Flip the word being composed VN↔raw; returns the delete+inject to apply.
    pub fn flip_composing(&mut self) -> ImeResult {
        self.engine.flip_composing()
    }

    /// The caret moved without typing — a caret key, Enter, a mouse click, a focus
    /// change — so nothing the shell typed is reliably in front of it any more.
    /// Commits whatever is composed, and tells the engine what is known about where
    /// the caret landed, so auto-capitalize reads the sentence from there rather
    /// than from keys typed somewhere else.
    pub fn caret_moved(&mut self, caret: Caret) {
        self.reset_composition();
        match caret {
            Caret::LineStart => self.engine.arm_capitalization(),
            Caret::Unknown => self.engine.disarm_capitalization(),
        }
    }

    /// Whether a word is being composed right now.
    pub fn is_composing(&self) -> bool {
        !self.engine.buffer().is_empty()
    }
}
