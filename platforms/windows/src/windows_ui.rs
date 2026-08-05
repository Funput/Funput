//! Short-lived Slint UI processes for Settings, Onboarding, and Control Center.
//!
//! The background tray process only uses [`lifecycle`] to launch a child. Window
//! components and callbacks are initialized exclusively inside that child.

mod control_center;
mod lifecycle;
mod models;
mod onboarding;
mod settings_callbacks;
mod settings_window;

pub(crate) use lifecycle::{
    launch_onboarding, launch_settings, reap_ui_child, run_control_center, run_onboarding,
    run_settings, terminate_children, terminate_parent_for_update, toggle_control_center,
};
pub(crate) use settings_window::set_update_state;
