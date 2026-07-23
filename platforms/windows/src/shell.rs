//! Global engine + settings state shared between the keyboard-hook thread, the
//! tray, and the UI callbacks. The hook callback is a bare `extern "system"`
//! function with no user pointer, so this lives in a process-global behind a mutex.
//! No Windows APIs here.

use std::collections::HashMap;
use std::sync::{Mutex, OnceLock};

use funput_core::{InputMethod, ToneStyle as CoreToneStyle};
use funput_engine::{Engine, ImeResult, KeySource};

use crate::settings::{
    ExcludedApp, FlipHotkey, Hotkey, KeyCombo, Method, Settings, Shortcut, ToneStyle,
};

/// Tag stamped into `dwExtraInfo` of every event we synthesize via `SendInput`, so
/// the hook can recognize and ignore its own injected keystrokes (no re-entrancy).
pub const INJECT_TAG: usize = 0x4655_4E50; // "FUNP"

/// How many recently-focused apps to keep for the Settings "recent apps" picker.
const RECENT_CAP: usize = 12;

struct Shell {
    engine: Engine,
    settings: Settings,
    /// Recently-focused apps (most recent first), fed by the foreground hook. Not
    /// persisted — it's just a convenience source for the Settings UI.
    recent: Vec<ExcludedApp>,
    /// Per-app manual VI/EN overrides (runtime only, not persisted). A manual
    /// toggle records the user's choice here so the per-app auto-switch honours it
    /// on the next focus change instead of reverting to the exclusion-list default.
    /// Keyed by the lowercased exe id (e.g. "code.exe").
    overrides: HashMap<String, bool>,
    /// A manual toggle whose target app isn't known yet. The tray and the Settings
    /// toggle steal foreground (the taskbar/Settings window becomes foreground), so
    /// the choice is parked here and bound to the next app the user focuses.
    pending_override: Option<bool>,
}

static SHELL: OnceLock<Mutex<Shell>> = OnceLock::new();

fn apply_to_engine(engine: &mut Engine, s: &Settings) {
    engine.set_method(s.method.core());
    engine.set_tone_style(s.tone_style.core());
    engine.set_enabled(s.enabled);
    engine.set_smart_restore(s.smart_restore);
    engine.set_eager_restore(s.eager_restore);
    engine.set_spell_check(s.spell_check);
    engine.set_auto_capitalize(s.auto_capitalize);
    push_shortcuts(engine, &s.shortcuts);
    engine.clear();
}

/// Replace the engine's gõ tắt table with `shortcuts` (empty triggers are skipped by
/// the engine). The whole table is re-pushed after any edit — it's small and cheap.
fn push_shortcuts(engine: &mut Engine, shortcuts: &[Shortcut]) {
    engine.clear_shortcuts();
    for sc in shortcuts {
        engine.add_shortcut(sc.trigger.clone(), sc.expansion.clone());
    }
}

fn shell() -> &'static Mutex<Shell> {
    SHELL.get_or_init(|| {
        let settings = Settings::load();
        let mut engine = Engine::new();
        apply_to_engine(&mut engine, &settings);
        Mutex::new(Shell {
            engine,
            settings,
            recent: Vec::new(),
            overrides: HashMap::new(),
            pending_override: None,
        })
    })
}

fn with<R>(f: impl FnOnce(&mut Shell) -> R) -> R {
    let mut guard = shell().lock().expect("shell mutex poisoned");
    f(&mut guard)
}

/// Apply a VI/EN state to both the persisted settings and the live engine. Callers
/// persist (`save`) themselves, since some batch this with other field writes.
fn set_enabled_state(s: &mut Shell, on: bool) {
    s.settings.enabled = on;
    s.engine.set_enabled(on);
    if !on {
        s.engine.clear();
    }
}

// --- reads -----------------------------------------------------------------

pub fn snapshot() -> Settings {
    with(|s| s.settings.clone())
}
pub fn excluded_apps() -> Vec<ExcludedApp> {
    with(|s| s.settings.excluded_apps.clone())
}
pub fn recent_apps() -> Vec<ExcludedApp> {
    with(|s| s.recent.clone())
}
pub fn shortcuts() -> Vec<Shortcut> {
    with(|s| s.settings.shortcuts.clone())
}
pub fn enabled() -> bool {
    with(|s| s.settings.enabled)
}
pub fn method() -> InputMethod {
    with(|s| s.settings.method.core())
}
pub fn tone_style() -> CoreToneStyle {
    with(|s| s.settings.tone_style.core())
}
pub fn toggle_hotkey() -> Hotkey {
    with(|s| s.settings.toggle_hotkey)
}
pub fn flip_hotkey() -> FlipHotkey {
    with(|s| s.settings.flip_hotkey)
}
/// The user-recorded toggle combo, if one is set (overrides the preset).
pub fn toggle_combo() -> Option<KeyCombo> {
    with(|s| s.settings.toggle_combo.clone())
}
/// The user-recorded flip combo, if one is set (overrides the preset).
pub fn flip_combo() -> Option<KeyCombo> {
    with(|s| s.settings.flip_combo.clone())
}
pub fn is_composing() -> bool {
    with(|s| !s.engine.buffer().is_empty())
}

/// The user's current input method + tone style, for the in-process composer that the
/// Settings window's gõ tắt field uses (it can't go through the global hook).
pub fn method_and_tone() -> (InputMethod, CoreToneStyle) {
    with(|s| (s.settings.method.core(), s.settings.tone_style.core()))
}

/// Browsers whose URL bar inline-autofills a *selected* suffix that eats a
/// synthesized Backspace (Chrome's omnibox, Firefox's address bar). Chrome
/// Beta/Dev/Canary also report `chrome.exe`; Firefox Developer Edition/Nightly
/// also report `firefox.exe`. Edge (`msedge.exe`) and Brave (`brave.exe`)
/// deliberately do not match — they are unaffected.
const URLBAR_AUTOFILL_BROWSERS: [&str; 2] = ["chrome.exe", "firefox.exe"];

/// True when the most-recently-focused app is a browser whose URL bar autofill
/// swallows synthesized Backspaces. Used to route text injection through the
/// Delete-primer path (see `inject::send_plan_primed`). `recent[0]` is the
/// current foreground app (it is pushed there by the foreground hook).
pub fn foreground_has_urlbar_autofill() -> bool {
    with(|s| {
        s.recent
            .first()
            .map(|a| URLBAR_AUTOFILL_BROWSERS.contains(&a.id.as_str()))
            .unwrap_or(false)
    })
}

/// Reload settings written by the separate Settings process. Runtime-only recent
/// apps and per-app overrides stay in the background process; only persisted state
/// and the live composition engine are refreshed.
pub fn reload_settings() -> bool {
    let loaded = Settings::load();
    with(|s| {
        if s.settings == loaded {
            return false;
        }
        apply_to_engine(&mut s.engine, &loaded);
        s.settings = loaded;
        true
    })
}

/// Replace all settings at once (config import). Applies to the live engine and
/// persists to disk; the background hook process picks the change up via
/// `reload_settings`, exactly as it does for any single-field edit.
pub fn replace_settings(new: Settings) {
    with(|s| {
        apply_to_engine(&mut s.engine, &new);
        s.settings = new;
        s.settings.save();
    });
}

/// Seed a Settings child with the background process's runtime-only recent-app list.
pub fn seed_recent_apps(apps: Vec<ExcludedApp>) {
    with(|s| s.recent = apps);
}

// --- writes (each persists) ------------------------------------------------

/// Flip VI/EN from the tray; returns the new state. The tray click steals
/// foreground, so the choice is parked as a pending override and bound to the next
/// app the user focuses (see [`apply_for_app`]) — otherwise the per-app auto-switch
/// would revert it the instant focus returns to a non-excluded app.
pub fn toggle_enabled() -> bool {
    with(|s| {
        let on = !s.settings.enabled;
        set_enabled_state(s, on);
        s.pending_override = Some(on);
        s.settings.save();
        on
    })
}

/// Flip VI/EN from the keyboard hotkey; returns the new state. Unlike the tray, the
/// hotkey fires while the target app is focused, so the choice binds to that app
/// (`recent[0]`) immediately and clears any stale pending override.
pub fn toggle_enabled_hotkey() -> bool {
    with(|s| {
        let on = !s.settings.enabled;
        set_enabled_state(s, on);
        if let Some(app) = s.recent.first() {
            s.overrides.insert(app.id.clone(), on);
        }
        s.pending_override = None;
        s.settings.save();
        on
    })
}

pub fn set_enabled(on: bool) {
    with(|s| {
        set_enabled_state(s, on);
        // The Settings window holds focus while this runs, so treat it like the
        // tray: bind the choice to the next app the user returns to.
        s.pending_override = Some(on);
        s.settings.save();
    });
}

pub fn set_method(method: InputMethod) {
    with(|s| {
        s.settings.method = Method::from_core(method);
        s.engine.set_method(method);
        s.engine.clear();
        s.settings.save();
    });
}

pub fn set_tone_style(style: CoreToneStyle) {
    with(|s| {
        s.settings.tone_style = ToneStyle::from_core(style);
        s.engine.set_tone_style(style);
        s.settings.save();
    });
}

pub fn set_smart_restore(on: bool) {
    with(|s| {
        s.settings.smart_restore = on;
        s.engine.set_smart_restore(on);
        s.settings.save();
    });
}

pub fn set_eager_restore(on: bool) {
    with(|s| {
        s.settings.eager_restore = on;
        s.engine.set_eager_restore(on);
        s.settings.save();
    });
}

pub fn set_spell_check(on: bool) {
    with(|s| {
        s.settings.spell_check = on;
        s.engine.set_spell_check(on);
        s.settings.save();
    });
}

pub fn set_auto_capitalize(on: bool) {
    with(|s| {
        s.settings.auto_capitalize = on;
        s.engine.set_auto_capitalize(on);
        s.settings.save();
    });
}

/// Arm auto-capitalize for the next word (the engine no-ops unless the feature is on).
/// Called on foreground change so the first letter typed in a newly-focused app is
/// capitalized, and after Enter for a new line.
pub fn arm_capitalization() {
    with(|s| s.engine.arm_capitalization());
}

/// Picking a preset also clears any recorded custom combo — the two are
/// alternatives, and the combo (when present) always wins in the hook.
pub fn set_toggle_hotkey(hotkey: Hotkey) {
    with(|s| {
        s.settings.toggle_hotkey = hotkey;
        s.settings.toggle_combo = None;
        s.settings.save();
    });
}

pub fn set_toggle_combo(combo: KeyCombo) {
    with(|s| {
        s.settings.toggle_combo = Some(combo);
        s.settings.save();
    });
}

pub fn set_flip_hotkey(hotkey: FlipHotkey) {
    with(|s| {
        s.settings.flip_hotkey = hotkey;
        s.settings.flip_combo = None;
        s.settings.save();
    });
}

pub fn set_flip_combo(combo: KeyCombo) {
    with(|s| {
        s.settings.flip_combo = Some(combo);
        s.settings.save();
    });
}

/// Persist the launch-at-login preference. The registry side effect (auto-launch)
/// is applied by `commands`, which owns the OS integration.
pub fn set_launch_at_login(on: bool) {
    with(|s| {
        s.settings.launch_at_login = on;
        s.settings.save();
    });
}

pub fn complete_onboarding() {
    with(|s| {
        s.settings.has_completed_onboarding = true;
        s.settings.save();
    });
}

pub fn add_excluded_app(app: ExcludedApp) {
    with(|s| {
        if !s.settings.excluded_apps.iter().any(|a| a.id == app.id) {
            s.settings.excluded_apps.push(app);
            s.settings.save();
        }
    });
}

pub fn remove_excluded_app(id: &str) {
    with(|s| {
        let before = s.settings.excluded_apps.len();
        s.settings.excluded_apps.retain(|a| a.id != id);
        if s.settings.excluded_apps.len() != before {
            s.settings.save();
        }
    });
}

// --- shortcuts (gõ tắt) -----------------------------------------------------

/// Persist `shortcuts` and re-push the table to the live engine.
fn commit_shortcuts(s: &mut Shell) {
    push_shortcuts(&mut s.engine, &s.settings.shortcuts);
    s.settings.save();
}

pub fn add_shortcut() {
    with(|s| {
        s.settings.shortcuts.push(Shortcut {
            trigger: String::new(),
            expansion: String::new(),
        });
        commit_shortcuts(s);
    });
}

pub fn remove_shortcut(index: usize) {
    with(|s| {
        if index < s.settings.shortcuts.len() {
            s.settings.shortcuts.remove(index);
            commit_shortcuts(s);
        }
    });
}

pub fn set_shortcut_trigger(index: usize, trigger: String) {
    with(|s| {
        if let Some(sc) = s.settings.shortcuts.get_mut(index) {
            sc.trigger = trigger;
            commit_shortcuts(s);
        }
    });
}

pub fn set_shortcut_expansion(index: usize, expansion: String) {
    with(|s| {
        if let Some(sc) = s.settings.shortcuts.get_mut(index) {
            sc.expansion = expansion;
            commit_shortcuts(s);
        }
    });
}

// --- per-app auto-switch (called from the foreground hook) ------------------

/// Record the just-focused app for the Settings "recent apps" picker (deduped,
/// most-recent-first, capped). No-op for empty ids.
pub fn note_foreground(id: String, name: String) {
    if id.is_empty() {
        return;
    }
    with(|s| {
        s.recent.retain(|a| a.id != id);
        s.recent.insert(0, ExcludedApp { id, name });
        s.recent.truncate(RECENT_CAP);
    });
}

/// Decide VI/EN for the newly-focused app, in priority order:
///
/// 1. A pending manual toggle (from the tray / Settings, which steal foreground)
///    binds to this app — the user's choice lands on the app they return to.
/// 2. A remembered manual override for this app wins over the list default, so a
///    prior manual toggle survives leaving and re-focusing the app.
/// 3. Otherwise the exclusion-list default, mirroring the macOS shell: excluded
///    apps → English, every other app → Vietnamese. No-op when the list is empty,
///    so users who don't use the feature keep a plain global toggle.
///
/// Returns `Some(on)` when it flipped VI/EN (so the caller can refresh the tray),
/// `None` when nothing changed.
pub fn apply_for_app(id: &str) -> Option<bool> {
    with(|s| {
        let target = if let Some(on) = s.pending_override.take() {
            s.overrides.insert(id.to_string(), on);
            on
        } else if let Some(&on) = s.overrides.get(id) {
            on
        } else if s.settings.excluded_apps.is_empty() {
            return None;
        } else {
            !s.settings.excluded_apps.iter().any(|a| a.id == id)
        };

        if s.settings.enabled == target {
            return None;
        }
        set_enabled_state(s, target);
        s.settings.save();
        Some(target)
    })
}

// --- composition driving (called from the hook) ----------------------------

/// Feed one character to the engine, tagged with its physical [`KeySource`] so a
/// numpad digit stays a literal number instead of acting as a VNI modifier.
pub fn process_key(c: char, source: KeySource) -> ImeResult {
    with(|s| s.engine.process_key(c, source))
}

/// Flip the word being composed VN↔raw; returns the delete+inject to apply.
pub fn flip_composing() -> ImeResult {
    with(|s| s.engine.flip_composing())
}

/// Sync the engine after Backspace while composing; the physical Backspace then
/// passes through so the app deletes its own visible char (like `funput-term`).
pub fn on_backspace() {
    with(|s| {
        s.engine.on_backspace();
    });
}

pub fn clear() {
    with(|s| s.engine.clear());
}
