//! These exercise rules that were unreachable while the state lived inside a
//! process-global mutex: which app gets Vietnamese, which surface is allowed to
//! pin one, and when composition is thrown away.

use funput_config::{Method, Settings};
use funput_engine::{Action, KeySource};

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

/// The flyout is a global surface — it has no app in front of it, so it pins none.
/// It used to bind to whatever the user clicked into next, which pinned an app they
/// had never mentioned.
#[test]
fn a_flyout_toggle_does_not_pin_the_next_app() {
    let mut state = shell();

    state.set_enabled(false); // flyout turned Vietnamese off
    // code.exe has no opinion, so it simply inherits the new default…
    assert_eq!(state.apply_for_app("code.exe"), None);
    assert!(!state.enabled());
    // …and nothing was written down about it.
    assert!(state.settings().app_language_memory.is_empty());
}

/// The trade this rule makes, stated as a test rather than left as a surprise: a
/// global switch does not un-pin an app the user pinned on purpose.
#[test]
fn a_pinned_app_keeps_its_pin_when_the_global_switch_moves() {
    let mut state = shell_remembering(&[("notepad.exe", true)]);

    state.set_enabled(false); // flyout turned Vietnamese off
    assert_eq!(
        state.apply_for_app("notepad.exe"),
        Some(true),
        "the pin wins"
    );
    assert!(state.enabled());
    assert_eq!(
        state.settings().app_language_memory.get("notepad.exe"),
        Some(&true),
        "and it is still the only entry"
    );
    assert_eq!(state.settings().app_language_memory.len(), 1);
}

/// The whole rule in one place: the hotkey is the only writer.
#[test]
fn only_the_hotkey_writes_the_memory() {
    let mut state = shell();

    state.set_enabled(false);
    state.apply_for_app("code.exe");
    state.set_enabled(true);
    state.apply_for_app("notepad.exe");
    assert!(state.settings().app_language_memory.is_empty());

    state.note_foreground("code.exe".into());
    state.toggle_enabled_hotkey();
    assert_eq!(
        state.settings().app_language_memory.get("code.exe"),
        Some(&false)
    );
    assert_eq!(state.settings().app_language_memory.len(), 1);
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

/// The Settings window and the Control Center are their own processes, so their
/// flip reaches the hook process as a settings file. It is a global default and
/// arrives as one — no app is pinned on the way in.
#[test]
fn a_flip_written_by_another_process_changes_the_global_state_without_pinning() {
    let dir = std::env::temp_dir().join(format!("funput-reload-{}", std::process::id()));
    std::fs::create_dir_all(&dir).unwrap();
    let path = dir.join("settings.json");

    let mut background = ShellState::new(Some(path.clone()));
    // Stand in for the child process: flip VI off and persist.
    let mut child = ShellState::new(Some(path.clone()));
    child.set_enabled(false);

    assert!(background.reload_settings());
    assert!(!background.enabled(), "the flip reached this process");
    assert_eq!(background.apply_for_app("code.exe"), None, "already off");
    assert!(
        background.settings().app_language_memory.is_empty(),
        "and pinned nothing on the way"
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
    state.toggle_enabled_hotkey(); // code.exe → English, in memory…
    state.save_settings(); // …and on disk, the step the hook defers

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

    // A window that resolved to no executable must not claim an entry, from either
    // direction: nothing to replay for it, and a hotkey pressed in it writes nothing.
    assert_eq!(state.apply_for_app(""), None);
    state.toggle_enabled_hotkey();
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

/// Type `keys` through the shell and rebuild what the app would be showing, the
/// same way the hook applies an [`ImeResult`]: pass the key through, or eat the
/// backspaces and inject.
fn app_text(state: &mut ShellState, keys: &str) -> String {
    let mut app = String::new();
    for key in keys.chars() {
        let result = state.process_key(key, KeySource::Standard);
        match result.action {
            Action::None => app.push(key),
            Action::Send => {
                for _ in 0..result.backspace {
                    app.pop();
                }
                app.push_str(&result.output);
            }
            Action::Restore => unreachable!("Restore not implemented yet"),
        }
    }
    app
}

/// A shell holding one finished row, `tp` → `TP. HCM`.
fn shell_with_tp() -> ShellState {
    let mut state = shell();
    state.add_shortcut();
    state.set_shortcut_trigger(0, "tp".into());
    state.set_shortcut_expansion(0, "TP. HCM".into());
    state
}

#[test]
fn smart_case_is_on_by_default() {
    let mut state = shell_with_tp();
    assert_eq!(app_text(&mut state, "Tp "), "Tp. Hcm ");
}

/// The Settings switch has to reach the engine, not just the settings file — on
/// this shell that is `update_config` re-syncing the whole `EngineConfig`, with no
/// separate setter to forget.
#[test]
fn turning_smart_case_off_reaches_the_engine() {
    let mut state = shell_with_tp();
    state.set_shortcut_smart_case(false);

    assert_eq!(
        app_text(&mut state, "Tp "),
        "Tp ",
        "a differently cased trigger must be left alone"
    );
    assert_eq!(
        app_text(&mut state, "tp "),
        "TP. HCM ",
        "the row itself is untouched — the exact trigger still expands"
    );
}

/// Flipping it back must take effect without re-pushing the table.
#[test]
fn turning_smart_case_back_on_restores_matching() {
    let mut state = shell_with_tp();
    state.set_shortcut_smart_case(false);
    state.set_shortcut_smart_case(true);

    assert_eq!(app_text(&mut state, "Tp "), "Tp. Hcm ");
}

// --- keyboard layout -------------------------------------------------------

/// Real Windows layout handles. `JAPANESE_IME` is what Microsoft IME reports;
/// `US` and `VIETNAMESE` are plain keyboard layouts.
const US: u32 = 0x0409_0409;
const VIETNAMESE: u32 = 0x042A_042A;
const JAPANESE_IME: u32 = 0xE020_0411;
const RUSSIAN: u32 = 0x0419_0419;

#[test]
fn a_foreign_layout_suspends_vietnamese_and_leaving_it_gives_it_back() {
    let mut state = shell();
    assert_eq!(state.apply_for_layout(US), None); // nothing to do, already typing

    assert_eq!(state.apply_for_layout(JAPANESE_IME), Some(false));
    assert!(!state.enabled());
    assert_eq!(state.apply_for_layout(US), Some(true));
    assert!(state.enabled());
}

#[test]
fn the_same_layout_twice_reports_no_change() {
    let mut state = shell();
    assert_eq!(state.apply_for_layout(RUSSIAN), Some(false));
    assert_eq!(state.apply_for_layout(RUSSIAN), None);
}

/// The suspension is about the keyboard, not about what the user asked for. Their
/// setting has to survive it untouched, or returning to a Latin layout would come
/// back to English and the choice would have been quietly eaten.
#[test]
fn a_suspension_never_writes_to_the_setting() {
    let mut state = shell();
    state.apply_for_layout(JAPANESE_IME);

    assert!(!state.enabled(), "not running");
    assert!(
        state.settings().enabled,
        "but still what the user asked for"
    );
}

#[test]
fn the_vietnamese_layout_is_not_a_foreign_one() {
    let mut state = shell();
    assert_eq!(state.apply_for_layout(VIETNAMESE), None);
    assert!(state.enabled());
}

#[test]
fn nothing_is_suspended_while_the_user_is_already_in_english() {
    let mut state = shell_remembering(&[("code.exe", false)]);
    assert_eq!(state.apply_for_app("code.exe"), Some(false));

    // Already off: the layout has nothing left to take away, and the tray must not
    // be told about a change that did not happen.
    assert_eq!(state.apply_for_layout(JAPANESE_IME), None);
}

/// The platform applies the layout rule after the per-app one, so a layout veto is
/// the last word: an app remembered as Vietnamese does not switch it back on while
/// a Japanese IME is loaded.
#[test]
fn a_layout_veto_outranks_a_remembered_app() {
    let mut state = shell_remembering(&[("code.exe", true)]);
    state.apply_for_layout(JAPANESE_IME);

    state.note_foreground("code.exe".to_string());
    assert_eq!(state.apply_for_app("code.exe"), None);
    assert_eq!(state.apply_for_layout(JAPANESE_IME), None);
    assert!(!state.enabled());
}

#[test]
fn the_hotkey_overrules_the_layout_until_the_user_moves_to_another_one() {
    let mut state = shell();
    assert_eq!(state.apply_for_layout(JAPANESE_IME), Some(false));

    // The hotkey is measured against what is running, so this turns it back on
    // rather than toggling the setting that already said "on".
    assert!(state.toggle_enabled_hotkey());
    assert!(state.enabled());
    assert_eq!(
        state.apply_for_layout(JAPANESE_IME),
        None,
        "no second opinion"
    );

    // The override was for that keyboard only.
    assert_eq!(state.apply_for_layout(US), None);
    assert_eq!(state.apply_for_layout(JAPANESE_IME), Some(false));
}

#[test]
fn the_switch_being_off_leaves_every_layout_alone() {
    let mut state = shell_with(Settings {
        auto_english_on_foreign_layout: false,
        ..Settings::default()
    });
    assert_eq!(state.apply_for_layout(JAPANESE_IME), None);
    assert!(state.enabled());
}

/// Turning the switch off in Settings has to give Vietnamese back there and then —
/// waiting for the user to change keyboards would look like the switch did nothing.
#[test]
fn turning_the_switch_off_lifts_a_suspension_already_in_place() {
    let mut state = shell();
    assert_eq!(state.apply_for_layout(JAPANESE_IME), Some(false));

    state.set_auto_english_on_foreign_layout(false);
    assert!(state.enabled());
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
    state.set_shortcut_smart_case(false);

    let reopened = ShellState::new(Some(path));
    assert_eq!(reopened.settings().method, Method::Vni);
    assert!(reopened.settings().spell_check);
    assert!(!reopened.settings().shortcut_smart_case);
    let _ = std::fs::remove_dir_all(dir);
}

#[test]
fn without_a_settings_file_everything_still_works_in_memory() {
    let mut state = ShellState::new(None);
    assert_eq!(state.tone_style(), funput_core::ToneStyle::Modern);
    state.set_spell_check(true);
    assert!(state.settings().spell_check);
}

// ---- gõ tắt in English mode ----

/// English mode is the user's choice here, not a layout veto — the shell must still
/// let the hook see keys so a trigger can expand.
#[test]
fn english_mode_keeps_the_hook_active_for_shortcuts() {
    let mut state = shell_with_tp();
    state.set_enabled(false);

    assert!(!state.enabled(), "Vietnamese really is off");
    assert!(state.hook_active(), "but gõ tắt still has work to do");
    assert_eq!(app_text(&mut state, "tp "), "TP. HCM ");
}

#[test]
fn turning_the_english_switch_off_hands_english_mode_back() {
    let mut state = shell_with_tp();
    state.set_enabled(false);
    state.set_shortcuts_in_english(false);

    assert!(!state.hook_active());
    assert_eq!(app_text(&mut state, "tp "), "tp ");
}

/// No rows means nothing to expand, so English mode stays exactly as hands-off as
/// it was before the feature existed.
#[test]
fn an_empty_table_leaves_english_mode_out_of_the_hook() {
    let mut state = shell();
    state.set_enabled(false);

    assert!(!state.hook_active());
}

/// A suspended layout vetoes gõ tắt as well: an expansion injected into a CJK IME
/// corrupts its text exactly as composition would.
#[test]
fn a_suspended_layout_vetoes_english_shortcuts_too() {
    let mut state = shell_with_tp();
    state.apply_for_layout(JAPANESE_IME);

    assert!(!state.enabled());
    assert!(
        !state.hook_active(),
        "the layout suspension outranks the English-mode switch"
    );
}

/// Turning gõ tắt off entirely takes English mode with it — one feature, one switch.
#[test]
fn the_shortcuts_switch_also_closes_the_hook_in_english_mode() {
    let mut state = shell_with_tp();
    state.set_enabled(false);
    state.set_shortcuts_enabled(false);

    assert!(!state.hook_active());
}

/// Vietnamese mode never depends on the English-mode switch.
#[test]
fn the_english_switch_does_not_touch_vietnamese_mode() {
    let mut state = shell_with_tp();
    state.set_shortcuts_in_english(false);

    assert!(state.hook_active());
    assert_eq!(app_text(&mut state, "tp "), "TP. HCM ");
}
