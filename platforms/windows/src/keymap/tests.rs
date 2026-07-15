use super::*;

fn m(ctrl: bool, alt: bool, shift: bool, win: bool) -> Mods {
    Mods {
        ctrl,
        alt,
        win,
        shift,
    }
}

#[test]
fn alt_shift_matches_the_side_specific_vks_the_hook_delivers() {
    // Alt held, then Shift pressed — the hook reports VK_LSHIFT/VK_RSHIFT, and
    // the async state may not yet include the Shift being pressed.
    for vk in [VK_LSHIFT, VK_RSHIFT] {
        assert!(is_toggle(
            vk,
            m(false, true, false, false),
            Hotkey::AltShift
        ));
    }
    // Shift held, then Alt pressed (either side).
    for vk in [VK_LMENU, VK_RMENU] {
        assert!(is_toggle(
            vk,
            m(false, false, true, false),
            Hotkey::AltShift
        ));
    }
}

#[test]
fn alt_shift_rejects_extra_modifiers_and_other_keys() {
    // Extra Ctrl / Win held → not the toggle.
    assert!(!is_toggle(
        VK_LSHIFT,
        m(true, true, false, false),
        Hotkey::AltShift
    ));
    assert!(!is_toggle(
        VK_LMENU,
        m(false, false, true, true),
        Hotkey::AltShift
    ));
    // A normal key while Alt+Shift are held is not the toggle (it may be an
    // app shortcut like Alt+Shift+Tab).
    assert!(!is_toggle(
        VK_TAB,
        m(false, true, true, false),
        Hotkey::AltShift
    ));
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
