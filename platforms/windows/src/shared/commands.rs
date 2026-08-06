//! Plain functions the UI callbacks (`crate::ui`) and the tray invoke. Each
//! mutates the shared `shell` state (which applies to the engine + persists); a
//! couple also apply an OS side effect (autostart registry, opening a link).

use std::path::Path;
use std::sync::Mutex;

use crate::shared::shell;
use crate::shared::update::{self, Manifest};
use crate::ui;
use funput_config::transfer::{self, ConfigError, ImportSummary, Source};
use funput_config::{ExcludedApp, FlipHotkey, Hotkey, KeyCombo, Method, ToneStyle};

pub fn set_method(method: Method) {
    shell::set_method(method.core());
}

pub fn set_tone_style(tone_style: ToneStyle) {
    shell::set_tone_style(tone_style.core());
}

pub fn set_smart_restore(on: bool) {
    shell::set_smart_restore(on);
}

pub fn set_eager_restore(on: bool) {
    shell::set_eager_restore(on);
}

pub fn set_spell_check(on: bool) {
    shell::set_spell_check(on);
}

pub fn set_auto_capitalize(on: bool) {
    shell::set_auto_capitalize(on);
}

pub fn set_enabled(on: bool) {
    shell::set_enabled(on);
}

pub fn set_toggle_hotkey(hotkey: Hotkey) {
    shell::set_toggle_hotkey(hotkey);
}

pub fn set_flip_hotkey(hotkey: FlipHotkey) {
    shell::set_flip_hotkey(hotkey);
}

pub fn set_toggle_combo(combo: KeyCombo) {
    shell::set_toggle_combo(combo);
}

pub fn set_flip_combo(combo: KeyCombo) {
    shell::set_flip_combo(combo);
}

pub fn complete_onboarding() {
    shell::complete_onboarding();
}

pub fn add_excluded_app(app: ExcludedApp) {
    shell::add_excluded_app(app);
}

pub fn remove_excluded_app(id: &str) {
    shell::remove_excluded_app(id);
}

// --- Shortcuts (gõ tắt) -----------------------------------------------------

pub fn add_shortcut() {
    shell::add_shortcut();
}

pub fn can_add_shortcut() -> bool {
    shell::can_add_shortcut()
}

pub fn prune_incomplete_shortcuts() {
    shell::prune_incomplete_shortcuts();
}

pub fn remove_shortcut(index: usize) {
    shell::remove_shortcut(index);
}

pub fn set_shortcut_trigger(index: usize, trigger: String) {
    shell::set_shortcut_trigger(index, trigger);
}

pub fn set_shortcut_expansion(index: usize, expansion: String) {
    shell::set_shortcut_expansion(index, expansion);
}

/// Persist the launch-at-login preference and mirror it into the OS autostart
/// (HKCU `…\Run`) via `auto-launch`.
pub fn set_launch_at_login(on: bool) {
    shell::set_launch_at_login(on);
    sync_autostart(on);
}

/// Bring the OS autostart entry in line with `on`. Called on startup (from the
/// persisted preference) and whenever the toggle changes.
pub fn sync_autostart(on: bool) {
    let Some(auto) = autolaunch() else { return };
    let _ = if on { auto.enable() } else { auto.disable() };
}

fn autolaunch() -> Option<auto_launch::AutoLaunch> {
    let exe = std::env::current_exe().ok()?;
    auto_launch::AutoLaunchBuilder::new()
        .set_app_name("Funput")
        .set_app_path(&exe.to_string_lossy())
        .build()
        .ok()
}

/// Open an external link (GitHub / Website) in the system browser.
pub fn open_url(url: &str) {
    let _ = open::that(url);
}

// --- Config Export / Import -------------------------------------------------

/// Write the current settings to `path` as an interchange file.
///
/// `Source` is filled in here, not by `funput-config`: `CARGO_PKG_VERSION` has to
/// be *this* crate's version (the shipped app's), not the config crate's.
pub fn export_config(path: &Path) -> std::io::Result<()> {
    let source = Source {
        platform: "windows".to_string(),
        app_version: env!("CARGO_PKG_VERSION").to_string(),
    };
    transfer::export_to(path, &shell::snapshot(), source)
}

/// Merge a config file into the live settings (applies to the engine + persists).
pub fn import_config(path: &Path) -> Result<ImportSummary, ConfigError> {
    let mut settings = shell::snapshot();
    let summary = transfer::import_file(path, &mut settings)?;
    shell::replace_settings(settings);
    Ok(summary)
}

// --- Auto-update ------------------------------------------------------------
//
// All three steps run off the main thread (network + file I/O must not block the
// Slint loop or the keyboard hook) and report progress back to the About pane via
// `ui::set_update_state` marshalled onto the event loop. The available
// update is stashed between "check" and "install" so the UI callback can stay
// argument-free.

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
