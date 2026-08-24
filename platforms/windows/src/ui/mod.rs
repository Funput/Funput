//! The Slint UI: Settings, Onboarding, and Control Center.
//!
//! These run as separate short-lived processes, spawned by the background hook
//! (`--settings`, `--onboarding`, `--control-center`). Keeping Slint out of the
//! long-running process is what lets Windows reclaim the whole UI runtime —
//! renderer, font caches, graphics driver allocations — when a window closes.
//!
//! [`lifecycle`] is the one exception to that, and the seam between the two
//! processes: its `run_*` half runs in the child and opens a window, but its
//! `launch_*` half (`lifecycle::child`) runs on the *hook thread* — it spawns the
//! process and reaps it. It stays here because owning UI processes is a UI
//! concern; moving it to `shared` would only force `ui`'s window modules public.
//!
//! Keystrokes typed into these windows never reach the global hook — winit ignores
//! synthetic input — so the Vietnamese fields compose in-process via [`compose`].

mod control_center;
mod convert;
mod lifecycle;
mod models;
mod onboarding;
mod settings_callbacks;
mod settings_window;

pub mod compose;
pub mod mica;
pub mod recorder;
pub mod system_accent;

pub(crate) use lifecycle::{
    current_child_handle, launch_convert, launch_onboarding, launch_settings, reap_ui_child,
    run_control_center, run_convert, run_onboarding, run_settings, terminate_children,
    terminate_parent_for_update, toggle_control_center,
};
pub(crate) use settings_window::set_update_state;
