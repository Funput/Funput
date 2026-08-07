//! Pure predicates deciding whether a key event matches a configured hotkey.
//! No process state, so every rule here is unit-testable without the hook.

use funput_desktop::Mods;
use windows::Win32::UI::Input::KeyboardAndMouse::{
    VIRTUAL_KEY, VK_CONTROL, VK_LCONTROL, VK_LMENU, VK_LSHIFT, VK_LWIN, VK_MENU, VK_OEM_3,
    VK_RCONTROL, VK_RMENU, VK_RSHIFT, VK_RWIN, VK_SHIFT, VK_SPACE,
};

use funput_config::{FlipHotkey, Hotkey, KeyCombo};

/// Fold side-specific modifier virtual-keys back to their generic codes. The
/// low-level hook always delivers the side-specific keys (`VK_LSHIFT`,
/// `VK_RMENU`, …), never the generic `VK_SHIFT`/`VK_MENU`/`VK_CONTROL` an app
/// sees after message translation — comparing against the generics without this
/// fold can never match (the original Alt+Shift preset bug).
fn normalize_vk(vk: VIRTUAL_KEY) -> VIRTUAL_KEY {
    match vk {
        VK_LSHIFT | VK_RSHIFT => VK_SHIFT,
        VK_LCONTROL | VK_RCONTROL => VK_CONTROL,
        VK_LMENU | VK_RMENU => VK_MENU,
        VK_RWIN => VK_LWIN,
        other => other,
    }
}

/// Whether the key is a modifier, i.e. one that can only ever be part of a
/// gesture resolved on release.
pub fn is_modifier(vk: VIRTUAL_KEY) -> bool {
    matches!(normalize_vk(vk), VK_CONTROL | VK_MENU | VK_SHIFT | VK_LWIN)
}

/// `mods` with this event's own key forced to `down`. `GetAsyncKeyState` lags
/// the low-level hook by an event, so the key being delivered right now still
/// reads as up on its keydown (and as down on its keyup).
pub fn mods_with(vk: VIRTUAL_KEY, mods: Mods, down: bool) -> Mods {
    let mut mods = mods;
    match normalize_vk(vk) {
        VK_CONTROL => mods.ctrl = down,
        VK_MENU => mods.alt = down,
        VK_SHIFT => mods.shift = down,
        VK_LWIN => mods.win = down,
        _ => {}
    }
    mods
}

/// Whether this keydown matches the configured VI/EN toggle preset. Presets
/// match their exact modifier set, so nearby combos (e.g. Ctrl+Shift+Space)
/// still reach the focused app.
pub fn is_toggle(vk: VIRTUAL_KEY, mods: Mods, hotkey: Hotkey) -> bool {
    let vk = normalize_vk(vk);
    match hotkey {
        Hotkey::CtrlBacktick => {
            mods.ctrl && !mods.alt && !mods.win && !mods.shift && vk == VK_OEM_3
        }
        Hotkey::CtrlSpace => mods.ctrl && !mods.alt && !mods.win && !mods.shift && vk == VK_SPACE,
        // Modifier-only: never on keydown, or Alt+Shift+Tab would toggle before
        // Tab ever arrived. Resolved by `preset_mods` + the release watcher.
        Hotkey::AltShift => false,
    }
}

/// Whether this keydown matches a user-recorded combo: the main key plus the
/// *exact* modifier set (the modifier state is settled by the time it fires).
pub fn matches_combo(vk: VIRTUAL_KEY, mods: Mods, combo: &KeyCombo) -> bool {
    !combo.is_modifier_only()
        && normalize_vk(vk).0 == combo.vk
        && mods.ctrl == combo.ctrl
        && mods.alt == combo.alt
        && mods.shift == combo.shift
        && mods.win == combo.win
}

/// Whether this keydown matches the configured flip hotkey. Letter virtual-keys are
/// their ASCII uppercase codes (`Z` = 0x5A, `X` = 0x58).
pub fn is_flip(vk: VIRTUAL_KEY, mods: Mods, hotkey: FlipHotkey) -> bool {
    let vk = normalize_vk(vk);
    let ctrl_shift = mods.ctrl && mods.shift && !mods.alt && !mods.win;
    match hotkey {
        FlipHotkey::Off => false,
        FlipHotkey::CtrlShiftZ => ctrl_shift && vk.0 == 0x5A,
        FlipHotkey::CtrlShiftX => ctrl_shift && vk.0 == 0x58,
    }
}

/// The modifier set of a preset that has no main key, or `None` when the preset
/// fires on a real key.
pub fn preset_mods(hotkey: Hotkey) -> Option<Mods> {
    match hotkey {
        Hotkey::AltShift => Some(Mods {
            alt: true,
            shift: true,
            ..Mods::default()
        }),
        Hotkey::CtrlBacktick | Hotkey::CtrlSpace => None,
    }
}

/// The modifier set of a recorded combo that has no main key.
pub fn combo_mods(combo: &KeyCombo) -> Option<Mods> {
    combo.is_modifier_only().then_some(Mods {
        ctrl: combo.ctrl,
        alt: combo.alt,
        win: combo.win,
        shift: combo.shift,
    })
}

#[cfg(test)]
mod tests;
