//! Global engine + settings state shared between the keyboard-hook thread, the
//! tray, and the UI callbacks. The hook callback is a bare `extern "system"`
//! function with no user pointer, so this lives in a process-global behind a mutex.
//! No Windows APIs here.

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::{Mutex, OnceLock};

use funput_config::{
    ExcludedApp, FlipHotkey, Hotkey, KeyCombo, Method, Settings, Shortcut, ToneStyle,
};
use funput_core::{InputMethod, ToneStyle as CoreToneStyle};
use funput_desktop::CommittedTail;
use funput_engine::{Engine, EngineConfig, ImeResult, KeySource};

use crate::settings_path;

/// Tag stamped into `dwExtraInfo` of every event we synthesize via `SendInput`, so
/// the hook can recognize and ignore its own injected keystrokes (no re-entrancy).
pub const INJECT_TAG: usize = 0x4655_4E50; // "FUNP"

/// How many recently-focused apps to keep for the Settings "recent apps" picker.
const RECENT_CAP: usize = 12;

struct Shell {
    engine: Engine,
    /// Shadow of the text Funput has typed into the focused app, up to the live
    /// composition. A hook shell cannot read the document, so this stands in for it
    /// when Backspace should re-open a finished word (see [`on_backspace`]). It only
    /// ever holds text this process typed, and any event that may have moved the
    /// caret behind our back drops it — see [`reset_composition`].
    tail: CommittedTail,
    settings: Settings,
    /// Where `settings` is persisted, resolved once at startup. `None` when no
    /// writable location exists at all — settings then live for this session only.
    settings_file: Option<PathBuf>,
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

impl Shell {
    /// Read the settings file (defaults when it is missing, corrupt, or there is
    /// nowhere to keep one).
    fn read_settings(&self) -> Settings {
        self.settings_file
            .as_deref()
            .map(Settings::load_from)
            .unwrap_or_default()
    }

    /// Persist the current settings.
    fn save(&self) {
        if let Some(path) = &self.settings_file {
            self.settings.save_to(path);
        }
    }
}

/// Push everything in `s.settings` to the engine and start from a clean slate.
fn apply_settings(s: &mut Shell) {
    sync_engine_config(&mut s.engine, &s.settings);
    s.engine.set_enabled(s.settings.enabled);
    push_shortcuts(&mut s.engine, &s.settings.shortcuts);
    reset_composition(s);
}

/// Drop the live composition *and* the committed-text shadow together. The two
/// model one stretch of text around the caret, so one must never outlive the other:
/// a shadow left standing after the engine forgot the word would let Backspace
/// re-open text that is no longer where Funput thinks it is.
fn reset_composition(s: &mut Shell) {
    s.engine.clear();
    s.tail.clear();
}

/// Push the six engine options from `settings` in one call. `settings` is the single
/// source of truth, so every per-setting entry point below just mutates its field and
/// re-syncs — the engine can never drift from what was persisted. `enabled` and the
/// gõ tắt table are separate (see [`set_enabled_state`] / [`push_shortcuts`]).
fn sync_engine_config(engine: &mut Engine, s: &Settings) {
    engine.configure(EngineConfig {
        method: s.method.core(),
        tone_style: s.tone_style.core(),
        smart_restore: s.smart_restore,
        eager_restore: s.eager_restore,
        spell_check: s.spell_check,
        auto_capitalize: s.auto_capitalize,
    });
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
        let mut shell = Shell {
            engine: Engine::new(),
            tail: CommittedTail::new(),
            settings: Settings::default(),
            settings_file: settings_path::settings_path(),
            recent: Vec::new(),
            overrides: HashMap::new(),
            pending_override: None,
        };
        shell.settings = shell.read_settings();
        apply_settings(&mut shell);
        Mutex::new(shell)
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
    // Both directions: English-mode keystrokes never reach the engine, so whatever
    // the shadow held before the switch no longer describes the text at the caret.
    reset_composition(s);
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

/// The user's current input method + tone style, for the in-process composer that the
/// Settings window's gõ tắt field uses (it can't go through the global hook).
pub fn method_and_tone() -> (InputMethod, CoreToneStyle) {
    with(|s| (s.settings.method.core(), s.settings.tone_style.core()))
}

/// Browsers whose URL bar inline-autofills a *selected* suffix that eats a
/// synthesized Backspace (Chrome's omnibox, Firefox's address bar). Chrome
/// Beta/Dev/Canary also report `chrome.exe`; Firefox Developer Edition/Nightly
/// also report `firefox.exe`. Zen (Firefox fork) reports `zen.exe`. Edge
/// (`msedge.exe`) and Brave (`brave.exe`) deliberately do not match — they
/// are unaffected.
const URLBAR_AUTOFILL_BROWSERS: [&str; 3] = ["chrome.exe", "firefox.exe", "zen.exe"];

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
    with(|s| {
        let loaded = s.read_settings();
        if s.settings == loaded {
            return false;
        }
        s.settings = loaded;
        apply_settings(s);
        true
    })
}

/// Replace all settings at once (config import). Applies to the live engine and
/// persists to disk; the background hook process picks the change up via
/// `reload_settings`, exactly as it does for any single-field edit.
pub fn replace_settings(new: Settings) {
    with(|s| {
        s.settings = new;
        apply_settings(s);
        s.save();
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
        s.save();
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
        s.save();
        on
    })
}

pub fn set_enabled(on: bool) {
    with(|s| {
        set_enabled_state(s, on);
        // The Settings window holds focus while this runs, so treat it like the
        // tray: bind the choice to the next app the user returns to.
        s.pending_override = Some(on);
        s.save();
    });
}

pub fn set_method(method: InputMethod) {
    // Spelled out rather than routed through `update_config` so the extra `clear()`
    // happens under the *same* lock: the hook thread must never see the new method
    // with a buffer still composed under the old grammar.
    with(|s| {
        s.settings.method = Method::from_core(method);
        sync_engine_config(&mut s.engine, &s.settings);
        reset_composition(s);
        s.save();
    });
}

pub fn set_tone_style(style: CoreToneStyle) {
    update_config(|s| s.tone_style = ToneStyle::from_core(style));
}

pub fn set_smart_restore(on: bool) {
    update_config(|s| s.smart_restore = on);
}

pub fn set_eager_restore(on: bool) {
    update_config(|s| s.eager_restore = on);
}

pub fn set_spell_check(on: bool) {
    update_config(|s| s.spell_check = on);
}

pub fn set_auto_capitalize(on: bool) {
    update_config(|s| s.auto_capitalize = on);
}

/// Mutate one engine option in `settings`, then re-sync the whole config and persist.
/// Folding the three steps here keeps every toggle above a single line and makes it
/// impossible to change a setting without pushing it to the engine.
fn update_config(edit: impl FnOnce(&mut Settings)) {
    with(|s| {
        edit(&mut s.settings);
        sync_engine_config(&mut s.engine, &s.settings);
        s.save();
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
        s.save();
    });
}

pub fn set_toggle_combo(combo: KeyCombo) {
    with(|s| {
        s.settings.toggle_combo = Some(combo);
        s.save();
    });
}

pub fn set_flip_hotkey(hotkey: FlipHotkey) {
    with(|s| {
        s.settings.flip_hotkey = hotkey;
        s.settings.flip_combo = None;
        s.save();
    });
}

pub fn set_flip_combo(combo: KeyCombo) {
    with(|s| {
        s.settings.flip_combo = Some(combo);
        s.save();
    });
}

/// Persist the launch-at-login preference. The registry side effect (auto-launch)
/// is applied by `commands`, which owns the OS integration.
pub fn set_launch_at_login(on: bool) {
    with(|s| {
        s.settings.launch_at_login = on;
        s.save();
    });
}

pub fn complete_onboarding() {
    with(|s| {
        s.settings.has_completed_onboarding = true;
        s.save();
    });
}

pub fn add_excluded_app(app: ExcludedApp) {
    with(|s| {
        if !s.settings.excluded_apps.iter().any(|a| a.id == app.id) {
            s.settings.excluded_apps.push(app);
            s.save();
        }
    });
}

pub fn remove_excluded_app(id: &str) {
    with(|s| {
        let before = s.settings.excluded_apps.len();
        s.settings.excluded_apps.retain(|a| a.id != id);
        if s.settings.excluded_apps.len() != before {
            s.save();
        }
    });
}

// --- shortcuts (gõ tắt) -----------------------------------------------------

/// A row the engine and disk will keep — both sides filled (whitespace ignored).
fn shortcut_is_complete(sc: &Shortcut) -> bool {
    !sc.trigger.trim().is_empty() && !sc.expansion.trim().is_empty()
}

/// Persist complete rows only. Incomplete drafts stay in memory for the Settings
/// UI so the user can finish typing; they never hit disk or the live engine.
fn commit_shortcuts(s: &mut Shell) {
    let complete: Vec<Shortcut> = s
        .settings
        .shortcuts
        .iter()
        .filter(|sc| shortcut_is_complete(sc))
        .cloned()
        .collect();
    push_shortcuts(&mut s.engine, &complete);
    let drafts = std::mem::replace(&mut s.settings.shortcuts, complete);
    s.save();
    s.settings.shortcuts = drafts;
}

/// "Thêm" is allowed when the list is empty or the last row is already complete.
pub fn can_add_shortcut() -> bool {
    with(|s| {
        s.settings
            .shortcuts
            .last()
            .map_or(true, shortcut_is_complete)
    })
}

pub fn add_shortcut() {
    with(|s| {
        if let Some(last) = s.settings.shortcuts.last() {
            if !shortcut_is_complete(last) {
                return;
            }
        }
        s.settings.shortcuts.push(Shortcut {
            trigger: String::new(),
            expansion: String::new(),
        });
        commit_shortcuts(s);
    });
}

/// Drop blank / half-filled drafts (e.g. when leaving the Gõ tắt page).
pub fn prune_incomplete_shortcuts() {
    with(|s| {
        let before = s.settings.shortcuts.len();
        s.settings.shortcuts.retain(shortcut_is_complete);
        if s.settings.shortcuts.len() != before {
            commit_shortcuts(s);
        }
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
        s.save();
        Some(target)
    })
}

// --- composition driving (called from the hook) ----------------------------

/// Feed one character to the engine, tagged with its physical [`KeySource`] so a
/// numpad digit stays a literal number instead of acting as a VNI modifier.
///
/// The two `tail` calls bracket the engine: the shadow needs the composition as it
/// stood *before* the key (a word boundary wipes it, and that is exactly the text
/// that just became committed) and the engine's verdict *after*.
pub fn process_key(c: char, source: KeySource) -> ImeResult {
    with(|s| {
        s.tail.before_key(s.engine.buffer());
        let result = s.engine.process_key(c, source);
        s.tail.after_key(c, &result, s.engine.buffer());
        result
    })
}

/// Flip the word being composed VN↔raw; returns the delete+inject to apply.
pub fn flip_composing() -> ImeResult {
    with(|s| s.engine.flip_composing())
}

/// Backspace while Vietnamese mode is on. The physical key always passes through, so
/// the app deletes its own visible char (like `funput-term`); this only keeps Funput
/// in step with it.
///
/// Mid-word that means shortening the composition. With nothing composing, the
/// character about to disappear is a committed one, and when its removal leaves the
/// caret at the end of a finished Vietnamese word the engine re-opens that word — so
/// `phủ` + Space + Backspace + `s` gives `phú` instead of `phủs`. The engine refuses
/// anything that is not a complete syllable, which keeps English words and URLs
/// literal; a refusal costs the shadow its bearings, so it starts over.
pub fn on_backspace() {
    with(|s| {
        if !s.engine.buffer().is_empty() {
            s.engine.on_backspace();
            return;
        }
        let Some(word) = s.tail.backspace() else {
            return;
        };
        let adopted = s.engine.adopt(word);
        s.tail.resolve(adopted);
    });
}

pub fn clear() {
    with(reset_composition);
}
