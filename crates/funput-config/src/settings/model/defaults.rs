//! What a document reads as when a field — or the whole file — is absent.
//!
//! Split from [`super`] so the struct there stays the on-disk contract and nothing
//! else. Every function here answers one question: what did Funput do before this
//! field existed? An update may not silently change that answer.

use std::collections::BTreeMap;

use super::super::{FlipHotkey, Hotkey, Method, ToneStyle};
use super::Settings;

/// How many one-time repairs exist. Bump it in the same commit that adds one to
/// `Settings::repair`, never on its own.
pub(in crate::settings) const SCHEMA: u32 = 1;

/// The tone placement a file that predates the setting implies: whoever wrote it
/// has been typing traditional placement, whether they chose it or never looked.
/// A machine with no file at all is a new install and takes [`Settings::default`].
pub(in crate::settings) fn legacy_tone_style_default() -> ToneStyle {
    ToneStyle::Traditional
}

/// The default every switch that carries one shares: a file written before the
/// switch existed must decode as "on" — that is how Funput behaved when it was
/// written, and an update may not silently take a feature away. Each field says
/// why that reasoning holds for it.
pub(super) fn default_true() -> bool {
    true
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            method: Method::Vni,
            tone_style: ToneStyle::Modern,
            enabled: true,
            smart_restore: true,
            eager_restore: true,
            spell_check: false,
            auto_capitalize: false,
            toggle_hotkey: Hotkey::CtrlBacktick,
            toggle_combo: None,
            flip_hotkey: FlipHotkey::Off,
            flip_combo: None,
            launch_at_login: false,
            has_completed_onboarding: false,
            app_language_memory: BTreeMap::new(),
            // A document nobody has written yet needs no repair; only a file
            // missing the key reads as 0 and gets one.
            schema: SCHEMA,
            excluded_apps: Vec::new(),
            shortcuts: Vec::new(),
            shortcuts_enabled: true,
            shortcut_smart_case: true,
            shortcuts_in_english: true,
            auto_english_on_foreign_layout: true,
        }
    }
}
