//! What the keyboard hook calls on every keystroke.
//!
//! These are the hot path — they run inside the low-level hook, before the
//! focused app sees the key — so they do no I/O and take no allocation beyond what
//! the engine itself needs.

use funput_engine::{ImeResult, KeySource};

use super::ShellState;
use crate::{Caret, TriggerKey, undo_shortcut};

impl ShellState {
    /// Feed one character to the engine, tagged with its physical [`KeySource`] so
    /// a numpad digit stays a literal number instead of acting as a VNI modifier.
    ///
    /// The two `tail` calls bracket the engine: the shadow needs the composition as
    /// it stood *before* the key (a word boundary wipes it, and that is exactly the
    /// text that just became committed) and the engine's verdict *after*.
    pub fn process_key(&mut self, c: char, source: KeySource) -> ImeResult {
        // A completion list stays open while its word is typed on, and closes with it.
        self.completion &= !c.is_whitespace();
        self.tail.before_key(self.engine.buffer());
        let result = self.engine.process_key(c, source);
        self.tail.after_key(c, &result, self.engine.buffer());
        result
    }

    /// Backspace while the hook is live.
    ///
    /// Right after a gõ tắt expansion it takes the expansion back: the result is
    /// what to inject in place of the swallowed key (`Việt Nam ` → `vn `). Otherwise
    /// it returns `None`, the physical key passes through so the app deletes its own
    /// visible char, and this only keeps the shell in step with it.
    ///
    /// Mid-word that means shortening the composition. With nothing composing, the
    /// character about to disappear is a committed one, and when its removal leaves
    /// the caret at the end of a finished Vietnamese word the engine re-opens that
    /// word — so `phủ` + Space + Backspace + `s` gives `phú` instead of `phủs`. The
    /// engine refuses anything that is neither a Vietnamese syllable nor the start of
    /// a gõ tắt trigger, which keeps English words and URLs literal while a mistyped
    /// trigger stays fixable. A refusal keeps the shadow, so Backspace goes on
    /// eating the word and re-opens it as soon as what is left *is* a syllable —
    /// `dungh` + Space + ⌫⌫ lands on `dung`, and the next tone key reaches it.
    pub fn on_backspace(&mut self) -> Option<ImeResult> {
        if let Some(undo) = undo_shortcut(&mut self.engine, &mut self.tail) {
            return Some(undo);
        }
        if !self.engine.buffer().is_empty() {
            self.engine.on_backspace();
            return None;
        }
        let removed = self.tail.backspace()?;
        // A full stop leaves the caret on punctuation, so there is no word to
        // adopt — and that full stop is what the sentence scanner must forget.
        self.engine.rewind_sentence(removed.deleted);
        if let Some(word) = removed.word {
            let adopted = self.engine.adopt(word);
            self.tail.resolve(adopted);
        }
        None
    }

    /// Flip the word being composed VN↔raw; returns the delete+inject to apply.
    pub fn flip_composing(&mut self) -> ImeResult {
        self.engine.flip_composing()
    }

    /// Enter or Tab ended the word. When the Settings switch for that key is on and
    /// the word is a gõ tắt trigger, returns the expansion to inject; the hook then
    /// sends the real key itself, so Enter still sends or breaks the line. Either way
    /// the composition is committed as for any key that moves the caret.
    ///
    /// Over a word that opens an app's completion list (`@pcod`, see
    /// [`CommittedTail::opens_completion`](crate::CommittedTail::opens_completion)),
    /// the key is the list's: nothing expands, and Enter, which picked an entry
    /// rather than breaking a line, leaves the next word as typed (#712).
    pub fn trigger_key(&mut self, key: TriggerKey) -> Option<ImeResult> {
        let listing = self.completion || self.tail.opens_completion(self.engine.buffer());
        let wanted = !listing
            && match key {
                TriggerKey::Enter => self.settings.shortcut_on_enter,
                TriggerKey::Tab => self.settings.shortcut_on_tab,
            };
        let expansion = wanted.then(|| self.engine.expand_shortcut()).flatten();
        self.caret_moved(if listing { Caret::Unknown } else { key.caret() });
        expansion
    }

    /// The caret moved without typing — a caret key, Enter, a mouse click, a focus
    /// change — so nothing the shell typed is reliably in front of it any more.
    /// Commits whatever is composed, and tells the engine what is known about where
    /// the caret landed, so auto-capitalize reads the sentence from there rather
    /// than from keys typed somewhere else.
    ///
    /// Up and Down ([`Caret::Vertical`]) keep note of a completion list the word at
    /// the caret opened, for the Enter that picks from it after the shadow is gone.
    pub fn caret_moved(&mut self, caret: Caret) {
        self.completion = caret == Caret::Vertical
            && (self.completion || self.tail.opens_completion(self.engine.buffer()));
        self.reset_composition();
        match caret {
            Caret::LineStart => self.engine.arm_capitalization(),
            Caret::Unknown | Caret::Vertical => self.engine.disarm_capitalization(),
        }
    }

    /// Whether a word is being composed right now.
    pub fn is_composing(&self) -> bool {
        !self.engine.buffer().is_empty()
    }
}
