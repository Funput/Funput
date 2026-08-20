//! Re-opening a finished word after Backspace, on a shell that cannot read the
//! document.
//!
//! [`funput_engine::Engine::adopt`] takes the word the caret sits at the end of and
//! makes it the live composition again, so `phủ` + Space + Backspace + `s` gives
//! `phú`. Android asks the editor for that word (`getTextBeforeCursor`); a hook +
//! inject shell has no editor to ask.
//!
//! It does not need one. While Vietnamese mode is on, every character that reaches
//! the app is one this engine composed or waved through, so remembering the last
//! few of them answers the only question `adopt` asks.
//!
//! # Invariant
//!
//! `tail` followed by the engine's composition buffer is always a **suffix** of the
//! text before the caret. Remembering less than the truth is safe — the feature
//! simply does not fire. Remembering more would hand `adopt` a word that is not
//! there and let the next keystroke's diff delete characters Funput never wrote, so
//! every event the shell cannot model — a click, a caret key, a focus change, an
//! EN/VI toggle — must call [`CommittedTail::clear`].

use funput_engine::{Action, ImeResult};

/// How much committed text to keep, in bytes. Only the last word and whatever
/// separates it from the caret is ever read, so this only has to outlast a word:
/// the longest Vietnamese syllable (`nghiêng`) is 9 bytes in NFC.
const CAPACITY: usize = 64;

/// The tail of the text Funput has typed into the focused app, up to the live
/// composition. Fed by the shell on every keystroke; queried on Backspace.
#[derive(Debug, Default, Clone, PartialEq, Eq)]
pub struct CommittedTail {
    tail: String,
    /// The composition as it stood before the current key — a word boundary wipes
    /// it from the engine, and that is precisely the text that just got committed.
    composing: String,
    /// Where the word handed out by [`CommittedTail::backspace`] starts in `tail`.
    word_start: usize,
}

impl CommittedTail {
    pub fn new() -> Self {
        Self::default()
    }

    /// Forget everything. Call whenever the caret may have moved on its own.
    pub fn clear(&mut self) {
        self.tail.clear();
        self.composing.clear();
        self.word_start = 0;
    }

    /// Snapshot the engine's composition *before* it processes the next key.
    /// Pairs with [`CommittedTail::after_key`], once per composed keystroke.
    pub fn before_key(&mut self, composing: &str) {
        self.composing.clear();
        self.composing.push_str(composing);
    }

    /// Fold what the app now shows into the shadow, *after* the engine ran `key`.
    /// `composing` is the engine's buffer as it stands now.
    pub fn after_key(&mut self, key: char, result: &ImeResult, composing: &str) {
        if !composing.is_empty() {
            return; // still inside a word — it joins the shadow when it commits
        }
        match result.action {
            // The shell types `output` and swallows `key`: an English restore or a
            // gõ tắt expansion, boundary character included. The deletions paired
            // with it only ever remove the live composition, which is not in `tail`.
            Action::Send | Action::Restore => self.tail.push_str(&result.output),
            // Nothing injected: the app keeps the word already on screen (empty when
            // the key composed nothing, e.g. a second space) and echoes `key` itself.
            Action::None => {
                self.tail.push_str(&self.composing);
                self.tail.push(key);
            }
        }
        self.trim();
    }

    /// One Backspace with nothing composing: the app is about to delete the last
    /// committed character. Returns the finished word the caret then sits at the end
    /// of — offer it to [`funput_engine::Engine::adopt`] and report back with
    /// [`CommittedTail::resolve`].
    pub fn backspace(&mut self) -> Option<&str> {
        self.tail.pop()?;
        let start = word_start(&self.tail);
        if start == self.tail.len() {
            return None; // the caret landed on a separator, not on a word
        }
        self.word_start = start;
        Some(&self.tail[start..])
    }

    /// What the engine did with the word from [`CommittedTail::backspace`]. `true`:
    /// it is the live composition now, so the shadow hands it over and the invariant
    /// holds across the two. `false`: the word stays on screen and the shadow keeps
    /// describing it — the deleted character was folded in by `backspace` itself, so
    /// what remains is still a suffix of the text before the caret. Holding on is
    /// what lets a *later* Backspace re-open the word: `dungh` + Space + ⌫⌫ leaves
    /// the caret on `dung`, which the engine does take.
    pub fn resolve(&mut self, adopted: bool) {
        if adopted {
            self.tail.truncate(self.word_start);
        }
    }

    /// Keep the shadow bounded. Drops from the front only as far as a separator, so
    /// what remains never starts mid-word — and stays a suffix of the real text.
    fn trim(&mut self) {
        if self.tail.len() <= CAPACITY {
            return;
        }
        let floor = self.tail.len() - CAPACITY;
        let cut = self
            .tail
            .char_indices()
            .find(|(i, c)| *i >= floor && is_separator(*c))
            .map_or(self.tail.len(), |(i, c)| i + c.len_utf8());
        self.tail.drain(..cut);
    }
}

/// Byte index where the unbroken run of word characters at the end of `text`
/// begins; `text.len()` when `text` ends on a separator.
fn word_start(text: &str) -> usize {
    text.char_indices()
        .rev()
        .find(|(_, c)| is_separator(*c))
        .map_or(0, |(i, c)| i + c.len_utf8())
}

/// What ends a word on screen, mirroring the engine's own boundary rule. `[` and `]`
/// compose in advanced Telex rather than separate, but they only ever reach the
/// shadow inside a gõ tắt expansion, where separating is the right reading.
fn is_separator(c: char) -> bool {
    c.is_whitespace() || c.is_ascii_punctuation()
}

#[cfg(test)]
mod tests;
