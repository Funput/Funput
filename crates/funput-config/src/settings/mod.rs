//! The settings a desktop shell persists, and where it keeps them.

mod combo;
mod hotkeys;
mod io;
mod method;
mod model;
pub mod path;

pub use combo::{KeyCombo, NO_KEY};
pub use hotkeys::{FlipHotkey, Hotkey};
pub use method::{Method, ToneStyle};
pub use model::{ExcludedApp, Settings, Shortcut};
