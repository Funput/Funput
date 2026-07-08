//! IME orchestration — session, buffer, and platform inject instructions.
//!
//! `funput-core` answers: given buffer + key, what is the new composed text?
//! `funput-engine` answers: after this key, what should the platform do?
//!
//! # API FROZEN (Phase E4)
//!
//! Public surface: [`Engine`], [`Action`], [`ImeResult`], and their methods.
//! Breaking changes require semver coordination with `funput-ffi` and platform shells.
//!
//! # Contract
//!
//! - **Stateful:** holds composition buffer across keystrokes.
//! - **Delegates transform:** all Telex/VNI rules live in `funput-core`.
//! - **No I/O:** no keyboard hooks, no inject — platform reads [`ImeResult`].

mod boundary;
mod diff;
mod flip;
mod pipeline;
mod result;
mod session;

pub use result::{Action, ImeResult};

use session::Session;

/// Vietnamese IME engine — single source of truth for composition state.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Engine {
    session: Session,
}

impl Engine {
    /// New engine with IME enabled and Telex as the default input method.
    pub fn new() -> Self {
        Self {
            session: Session::new(),
        }
    }

    /// Enable or disable Vietnamese composition. When disabled, [`Self::process_char`]
    /// returns [`Action::None`] and does not update buffer or keys.
    pub fn set_enabled(&mut self, enabled: bool) {
        self.session.enabled = enabled;
    }

    /// Whether Vietnamese composition is active.
    pub fn is_enabled(&self) -> bool {
        self.session.enabled
    }

    /// Switch between Telex and VNI digit modifiers.
    pub fn set_method(&mut self, method: funput_core::InputMethod) {
        self.session.method = method;
    }

    /// Current input method.
    pub fn method(&self) -> funput_core::InputMethod {
        self.session.method
    }

    /// Set the tone-mark placement style (traditional `hòa` vs modern `hoà`).
    pub fn set_tone_style(&mut self, style: funput_core::ToneStyle) {
        self.session.tone_style = style;
    }

    /// Current tone-mark placement style.
    pub fn tone_style(&self) -> funput_core::ToneStyle {
        self.session.tone_style
    }

    /// Toggle auto-restore of non-Vietnamese words to their raw Latin keystrokes
    /// (`card` stays `card` instead of composing `cảd`). When off, the composed
    /// buffer is always kept.
    pub fn set_smart_restore(&mut self, on: bool) {
        self.session.smart_restore = on;
    }

    /// Toggle eager restore — flip to raw keys the instant a word becomes a dead
    /// end, instead of waiting for a word boundary. Only applies while smart
    /// restore is on.
    pub fn set_eager_restore(&mut self, on: bool) {
        self.session.eager_restore = on;
    }

    /// Toggle spell-check ("Kiểm tra chính tả"): when on, a tone / shape / stroke is
    /// only placed if the result can still become a real Vietnamese syllable — an
    /// invalid one (`mix` + ngã → `mĩx`) keeps the modifier key as a literal instead.
    pub fn set_spell_check(&mut self, on: bool) {
        self.session.spell_check = on;
    }

    /// Toggle auto-capitalize ("Tự động viết hoa"): uppercase the first letter of a
    /// word that starts a sentence (sentence start, after `.`/`!`/`?` + space, after a
    /// newline). Off by default; when off this is a complete no-op.
    pub fn set_auto_capitalize(&mut self, on: bool) {
        self.session.auto_capitalize = on;
        if !on {
            self.session.cap_armed = false;
            self.session.cap_sentence_ended = false;
        }
    }

    /// Arm capitalization for the next word — the platform calls this when a text
    /// field gains focus, so the first letter typed (start of input) is capitalized.
    /// No-op unless auto-capitalize is on.
    pub fn arm_capitalization(&mut self) {
        if self.session.auto_capitalize {
            self.session.cap_armed = true;
        }
    }

    /// Reset composition state (buffer and raw keys) without changing enabled/method.
    pub fn clear(&mut self) {
        self.session.clear();
    }

    /// Define a text-expansion shortcut (gõ tắt): typing `trigger` then a word
    /// boundary injects `expansion` (`add_shortcut("vn", "Việt Nam")`). Triggers
    /// match the raw keystrokes case-sensitively and take priority over English
    /// restore. An empty `trigger` is ignored. Re-adding a trigger overwrites it.
    pub fn add_shortcut(&mut self, trigger: impl Into<String>, expansion: impl Into<String>) {
        let trigger = trigger.into();
        if trigger.is_empty() {
            return;
        }
        self.session.shortcuts.insert(trigger, expansion.into());
    }

    /// Remove a single shortcut by its trigger. No-op if it is not defined.
    pub fn remove_shortcut(&mut self, trigger: &str) {
        self.session.shortcuts.remove(trigger);
    }

    /// Remove every shortcut. Combine with [`Self::add_shortcut`] to replace the
    /// whole table when syncing from a config file.
    pub fn clear_shortcuts(&mut self) {
        self.session.shortcuts.clear();
    }

    /// The current shortcut table — trigger → expansion.
    pub fn shortcuts(&self) -> &std::collections::HashMap<String, String> {
        &self.session.shortcuts
    }

    /// Composed syllable buffer — text the app should show for the current word.
    pub fn buffer(&self) -> &str {
        &self.session.buffer
    }

    /// Raw keystrokes since the last word boundary (used for English restore).
    pub fn keys(&self) -> &str {
        &self.session.keys
    }

    /// The user pressed Backspace inside the current composition: drop the last
    /// character so the next keystroke composes against the corrected text
    /// (`Phua` → ⌫ → `Phu` → `s` → `Phú`, instead of losing the context).
    ///
    /// Returns [`Action::None`] — the Backspace key passes through so the app
    /// deletes its own last character, keeping app and engine in sync.
    pub fn on_backspace(&mut self) -> ImeResult {
        if !self.session.enabled {
            return ImeResult::none();
        }
        self.session.buffer.pop();
        // The remaining buffer is the corrected raw text for this word.
        self.session.keys = self.session.buffer.clone();
        ImeResult::none()
    }

    /// Flip the word being composed between its Vietnamese form and its raw
    /// keystrokes (`card` ⇄ `cải`), and back on a second call. Returns the
    /// delete+inject instruction ([`Action::Send`]) for hosts that type real text,
    /// or [`Action::None`] when there is nothing to flip: no live composition, or
    /// the word has no Vietnamese/raw distinction (`the`). Hosts that show marked
    /// text can ignore the payload and re-render [`Self::buffer`].
    ///
    /// The choice is sticky: further keystrokes keep the chosen form and the word
    /// boundary won't English-restore it back.
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

    /// Process one Unicode scalar (platform maps keycode → char).
    ///
    /// # Behavior
    ///
    /// - **Disabled:** [`Action::None`], state unchanged.
    /// - **Word boundary** (whitespace / ASCII punctuation): optionally restore Latin
    ///   via [`Action::Send`] when `keys != buffer` and buffer is not a complete
    ///   Vietnamese syllable; then clear session. Otherwise pass the boundary key.
    /// - **Normal key:** append to `keys`, call `funput-core`, map
    ///   `TransformKind` → `ImeResult` (see the README).
    pub fn process_char(&mut self, key: char) -> ImeResult {
        if !self.session.enabled {
            return ImeResult::none();
        }
        if boundary::is_word_boundary(key) {
            return boundary::on_word_boundary(&mut self.session, key);
        }
        let key = self.maybe_capitalize(key);
        self.session.keys.push(key);
        pipeline::process(&mut self.session, key)
    }

    /// Auto-capitalize the first letter of a new word when armed. Consumes the armed
    /// state at the start of any word (letter or not), so it only ever affects the
    /// first keystroke. The first keystroke of a Telex/VNI word is an ASCII Latin
    /// letter, so `to_ascii_uppercase` is sufficient.
    fn maybe_capitalize(&mut self, key: char) -> char {
        if !self.session.auto_capitalize || !self.session.buffer.is_empty() {
            return key;
        }
        let armed = self.session.cap_armed;
        self.session.cap_armed = false;
        self.session.cap_sentence_ended = false;
        if armed && key.is_alphabetic() {
            key.to_ascii_uppercase()
        } else {
            key
        }
    }
}

impl Default for Engine {
    fn default() -> Self {
        Self::new()
    }
}
