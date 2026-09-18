//! IME session state — enabled flag, input method, composition buffer.

use std::collections::HashMap;

use crate::compose::RestoreOverride;
use crate::model::EngineConfig;

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
    pub(crate) keys: String,
    /// A sentence-ending mark (`.`/`!`/`?`) was just seen; waiting for whitespace to
    /// confirm the next word starts a new sentence. Survives `clear()` — capitalize
    /// state spans word commits.
    pub(crate) cap_sentence_ended: bool,
    /// The next word's first letter should be capitalized. Set by a confirmed
    /// sentence start (whitespace after `.`/`!`/`?`, a newline) or focus; consumed
    /// when a word begins. Survives `clear()`.
    pub(crate) cap_armed: bool,
    /// Text-expansion table (gõ tắt): raw-keystroke trigger → expansion. Matched
    /// smart-case against `keys` at a word boundary, before English restore — a
    /// trigger typed lowercase, Title Case, or UPPERCASE all resolve to the same
    /// entry, re-casing the expansion to match. Config that lives for the whole
    /// session — `clear()` does not touch it.
    pub(crate) shortcuts: HashMap<String, String>,
    /// The composed Vietnamese form of the current word, captured each keystroke
    /// *before* an eager English-restore can collapse `buffer` to the raw keys. Lets
    /// the flip hotkey recover the Vietnamese form even after a restore. Per-word —
    /// reset by `clear()`.
    pub(crate) vn_form: String,
    /// A manual flip choice for the current word: pins the displayed form and keeps
    /// the word boundary from English-restoring it back. Per-word — reset by `clear()`.
    pub(crate) restore_override: Option<RestoreOverride>,
}

impl Session {
    pub(crate) fn new() -> Self {
        Self {
            enabled: true,
            config: EngineConfig::default(),
            buffer: String::new(),
            keys: String::new(),
            cap_sentence_ended: false,
            cap_armed: false,
            shortcuts: HashMap::new(),
            vn_form: String::new(),
            restore_override: None,
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

    pub(crate) fn clear(&mut self) {
        self.buffer.clear();
        self.keys.clear();
        self.vn_form.clear();
        self.restore_override = None;
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
    pub(crate) fn resync_after_backspace(&mut self) {
        self.keys.clear();
        self.keys.push_str(&self.buffer);
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
