//! User-configurable engine options, grouped out of the per-word [`super::Session`]
//! state so adding a feature toggle touches one struct instead of the god-object.
//!
//! Every option here is reachable from the public `Engine::configure` /
//! `Engine::update_config` API (and, over the FFI, from `funput_configure` plus the
//! `funput_set_*` functions for the ones that do not ride the by-value C struct).

use funput_core::{InputMethod, ToneStyle};

/// The engine's user-facing options. Runtime composition state (buffer, raw keys,
/// capitalization tracking, the flip override) stays in the internal `Session`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct EngineConfig {
    /// Input method grammar (Telex / VNI / Telex Advanced).
    pub method: InputMethod,
    /// Tone-mark placement style (traditional `hòa` vs modern `hoà`).
    pub tone_style: ToneStyle,
    /// Auto-restore non-Vietnamese words to their raw Latin keys.
    pub smart_restore: bool,
    /// Restore the instant a word dead-ends, without waiting for a word boundary.
    /// Only meaningful while `smart_restore` is on.
    pub eager_restore: bool,
    /// Spell-check ("Kiểm tra chính tả"): only place a diacritic when the result can
    /// still become a real Vietnamese syllable. Off by default.
    pub spell_check: bool,
    /// Auto-capitalize ("Tự động viết hoa"): uppercase the first letter of a word at
    /// the start of a sentence. Off by default.
    pub auto_capitalize: bool,
    /// Whether the gõ tắt table expands at all ("Bật gõ tắt"). On by default.
    ///
    /// A switch rather than an empty table, so turning the feature off for a moment
    /// — to type a word that collides with a trigger — costs nothing and gives the
    /// rows back untouched.
    pub shortcuts_enabled: bool,
    /// Smart-case matching for gõ tắt ("Tự nhận diện hoa/thường"). On by default.
    ///
    /// On, a trigger typed lowercase, Title Case, or UPPERCASE all resolve to the same
    /// entry and the expansion is re-cased to match (`tp`/`Tp`/`TP` → `TP. HCM`/
    /// `Tp. Hcm`/`TP. HCM`). Off, only the exact trigger matches and the expansion
    /// comes out verbatim — for users whose expansions have a casing of their own.
    pub shortcut_smart_case: bool,
}

impl Default for EngineConfig {
    fn default() -> Self {
        Self {
            method: InputMethod::Telex,
            tone_style: ToneStyle::Traditional,
            smart_restore: true,
            eager_restore: true,
            spell_check: false,
            auto_capitalize: false,
            shortcuts_enabled: true,
            shortcut_smart_case: true,
        }
    }
}
