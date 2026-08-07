//! Code both processes run.
//!
//! Funput ships as one executable that behaves as two programs — the background
//! hook/tray, and the short-lived Slint windows it spawns (`--settings` and
//! friends). What lives here is what neither owns exclusively: the settings state
//! both read and write, the commands the tray and the UI both invoke, and the
//! executable's own identity and updates.
//!
//! A module belongs here only when *both* processes genuinely use it. Anything
//! else belongs in [`crate::background`] or [`crate::ui`], where its lifetime is
//! obvious from the path.

pub mod canonical_exe;
pub mod commands;
pub mod dark_mode;
pub mod settings_path;
pub mod shell;
pub mod update;
