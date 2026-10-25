//! IME session state — enabled flag, input method, composition buffer.

use std::collections::HashMap;

use crate::compose::RestoreOverride;
use crate::compose::boundary::shortcut_undo::ShortcutUndo;
use crate::compose::doubled::Literal;
use crate::correction::CorrectionState;
use crate::model::{EngineConfig, NumberGlue, SentenceTrace};

/// Mutable session held by [`crate::Engine`]. Internal — not part of the public API.
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct Session {
    pub(crate) enabled: bool,
    /// User-configurable options (method, tone style, restore/spell/cap toggles).
    pub(crate) config: EngineConfig,
    /// Composed text currently shown in the app (the composition span).
    pub(crate) buffer: String,
    /// Raw keystrokes since the last word boundary. Lets English restore
    /// (phase E3) rebuild the original Latin text when the composed buffer is
    /// not a complete Vietnamese syllable (`keys != buffer && !is_complete_syllable(buffer)`).
    ///
    /// A revert rewrites it to what the revert left on screen (`pass` → `pas`), so it
    /// is then what English restore falls back to, and every typed key is in `literal`.
    /// Keys join through [`Session::push_key`]; a revert, Backspace, `adopt` and
    /// [`crate::compose::doubled::settle_keys`] rewrite it whole.
    pub(crate) keys: String,
    /// Every key since the word boundary, once the word has had a revert. Per-word —
    /// reset by `clear()` and by Backspace.
    pub(crate) literal: Literal,
    /// Auto-capitalize's sentence position. Survives `clear()` — see [`SentenceTrace`].
    pub(crate) sentence: SentenceTrace,
    /// Text-expansion table (gõ tắt): trigger → expansion. Matched smart-case at a
    /// word boundary, before English restore, against the keys as typed (`literal`
    /// when a revert dropped one), then `keys`, then the composed `buffer`, so `vn`
    /// and `đc` both work as triggers — a
    /// trigger typed lowercase, Title Case, or UPPERCASE all resolve to the same
    /// entry, re-casing the expansion to match. Config that lives for the whole
    /// session — `clear()` does not touch it.
    pub(crate) shortcuts: HashMap<String, String>,
    /// One-tap undo of the last gõ tắt expansion, and the trigger refused once.
    /// A word boundary's `clear()` keeps both: the undo is answered afterwards.
    pub(crate) shortcut_undo: ShortcutUndo,
    /// The composed Vietnamese form of the current word, captured each keystroke
    /// *before* an eager English-restore can collapse `buffer` to the raw keys. Lets
    /// the flip hotkey recover the Vietnamese form even after a restore. Per-word —
    /// reset by `clear()`.
    pub(crate) vn_form: String,
    /// A manual flip choice for the current word: pins the displayed form and keeps
    /// the word boundary from English-restoring it back. Per-word — reset by `clear()`.
    pub(crate) restore_override: Option<RestoreOverride>,
    /// Typo-correction state: the touch points behind the live word, a correction
    /// parked at a word boundary, and the one-tap undo behind it. Boxed and `None`
    /// until the setting is switched on — off, the feature costs one null check per
    /// keystroke and eight bytes here.
    pub(crate) correction: Option<Box<CorrectionState>>,
    /// Whether the current word is glued to a number on screen, which keeps gõ tắt
    /// off it. Unlike the rest of the per-word state, a digit still waiting for its
    /// word survives `clear()` — see [`NumberGlue`].
    pub(crate) glue: NumberGlue,
}

impl Session {
    pub(crate) fn new() -> Self {
        Self {
            enabled: true,
            config: EngineConfig::default(),
            buffer: String::new(),
            keys: String::new(),
            literal: Literal::new(),
            sentence: SentenceTrace::mid_text(),
            shortcuts: HashMap::new(),
            shortcut_undo: ShortcutUndo::default(),
            vn_form: String::new(),
            restore_override: None,
            correction: None,
            glue: NumberGlue::Loose,
        }
    }

    /// Whether English mode still owes the user gõ tắt: the raw keys are tracked
    /// and a trigger expands at a word boundary, but nothing is composed. An empty
    /// table answers `false`, so a user with no shortcuts gets the untouched English
    /// mode Funput has always had.
    pub(crate) fn english_shortcuts(&self) -> bool {
        self.config.shortcuts_enabled
            && self.config.shortcuts_in_english
            && !self.shortcuts.is_empty()
    }

    /// Record a key the user typed into the live word: `keys` always, `literal` while
    /// a revert has it following the word. Every key goes in through here, so the
    /// two cannot drift apart.
    pub(crate) fn push_key(&mut self, key: char) {
        self.keys.push(key);
        self.literal.follow(key);
    }

    pub(crate) fn clear(&mut self) {
        self.buffer.clear();
        self.keys.clear();
        self.literal.reset();
        self.vn_form.clear();
        self.restore_override = None;
        self.glue.end_word();
        // Only the touch log is per-word. A correction parked by the boundary that
        // is calling this has to outlive it — the platform answers it afterwards.
        if let Some(state) = self.correction.as_mut() {
            state.touch.reset();
        }
    }

    /// Bring the per-word state back in line with a `buffer` that Backspace just
    /// shortened. The counterpart of [`Session::clear`]: a word boundary throws the
    /// whole word away, Backspace eats it one character at a time.
    ///
    /// `vn_form` describes a composition that no longer exists, so it follows the
    /// buffer down — left standing, the flip hotkey would re-type the word that was
    /// just deleted.
    ///
    /// A flip choice only outlives the word it was made on. Correcting a typo
    /// mid-word is still that word, so the choice stays; emptying the buffer ends
    /// it, and the choice has to go with it or it would pin the *next* word too and
    /// keep its diacritics off.
    ///
    /// `literal` is dropped rather than shortened: which of its keys the deleted
    /// character stood for is no longer known, so the word leaves doubled-key restore.
    pub(crate) fn resync_after_backspace(&mut self) {
        self.keys.clear();
        self.keys.push_str(&self.buffer);
        self.literal.reset();
        self.vn_form.clear();
        self.vn_form.push_str(&self.buffer);
        if self.buffer.is_empty() {
            self.restore_override = None;
        }
    }
}

impl Default for Session {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use funput_core::InputMethod;

    use super::*;

    #[test]
    fn new_defaults() {
        let session = Session::new();
        assert!(session.enabled);
        assert_eq!(session.config.method, InputMethod::Telex);
        assert!(session.buffer.is_empty());
        assert!(session.keys.is_empty());
        assert!(session.shortcuts.is_empty());
    }

    #[test]
    fn clear_resets_buffer_and_keys() {
        let mut session = Session::new();
        session.buffer.push('á');
        session.keys.push_str("as");
        session.clear();
        assert!(session.buffer.is_empty());
        assert!(session.keys.is_empty());
    }

    /// The host resets the engine after committing a passed-through digit, so the
    /// digit has to outlive `clear()` while the word it glued to does not.
    #[test]
    fn clear_keeps_a_pending_digit_but_frees_the_glued_word() {
        let mut session = Session::new();
        session.glue = NumberGlue::Digit;
        session.clear();
        assert_eq!(session.glue, NumberGlue::Digit);
        session.glue = NumberGlue::Word;
        session.clear();
        assert_eq!(session.glue, NumberGlue::Loose);
    }

    #[test]
    fn clear_keeps_shortcuts() {
        // Shortcuts are session-wide config, not per-word state.
        let mut session = Session::new();
        session.shortcuts.insert("vn".into(), "Việt Nam".into());
        session.buffer.push('á');
        session.clear();
        assert_eq!(
            session.shortcuts.get("vn").map(String::as_str),
            Some("Việt Nam")
        );
    }

    /// A word flipped to its raw keys and then deleted character by character. The
    /// choice must not survive the word it was made on, or it pins the next one.
    #[test]
    fn emptying_the_buffer_drops_the_flip_choice() {
        let mut session = Session::new();
        session.buffer.push_str("mas");
        session.keys.push_str("mas");
        session.vn_form.push_str("má");
        session.restore_override = Some(RestoreOverride::ForceRaw);

        for _ in 0..3 {
            session.buffer.pop();
            session.resync_after_backspace();
        }
        assert!(session.keys.is_empty());
        assert!(session.vn_form.is_empty());
        assert_eq!(session.restore_override, None);
    }

    /// Backspacing inside a word is correcting a typo, not abandoning the word, so
    /// the flip choice stays — but `vn_form` still follows the shortened buffer, or
    /// the flip hotkey would re-type the characters just deleted.
    #[test]
    fn backspacing_inside_a_word_keeps_the_flip_choice() {
        let mut session = Session::new();
        session.buffer.push_str("mas");
        session.keys.push_str("mas");
        session.vn_form.push_str("má");
        session.restore_override = Some(RestoreOverride::ForceRaw);

        session.buffer.pop();
        session.resync_after_backspace();
        assert_eq!(session.keys, "ma");
        assert_eq!(session.vn_form, "ma");
        assert_eq!(session.restore_override, Some(RestoreOverride::ForceRaw));
    }
}
