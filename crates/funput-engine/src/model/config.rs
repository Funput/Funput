//! User-configurable engine options, grouped out of the per-word [`super::Session`]
//! state so adding a feature toggle touches one struct instead of the god-object.
//!
//! These six options map 1:1 to the public `Engine::set_*` methods (and, over the
//! FFI, to `funput_set_*`). Keeping them together is also the seam for a future
//! `Engine::configure(EngineConfig)` API — this type would simply become `pub`.

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
        }
    }
}
