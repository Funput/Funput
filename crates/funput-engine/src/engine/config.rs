use std::collections::HashMap;

use funput_core::{InputMethod, ToneStyle};

use crate::{Engine, EngineConfig};

impl Engine {
    /// Apply a whole [`EngineConfig`] at once — the batch equivalent of the
    /// individual `set_*` methods, preserving their side effects (a method change
    /// clears the in-progress word; turning auto-capitalize off resets its tracking).
    /// `enabled` and the gõ tắt shortcuts are separate and left untouched.
    pub fn configure(&mut self, config: EngineConfig) {
        self.set_method(config.method);
        self.set_tone_style(config.tone_style);
        self.set_smart_restore(config.smart_restore);
        self.set_eager_restore(config.eager_restore);
        self.set_spell_check(config.spell_check);
        self.set_auto_capitalize(config.auto_capitalize);
    }

    /// The current engine configuration (method, tone style, and the feature toggles).
    pub fn config(&self) -> &EngineConfig {
        &self.session.config
    }

    pub fn set_enabled(&mut self, enabled: bool) {
        self.session.enabled = enabled;
    }

    pub fn is_enabled(&self) -> bool {
        self.session.enabled
    }

    /// Change input method and discard any composition using the old grammar.
    pub fn set_method(&mut self, method: InputMethod) {
        if self.session.config.method != method {
            self.session.clear();
            self.session.config.method = method;
        }
    }

    pub fn method(&self) -> InputMethod {
        self.session.config.method
    }

    pub fn set_tone_style(&mut self, style: ToneStyle) {
        self.session.config.tone_style = style;
    }

    pub fn tone_style(&self) -> ToneStyle {
        self.session.config.tone_style
    }

    pub fn set_smart_restore(&mut self, on: bool) {
        self.session.config.smart_restore = on;
    }

    pub fn set_eager_restore(&mut self, on: bool) {
        self.session.config.eager_restore = on;
    }

    pub fn set_spell_check(&mut self, on: bool) {
        self.session.config.spell_check = on;
    }

    pub fn set_auto_capitalize(&mut self, on: bool) {
        self.session.config.auto_capitalize = on;
        if !on {
            self.session.cap_armed = false;
            self.session.cap_sentence_ended = false;
        }
    }

    pub fn arm_capitalization(&mut self) {
        if self.session.config.auto_capitalize {
            self.session.cap_armed = true;
        }
    }

    /// Reset per-word state without changing settings.
    pub fn clear(&mut self) {
        self.session.clear();
    }

    pub fn add_shortcut(&mut self, trigger: impl Into<String>, expansion: impl Into<String>) {
        let trigger = trigger.into();
        if !trigger.is_empty() {
            self.session.shortcuts.insert(trigger, expansion.into());
        }
    }

    pub fn remove_shortcut(&mut self, trigger: &str) {
        self.session.shortcuts.remove(trigger);
    }

    pub fn clear_shortcuts(&mut self) {
        self.session.shortcuts.clear();
    }

    pub fn shortcuts(&self) -> &HashMap<String, String> {
        &self.session.shortcuts
    }

    pub fn buffer(&self) -> &str {
        &self.session.buffer
    }

    pub fn keys(&self) -> &str {
        &self.session.keys
    }
}
