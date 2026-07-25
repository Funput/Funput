use std::collections::HashMap;

use funput_core::{InputMethod, ToneStyle};

use crate::{Engine, EngineConfig};

impl Engine {
    /// Apply a whole [`EngineConfig`] — the entry point platform shells use to push
    /// their settings.
    ///
    /// Two side effects: switching input method discards a word composed under the
    /// old grammar, and turning auto-capitalize off drops its pending state.
    /// `enabled` and the gõ tắt shortcuts are separate and left untouched.
    pub fn configure(&mut self, config: EngineConfig) {
        let method_changed = self.session.config.method != config.method;
        let auto_capitalize_off = !config.auto_capitalize;
        self.session.config = config;
        if method_changed {
            self.session.clear();
        }
        if auto_capitalize_off {
            self.session.cap_armed = false;
            self.session.cap_sentence_ended = false;
        }
    }

    /// Change part of the configuration, keeping the rest — for callers that flip one
    /// option rather than pushing a whole config (a settings field, a dev tool, a
    /// test). Routes through [`Engine::configure`], so the same side effects apply.
    pub fn update_config(&mut self, edit: impl FnOnce(&mut EngineConfig)) {
        let mut config = self.session.config.clone();
        edit(&mut config);
        self.configure(config);
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

    /// Switch input method on its own, discarding any composition typed under the old
    /// grammar. Kept as a dedicated call because the iOS and Android keyboards flip the
    /// method at runtime from their Telex/VNI key, outside any settings change.
    pub fn set_method(&mut self, method: InputMethod) {
        self.update_config(|config| config.method = method);
    }

    pub fn method(&self) -> InputMethod {
        self.session.config.method
    }

    pub fn tone_style(&self) -> ToneStyle {
        self.session.config.tone_style
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
