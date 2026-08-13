//! Plain functions the UI callbacks and the tray invoke.
//!
//! Each mutates the shared shell state (which applies to the engine and persists);
//! a few also apply an OS side effect. Grouped by what they touch: `settings` is
//! pure passthrough, `system` reaches outside Funput, `config` moves whole
//! documents, and `updates` runs the download flow.

mod config;
mod settings;
mod system;
mod updates;

pub use config::{export_config, import_config, import_unikey_macros};
pub use settings::*;
pub use system::{open_url, set_launch_at_login, sync_autostart};
pub use updates::{check_for_updates, install_update, relaunch_after_update};
