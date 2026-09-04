//! On-disk `settings.json` schema (camelCase).
//!
//! Mirrors the platform settings readers (`platforms/windows/src/settings.rs`,
//! `platforms/linux/settings-gtk/src/settings.rs`); we keep a local copy rather
//! than depend on those platform-only crates. Every field has a serde default so
//! older or partial files still load, and an empty object (`{}`) yields the
//! canonical defaults — the single source of truth.

use funput_core::{InputMethod, ToneStyle};
use serde::Deserialize;

/// Input method as it serializes in `settings.json` (`"telex"`/`"vni"`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Deserialize)]
#[serde(rename_all = "lowercase")]
pub(super) enum Method {
    Telex,
    #[default]
    Vni,
}

impl From<Method> for InputMethod {
    fn from(m: Method) -> Self {
        match m {
            Method::Telex => InputMethod::Telex,
            Method::Vni => InputMethod::Vni,
        }
    }
}

/// Tone-mark placement as it serializes in `settings.json`
/// (`"traditional"`/`"modern"`).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Deserialize)]
#[serde(rename_all = "lowercase")]
pub(super) enum ToneStyleCfg {
    Traditional,
    #[default]
    Modern,
}

impl From<ToneStyleCfg> for ToneStyle {
    fn from(t: ToneStyleCfg) -> Self {
        match t {
            ToneStyleCfg::Traditional => ToneStyle::Traditional,
            ToneStyleCfg::Modern => ToneStyle::Modern,
        }
    }
}

/// A settings file written before tone style was configurable belongs to someone
/// already typing traditional placement — unlike no file at all, which is a new
/// install and takes [`ToneStyleCfg::default`].
fn legacy_tone_style() -> ToneStyleCfg {
    ToneStyleCfg::Traditional
}

/// A text-expansion shortcut (gõ tắt) as stored in `settings.json`.
#[derive(Debug, Clone, Deserialize)]
pub(super) struct Shortcut {
    pub(super) trigger: String,
    pub(super) expansion: String,
}

/// The subset of the on-disk settings that `funput-term` can act on.
#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(super) struct FileSettings {
    #[serde(default)]
    pub(super) method: Method,
    #[serde(default = "legacy_tone_style")]
    pub(super) tone_style: ToneStyleCfg,
    #[serde(default = "default_true")]
    pub(super) enabled: bool,
    #[serde(default = "default_true")]
    pub(super) smart_restore: bool,
    #[serde(default = "default_true")]
    pub(super) eager_restore: bool,
    #[serde(default)]
    pub(super) spell_check: bool,
    #[serde(default)]
    pub(super) auto_capitalize: bool,
    #[serde(default)]
    pub(super) shortcuts: Vec<Shortcut>,
    #[serde(default = "default_true")]
    pub(super) shortcuts_enabled: bool,
    #[serde(default = "default_true")]
    pub(super) shortcut_smart_case: bool,
}

fn default_true() -> bool {
    true
}
