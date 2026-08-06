//! These exercise rules that were unreachable while the state lived inside a
//! process-global mutex: which app gets Vietnamese, what a toggle made from the
//! tray binds to, and when composition is thrown away.

use funput_config::{ExcludedApp, Method, Settings};
use funput_engine::KeySource;

use super::*;

fn app(id: &str) -> ExcludedApp {
    ExcludedApp {
        id: id.to_string(),
        name: id.trim_end_matches(".exe").to_string(),
    }
}

/// In memory only — no settings file, so nothing touches the disk.
fn shell() -> ShellState {
    ShellState::new(None)
}

fn shell_with(settings: Settings) -> ShellState {
    let mut state = shell();
    state.replace_settings(settings);
    state
}

// --- per-app VI/EN ---------------------------------------------------------

#[test]
fn an_empty_exclusion_list_leaves_the_global_toggle_alone() {
    // Users who don't use the feature must keep a plain on/off switch.
    let mut state = shell();
    assert_eq!(state.apply_for_app("code.exe"), None);
    assert!(state.enabled());
}

#[test]
fn an_excluded_app_switches_to_english_and_back() {
    let mut state = shell_with(Settings {
        excluded_apps: vec![app("code.exe")],
        ..Settings::default()
    });

    assert_eq!(state.apply_for_app("code.exe"), Some(false));
    assert!(!state.enabled());
    assert_eq!(state.apply_for_app("notepad.exe"), Some(true));
    assert!(state.enabled());
}

#[test]
fn focusing_the_same_app_twice_reports_no_change() {
    let mut state = shell_with(Settings {
        excluded_apps: vec![app("code.exe")],
        ..Settings::default()
    });
    assert_eq!(state.apply_for_app("code.exe"), Some(false));
    assert_eq!(state.apply_for_app("code.exe"), None);
}

/// The tray steals foreground, so the toggle cannot bind to the app it was meant
/// for until the user goes back to it.
#[test]
fn a_tray_toggle_binds_to_the_next_app_focused() {
    let mut state = shell_with(Settings {
        excluded_apps: vec![app("code.exe")],
        ..Settings::default()
    });

    assert!(!state.toggle_enabled(), "tray turned Vietnamese off");
    // Returning to a *non-excluded* app would normally switch Vietnamese back on;
    // the parked choice has to win instead.
    assert_eq!(state.apply_for_app("notepad.exe"), None);
    assert!(!state.enabled());
    // …and it is remembered for that app from now on.
    state.apply_for_app("code.exe");
    assert_eq!(state.apply_for_app("notepad.exe"), None);
    assert!(!state.enabled());
}

/// The hotkey fires while the target app is focused, so it binds immediately.
#[test]
fn a_hotkey_toggle_binds_to_the_focused_app() {
    // The list has to be non-empty for the auto-switch to run at all — otherwise
    // rule 3 no-ops and a remembered override has nothing to override.
    let mut state = shell_with(Settings {
        excluded_apps: vec![app("chrome.exe")],
        ..Settings::default()
    });
    state.note_foreground("code.exe".into(), "code".into());

    // code.exe is not excluded, so it runs Vietnamese until the hotkey turns it off.
    assert!(!state.toggle_enabled_hotkey());
    // Another non-excluded app switches Vietnamese back on…
    assert_eq!(state.apply_for_app("notepad.exe"), Some(true));
    // …and returning to code.exe honours the manual choice made there.
    assert_eq!(state.apply_for_app("code.exe"), Some(false), "remembered");
}

#[test]
fn a_hotkey_toggle_clears_a_parked_tray_toggle() {
    let mut state = shell();
    state.toggle_enabled(); // tray: parks "off"
    state.note_foreground("code.exe".into(), "code".into());
    state.toggle_enabled_hotkey(); // back on, and binds to code.exe

    // The parked choice is gone, so a fresh app follows the list default (none).
    assert_eq!(state.apply_for_app("notepad.exe"), None);
    assert!(state.enabled());
}

// --- recent apps -----------------------------------------------------------

#[test]
fn recent_apps_are_deduped_most_recent_first_and_capped() {
    let mut state = shell();
    for i in 0..RECENT_CAP + 5 {
        state.note_foreground(format!("app{i}.exe"), format!("app{i}"));
    }
    assert_eq!(state.recent_apps().len(), RECENT_CAP);
    assert_eq!(state.foreground_id(), Some("app16.exe"));

    state.note_foreground("app16.exe".into(), "app16".into());
    assert_eq!(
        state.recent_apps().len(),
        RECENT_CAP,
        "deduped, not appended"
    );
}

#[test]
fn an_empty_app_id_is_ignored() {
    let mut state = shell();
    state.note_foreground(String::new(), "ghost".into());
    assert!(state.recent_apps().is_empty());
    assert_eq!(state.foreground_id(), None);
}

// --- composition lifecycle -------------------------------------------------

#[test]
fn switching_to_english_throws_away_the_composition() {
    let mut state = shell();
    state.process_key('c', KeySource::Standard);
    assert!(state.is_composing());

    state.set_enabled(false);
    assert!(!state.is_composing());
}

/// Turning Vietnamese back *on* must drop the shadow too: English-mode keystrokes
/// never reached the engine, so it no longer describes the text at the caret.
#[test]
fn switching_back_to_vietnamese_also_drops_the_shadow() {
    let mut state = shell();
    state.process_key('p', KeySource::Standard);
    state.process_key('h', KeySource::Standard);
    state.process_key('u', KeySource::Standard);
    state.process_key('r', KeySource::Standard);
    state.process_key(' ', KeySource::Standard);

    state.set_enabled(false);
    state.set_enabled(true);

    state.on_backspace();
    assert!(
        !state.is_composing(),
        "no word may be re-opened after EN mode"
    );
}

#[test]
fn changing_the_input_method_throws_away_the_composition() {
    let mut state = shell();
    state.process_key('c', KeySource::Standard);
    state.set_method(InputMethod::Vni);
    assert!(!state.is_composing());
}

/// The whole retone feature, driven through the state the hook actually holds.
#[test]
fn backspace_after_a_word_boundary_reopens_the_word() {
    let mut state = shell();
    state.set_method(InputMethod::Telex);
    for key in "phur ".chars() {
        state.process_key(key, KeySource::Standard);
    }
    assert!(!state.is_composing(), "the space committed the word");

    state.on_backspace();
    assert!(state.is_composing(), "phủ was re-opened");
}

// --- shortcuts -------------------------------------------------------------

#[test]
fn a_half_typed_shortcut_stays_visible_but_is_not_persisted() {
    let mut state = shell();
    state.add_shortcut();
    state.set_shortcut_trigger(0, "vn".into());

    // The draft is still in the list the UI renders…
    assert_eq!(state.shortcuts().len(), 1);
    assert!(state.shortcuts()[0].expansion.is_empty());
    // …and blocks adding another row until it is finished.
    assert!(!state.can_add_shortcut());

    state.set_shortcut_expansion(0, "Việt Nam".into());
    assert!(state.can_add_shortcut());
}

#[test]
fn pruning_drops_drafts_and_keeps_complete_rows() {
    let mut state = shell();
    state.add_shortcut();
    state.set_shortcut_trigger(0, "vn".into());
    state.set_shortcut_expansion(0, "Việt Nam".into());
    state.add_shortcut(); // a blank draft

    state.prune_incomplete_shortcuts();
    assert_eq!(state.shortcuts().len(), 1);
    assert_eq!(state.shortcuts()[0].trigger, "vn");
}

// --- persistence -----------------------------------------------------------

#[test]
fn settings_survive_a_restart() {
    let dir = std::env::temp_dir().join(format!("funput-shell-{}", std::process::id()));
    std::fs::create_dir_all(&dir).unwrap();
    let path = dir.join("settings.json");

    let mut state = ShellState::new(Some(path.clone()));
    state.set_method(InputMethod::Vni);
    state.set_spell_check(true);

    let reopened = ShellState::new(Some(path));
    assert_eq!(reopened.settings().method, Method::Vni);
    assert!(reopened.settings().spell_check);
    let _ = std::fs::remove_dir_all(dir);
}

#[test]
fn without_a_settings_file_everything_still_works_in_memory() {
    let mut state = ShellState::new(None);
    state.set_spell_check(true);
    assert!(state.settings().spell_check);
}
