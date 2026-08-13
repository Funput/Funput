//! Persisted settings and the Export/Import document, for every desktop shell.
//!
//! Both are pure `serde` over plain data — no OS APIs, no UI toolkit — so they
//! build and unit-test on any host. That matters: the shells that own them
//! (`platforms/windows`, `platforms/linux/settings-gtk`) each link a heavy
//! platform-only dependency and cannot be compiled, let alone tested, from a
//! developer machine running a different OS.
//!
//! # Layout
//!
//! - `settings` — what the app persists: the [`Settings`] model, the enums it is
//!   made of, where the file lives ([`settings::path`]), and reading/writing it.
//! - `transfer` — the versioned interchange document behind Export/Import,
//!   specified in `platforms/CONFIG_FORMAT.md`.
//! - `unikey` — reading UniKey's gõ tắt table, so a user switching over does not
//!   retype it. Its rows come out as `transfer`'s own shape, which lets the import
//!   reuse that module's merge rule instead of restating it.
//!
//! # Two things that stay with the platform
//!
//! Resolving the *actual* settings path needs `current_exe`/`config_dir`, and the
//! `platform.<name>` block of an exported file only applies on that OS. This crate
//! provides the decisions ([`settings::path::resolve`]) and the shapes; each shell
//! supplies the inputs.

pub mod settings;
pub mod transfer;
pub mod unikey;

#[cfg(test)]
mod test_support;

pub use settings::{
    ExcludedApp, FlipHotkey, Hotkey, KeyCombo, Method, NO_KEY, Settings, Shortcut, ToneStyle,
};
