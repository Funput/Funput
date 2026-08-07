//! The background process: the global keyboard hook and the tray icon.
//!
//! This is the process that runs for the whole session. It owns the Win32 message
//! loop, intercepts every keystroke before the focused app sees it, and injects
//! composed text back. It never initializes Slint — when the user asks for
//! Settings it spawns a separate short-lived process (see [`crate::ui`]), which is
//! what lets Windows reclaim the entire UI runtime when that window closes.
//!
//! Nothing here may block: every function below runs inside the hook callback,
//! between the user pressing a key and the app receiving it.

pub mod hook;
pub mod hotkey;
pub mod inject;
pub mod instance;
pub mod keymap;
pub mod tray;
