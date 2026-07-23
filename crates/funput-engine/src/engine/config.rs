use std::collections::HashMap;

use funput_core::{InputMethod, ToneStyle};

use crate::Engine;

impl Engine {
    pub fn set_enabled(&mut self, enabled: bool) {
        self.session.enabled = enabled;
    }

    pub fn is_enabled(&self) -> bool {
        self.session.enabled
    }

    /// Change input method and discard any composition using the old grammar.
    pub fn set_method(&mut self, method: InputMethod) {
        if self.session.method != method {
            self.session.clear();
            self.session.method = method;
        }
    }

    pub fn method(&self) -> InputMethod {
        self.session.method
    }

    pub fn set_tone_style(&mut self, style: ToneStyle) {
        self.session.tone_style = style;
    }

    pub fn tone_style(&self) -> ToneStyle {
        self.session.tone_style
    }

    pub fn set_smart_restore(&mut self, on: bool) {
        self.session.smart_restore = on;
    }

    pub fn set_eager_restore(&mut self, on: bool) {
        self.session.eager_restore = on;
    }

    pub fn set_spell_check(&mut self, on: bool) {
        self.session.spell_check = on;
    }

    pub fn set_auto_capitalize(&mut self, on: bool) {
        self.session.auto_capitalize = on;
        if !on {
            self.session.cap_armed = false;
            self.session.cap_sentence_ended = false;
        }
    }

    pub fn arm_capitalization(&mut self) {
        if self.session.auto_capitalize {
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
