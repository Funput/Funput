//! User-configurable engine options, grouped out of the per-word [`super::Session`]
//! state so adding a feature toggle touches one struct instead of the god-object.
//!
//! Every option here is reachable from the public `Engine::configure` /
//! `Engine::update_config` API (and, over the FFI, from `funput_configure` plus the
//! `funput_set_*` functions for the ones that do not ride the by-value C struct).

use funput_core::{ComposeOptions, InputMethod, SyllableRules, ToneStyle};

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
    /// The spelling a word is judged against — native Vietnamese by default.
    ///
    /// Widening it admits more words everywhere at once: diacritics compose on
    /// them, and English restore (eager and at the word boundary) keeps them. Its
    /// one relaxation today is [`funput_core::ExtraOnsets`] — the UniKey switch
    /// "Cho phép phụ âm đầu Z, F, W, J" is
    /// `SyllableRules::STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ)` (`zô`, `jờ`,
    /// `fải`, `wá`). Off by default, because it also lets English through: Telex
    /// `fast` → `fát`, VNI `win10` → `win`. Android carries it through the separate
    /// JNI `nativeSetExtraOnsets` setter, keeping the original configure signature.
    pub syllable_rules: SyllableRules,
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
    /// Whether the gõ tắt table also expands in English mode ("Gõ tắt cả khi ở chế
    /// độ tiếng Anh"). On by default.
    ///
    /// Text expansion is not a Vietnamese feature, so turning composition off need
    /// not turn it off too. What English mode gives up is everything else: no
    /// diacritics, no English restore, no auto-capitalize, no flip — the raw keys
    /// are tracked only so a trigger can still be recognized at a word boundary.
    pub shortcuts_in_english: bool,
    /// Auto-correct a mistyped neighbouring key at the end of a word ("Tự sửa lỗi gõ
    /// nhầm phím"). Off by default, and inert until the host also reports where each
    /// touch landed — see [`crate::Engine::set_next_key_touch`].
    pub typo_correction: bool,
}

impl Default for EngineConfig {
    fn default() -> Self {
        Self {
            method: InputMethod::Telex,
            tone_style: ToneStyle::Modern,
            smart_restore: true,
            eager_restore: true,
            spell_check: false,
            syllable_rules: SyllableRules::STANDARD,
            auto_capitalize: false,
            shortcuts_enabled: true,
            shortcut_smart_case: true,
            shortcuts_in_english: true,
            typo_correction: false,
        }
    }
}

impl EngineConfig {
    /// What funput-core composes each keystroke under, built in one place so a new
    /// core option is wired here and nowhere else.
    pub(crate) fn compose_options(&self) -> ComposeOptions {
        ComposeOptions::new(self.method)
            .with_tone_style(self.tone_style)
            .with_spell_check(self.spell_check)
            .with_syllable_rules(self.syllable_rules)
    }
}
