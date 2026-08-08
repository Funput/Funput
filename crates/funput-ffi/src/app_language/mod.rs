//! Per-app VI/EN memory C ABI: the opaque [`FunputAppLanguage`] handle and the
//! `funput_app_language_*` calls that drive it.
//!
//! Independent of [`crate::FunputEngine`] — this only answers "should this app
//! be Vietnamese", it never touches composition. The host wires the answer into
//! [`crate::funput_set_enabled`] itself, and owns persisting the remembered map
//! to its own settings store (this engine does no file I/O, same as the gõ tắt
//! shortcut table).
//!
//! - [`handle`] — the opaque handle, its lifecycle, and shared marshalling.
//! - [`memory`] — seeding, forgetting, and clearing remembered apps.
//! - [`focus`] — the runtime decision calls a focus hook drives.
//! - [`types`] — the `APP_LANG_*` result codes.

mod focus;
mod handle;
mod memory;
mod types;

pub use focus::{funput_app_language_note_focus, funput_app_language_note_toggle};
pub use handle::{FunputAppLanguage, funput_app_language_free, funput_app_language_new};
pub use memory::{funput_app_language_clear, funput_app_language_forget, funput_app_language_seed};
pub use types::{APP_LANG_ENGLISH, APP_LANG_UNKNOWN, APP_LANG_VIETNAMESE};
