//! Persisted Windows settings and UI-facing configuration enums.

mod combo;
mod hotkeys;
mod method;
mod model;
mod storage;

pub use combo::{KeyCombo, NO_KEY};
pub use hotkeys::{FlipHotkey, Hotkey};
pub use method::{Method, ToneStyle};
pub use model::{ExcludedApp, Settings, Shortcut};
