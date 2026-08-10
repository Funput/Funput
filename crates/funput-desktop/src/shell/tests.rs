//! These exercise rules that were unreachable while the state lived inside a
//! process-global mutex: which app gets Vietnamese, what a toggle made from the
//! tray binds to, and when composition is thrown away.

use funput_config::{Method, Settings};
use funput_engine::KeySource;

use super::*;

/// In memory only — no settings file, so nothing touches the disk.
fn shell() -> ShellState {
    ShellState::new(None)
}

fn shell_with(settings: Settings) -> ShellState {
    let mut state = shell();
    state.replace_settings(settings);
    state
}

/// A shell that already remembers `(id, is_vietnamese)` for each pair.
fn shell_remembering(entries: &[(&str, bool)]) -> ShellState {
    let mut settings = Settings::default();
    for (id, on) in entries {
        settings.app_language_memory.insert((*id).to_string(), *on);
    }
    shell_with(settings)
}

// --- per-app VI/EN ---------------------------------------------------------

#[test]
fn an_app_with_no_remembered_choice_leaves_the_toggle_alone() {
    // Funput only replays decisions the user made; it never guesses one.
    let mut state = shell();
    assert_eq!(state.apply_for_app("code.exe"), None);
    assert!(state.enabled());
}

#[test]
fn a_remembered_app_switches_to_english_and_an_unseen_one_does_not_switch_back() {
    let mut state = shell_remembering(&[("code.exe", false)]);

    assert_eq!(state.apply_for_app("code.exe"), Some(false));
    assert!(!state.enabled());
    // notepad.exe has no remembered choice, so it inherits the current state
    // rather than being forced back to Vietnamese.
    assert_eq!(state.apply_for_app("notepad.exe"), None);
    assert!(!state.enabled());
}

#[test]
fn focusing_the_same_app_twice_reports_no_change() {
    let mut state = shell_remembering(&[("code.exe", false)]);
    assert_eq!(state.apply_for_app("code.exe"), Some(false));
    assert_eq!(state.apply_for_app("code.exe"), None);
}

/// The flyout steals foreground, so the toggle cannot bind to the app it was
/// meant for until the user goes back to it.
#[test]
fn a_flyout_toggle_binds_to_the_next_app_focused() {
    let mut state = shell_remembering(&[("notepad.exe", true)]);

    state.set_enabled(false); // flyout turned Vietnamese off
    // notepad.exe is remembered as Vietnamese, which would normally switch it back
    // on the moment focus lands there; the parked choice has to win instead.
    assert_eq!(state.apply_for_app("notepad.exe"), None);
    assert!(!state.enabled());
    // …and it replaces what notepad.exe used to remember, from now on.
    state.apply_for_app("code.exe");
    assert_eq!(state.apply_for_app("notepad.exe"), None);
    assert!(!state.enabled());
}

/// The hotkey fires while the target app is focused, so it binds immediately.
#[test]
fn a_hotkey_toggle_binds_to_the_focused_app() {
    let mut state = shell();
    state.note_foreground("code.exe".into());

    assert!(!state.toggle_enabled_hotkey(), "hotkey turned it off");
    // An app with its own remembered choice still switches…
    state.remember("notepad.exe", true);
    assert_eq!(state.apply_for_app("notepad.exe"), Some(true));
    // …and returning to code.exe honours the manual choice made there.
    assert_eq!(state.apply_for_app("code.exe"), Some(false), "remembered");
}

#[test]
fn a_hotkey_toggle_clears_a_parked_flyout_toggle() {
    let mut state = shell();
    state.set_enabled(false); // flyout: parks "off"
    state.note_foreground("code.exe".into());
    state.toggle_enabled_hotkey(); // back on, and binds to code.exe

    // The parked choice is gone, so a fresh app has nothing to bind and nothing
    // remembered — it is left alone.
    assert_eq!(state.apply_for_app("notepad.exe"), None);
    assert!(state.enabled());
    assert!(
        !state
            .settings()
            .app_language_memory
            .contains_key("notepad.exe")
    );
}

/// A parked choice that happens to match the current state still has to be
/// written down, or it is lost the moment focus moves on.
#[test]
fn a_parked_choice_is_remembered_even_when_the_state_does_not_flip() {
    let mut state = shell();
    state.set_enabled(true); // Settings window: parks "on", state already on
    assert_eq!(state.apply_for_app("code.exe"), None, "nothing to flip");
    assert_eq!(
        state.settings().app_language_memory.get("code.exe"),
        Some(&true)
    );
}

/// The Settings window and the Control Center are their own processes, so the
/// choice they parked died with them. Picking their file up has to re-park it.
#[test]
fn a_flip_written_by_another_process_binds_to_the_next_app() {
    let dir = std::env::temp_dir().join(format!("funput-reload-{}", std::process::id()));
    std::fs::create_dir_all(&dir).unwrap();
    let path = dir.join("settings.json");

    let mut background = ShellState::new(Some(path.clone()));
    // Stand in for the child process: flip VI off and persist.
    let mut child = ShellState::new(Some(path.clone()));
    child.set_enabled(false);

    assert!(background.reload_settings());
    assert_eq!(background.apply_for_app("code.exe"), None, "already off");
    assert_eq!(
        background.settings().app_language_memory.get("code.exe"),
        Some(&false),
        "the flyout's choice bound to the app the user returned to"
    );
    let _ = std::fs::remove_dir_all(dir);
}

#[test]
fn a_remembered_choice_survives_a_restart() {
    let dir = std::env::temp_dir().join(format!("funput-memory-{}", std::process::id()));
    std::fs::create_dir_all(&dir).unwrap();
    let path = dir.join("settings.json");

    let mut state = ShellState::new(Some(path.clone()));
    state.note_foreground("code.exe".into());
    state.toggle_enabled_hotkey(); // code.exe → English

    let mut reopened = ShellState::new(Some(path));
    assert_eq!(
        reopened.settings().app_language_memory.get("code.exe"),
        Some(&false)
    );
    // The global toggle reloads as English too, so drive it back on through an
    // app remembered as Vietnamese and check code.exe still pulls it down.
    reopened.remember("notepad.exe", true);
    assert_eq!(reopened.apply_for_app("notepad.exe"), Some(true));
    assert_eq!(reopened.apply_for_app("code.exe"), Some(false), "replayed");
    let _ = std::fs::remove_dir_all(dir);
}

#[test]
fn an_empty_app_id_is_ignored() {
    let mut state = shell();
    state.note_foreground(String::new());
    assert_eq!(state.foreground_id(), None);

    state.set_enabled(false); // parks a choice with nowhere to land
    assert_eq!(state.apply_for_app(""), None);
    assert!(state.settings().app_language_memory.is_empty());
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
