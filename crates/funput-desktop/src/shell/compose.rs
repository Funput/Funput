//! What the keyboard hook calls on every keystroke.
//!
//! These four are the hot path — they run inside the low-level hook, before the
//! focused app sees the key — so they do no I/O and take no allocation beyond what
//! the engine itself needs.

use funput_engine::{ImeResult, KeySource};

use super::ShellState;

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
    /// engine refuses anything that is not a complete syllable, which keeps English
    /// words and URLs literal; a refusal costs the shadow its bearings, so it
    /// starts over.
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

    /// Commit whatever is composed and forget where the caret was. Called for
    /// anything the shell cannot model — a caret key, Enter, a mouse click, a
    /// focus change — after which nothing it typed is reliably in front of the
    /// caret any more.
    pub fn clear(&mut self) {
        self.reset_composition();
    }

    /// Arm auto-capitalize for the next word (the engine no-ops unless the feature
    /// is on). Called on focus change so the first letter typed in a newly-focused
    /// app is capitalized, and after Enter for a new line.
    pub fn arm_capitalization(&mut self) {
        self.engine.arm_capitalization();
    }

    /// Whether a word is being composed right now.
    pub fn is_composing(&self) -> bool {
        !self.engine.buffer().is_empty()
    }
}
