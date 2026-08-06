//! The three-step update flow, as the About pane drives it.
//!
//! All three steps run off the main thread — network and file I/O must not block
//! the Slint loop or the keyboard hook — and report progress back by marshalling
//! onto the event loop. The found update is stashed between "check" and "install"
//! so the UI callback can stay argument-free.

use std::sync::Mutex;

use crate::shared::update::{self, Manifest};
use crate::ui;

/// The update found by the last check, awaiting the user's "Tải và cài đặt".
static PENDING_UPDATE: Mutex<Option<Manifest>> = Mutex::new(None);

/// Check the GitHub Release feed for a newer build. Drives the About pane through
/// `checking` → `available`/`uptodate`/`error`.
pub fn check_for_updates() {
    set_update_ui("checking", "", "");
    std::thread::spawn(|| match update::fetch_manifest() {
        Ok(manifest) if update::is_newer(&manifest.version) => {
            let version = manifest.version.clone();
            *PENDING_UPDATE.lock().unwrap() = Some(manifest);
            set_update_ui("available", &version, "");
        }
        Ok(_) => {
            *PENDING_UPDATE.lock().unwrap() = None;
            set_update_ui("uptodate", "", "");
        }
        Err(e) => set_update_ui("error", "", &e.to_string()),
    });
}

/// Download, verify, and swap in the pending update. Drives the About pane through
/// `downloading` → `ready`/`error`. The relaunch waits for the user's confirmation.
pub fn install_update() {
    let Some(manifest) = PENDING_UPDATE.lock().unwrap().clone() else {
        return;
    };
    set_update_ui("downloading", &manifest.version, "");
    std::thread::spawn(move || {
        let outcome = update::download(&manifest.url, manifest.length).and_then(|bytes| {
            update::verify(&bytes, &manifest.ed_signature)?;
            update::stage_and_replace(&bytes)
        });
        match outcome {
            Ok(()) => set_update_ui("ready", &manifest.version, ""),
            Err(e) => set_update_ui("error", "", &e.to_string()),
        }
    });
}

/// Relaunch into the freshly installed build (no log-out needed — it is a plain
/// tray process). Never returns.
pub fn relaunch_after_update() {
    ui::terminate_parent_for_update();
    update::relaunch();
}

/// Push an update state onto the Settings window from any thread.
fn set_update_ui(state: &str, version: &str, message: &str) {
    let (state, version, message) = (state.to_owned(), version.to_owned(), message.to_owned());
    let _ = slint::invoke_from_event_loop(move || {
        ui::set_update_state(&state, &version, &message);
    });
}
