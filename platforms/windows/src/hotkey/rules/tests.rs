use super::*;
use crate::settings::NO_KEY;
use windows::Win32::UI::Input::KeyboardAndMouse::VK_TAB;

fn m(ctrl: bool, alt: bool, shift: bool, win: bool) -> Mods {
    Mods {
        ctrl,
        alt,
        win,
        shift,
    }
}

fn combo(vk: u16, ctrl: bool, alt: bool, shift: bool) -> KeyCombo {
    KeyCombo {
        vk,
        ctrl,
        alt,
        shift,
        win: false,
        label: if vk == NO_KEY {
            String::new()
        } else {
            "V".into()
        },
    }
}

#[test]
fn alt_shift_never_fires_on_keydown() {
    // It is a release gesture now, so Alt+Shift+Tab reaches the focused app.
    for vk in [VK_LSHIFT, VK_RSHIFT, VK_LMENU, VK_RMENU, VK_TAB] {
        assert!(!is_toggle(
            vk,
            m(false, true, true, false),
            Hotkey::AltShift
        ));
    }
    assert_eq!(
        preset_mods(Hotkey::AltShift),
        Some(m(false, true, true, false))
    );
    assert_eq!(preset_mods(Hotkey::CtrlSpace), None);
}

#[test]
fn ctrl_presets_match_exact_modifiers_only() {
    assert!(is_toggle(
        VK_SPACE,
        m(true, false, false, false),
        Hotkey::CtrlSpace
    ));
    assert!(is_toggle(
        VK_OEM_3,
        m(true, false, false, false),
        Hotkey::CtrlBacktick
    ));
    // Ctrl+Shift+Space / Ctrl+Alt+Space belong to the focused app.
    assert!(!is_toggle(
        VK_SPACE,
        m(true, false, true, false),
        Hotkey::CtrlSpace
    ));
    assert!(!is_toggle(
        VK_SPACE,
        m(true, true, false, false),
        Hotkey::CtrlSpace
    ));
    // The other preset's key does not toggle.
    assert!(!is_toggle(
        VK_SPACE,
        m(true, false, false, false),
        Hotkey::CtrlBacktick
    ));
}

#[test]
fn combo_requires_exact_modifier_set() {
    let v = combo(0x56, true, false, true);
    assert!(matches_combo(
        VIRTUAL_KEY(0x56),
        m(true, false, true, false),
        &v
    ));
    // Missing or extra modifiers → no match.
    assert!(!matches_combo(
        VIRTUAL_KEY(0x56),
        m(true, false, false, false),
        &v
    ));
    assert!(!matches_combo(
        VIRTUAL_KEY(0x56),
        m(true, true, true, false),
        &v
    ));
    // Different main key → no match.
    assert!(!matches_combo(
        VIRTUAL_KEY(0x58),
        m(true, false, true, false),
        &v
    ));
    assert_eq!(combo_mods(&v), None);
}

#[test]
fn modifier_only_combo_is_invisible_to_keydown_matching() {
    let pair = combo(NO_KEY, true, false, true);
    for vk in [VK_LCONTROL, VK_LSHIFT, VIRTUAL_KEY(0x56)] {
        assert!(!matches_combo(vk, m(true, false, true, false), &pair));
    }
    assert_eq!(combo_mods(&pair), Some(m(true, false, true, false)));
}

#[test]
fn side_specific_modifiers_fold_and_settle_their_own_state() {
    for vk in [
        VK_LSHIFT,
        VK_RSHIFT,
        VK_LCONTROL,
        VK_LMENU,
        VK_RMENU,
        VK_RWIN,
    ] {
        assert!(is_modifier(vk));
    }
    assert!(!is_modifier(VK_SPACE));
    // The async state has not caught up with the key this event delivers.
    assert_eq!(
        mods_with(VK_RSHIFT, m(false, true, false, false), true),
        m(false, true, true, false)
    );
    assert_eq!(
        mods_with(VK_LMENU, m(false, true, true, false), false),
        m(false, false, true, false)
    );
    // A non-modifier leaves the set untouched.
    assert_eq!(
        mods_with(VK_SPACE, m(true, false, false, false), true),
        m(true, false, false, false)
    );
}

#[test]
fn flip_matches_ctrl_shift_letter() {
    let ctrl_shift = m(true, false, true, false);
    assert!(is_flip(
        VIRTUAL_KEY(0x5A),
        ctrl_shift,
        FlipHotkey::CtrlShiftZ
    ));
    assert!(is_flip(
        VIRTUAL_KEY(0x58),
        ctrl_shift,
        FlipHotkey::CtrlShiftX
    ));
    assert!(!is_flip(VIRTUAL_KEY(0x5A), ctrl_shift, FlipHotkey::Off));
    // Extra Alt held → not the flip.
    assert!(!is_flip(
        VIRTUAL_KEY(0x5A),
        m(true, true, true, false),
        FlipHotkey::CtrlShiftZ
    ));
}
