//! Turn a key event recorded in the Settings window into a [`KeyCombo`].
//!
//! The recorder popup captures plain Slint key events (no global hook): the
//! pressed key arrives as text — a printable char or one of the function-key
//! code points in [`slint::platform::Key`]. A printable char is mapped through
//! `VkKeyScanExW` on the user's *current* keyboard layout, so the stored VK is
//! exactly what the low-level hook will see for that physical key. This is
//! what makes custom combos work on layouts where the fixed presets don't
//! (e.g. `Ctrl+\`` / `VK_OEM_3` only exists on US-like layouts).
//!
//! A combo can also be modifiers alone (Ctrl+Shift, Alt+Shift) — the shape most
//! Vietnamese IMEs use for the VI/EN toggle. Those carry no VK and the hook
//! matches them on release; see [`crate::hotkey`].

use slint::platform::Key;
use windows::Win32::UI::Input::KeyboardAndMouse::{GetKeyboardLayout, VkKeyScanExW};

use crate::settings::{KeyCombo, NO_KEY};

/// A successfully recorded combo, plus whether it collides with a well-known
/// Windows system shortcut (the UI shows a warning but does not block — the
/// maintainer's call: warn, never fight the OS silently).
pub struct Recorded {
    pub combo: KeyCombo,
    pub system_conflict: bool,
}

/// Validate + map one recording. An empty `text` means the user released a bare
/// modifier gesture; otherwise it is the main key they pressed. `None` = not
/// recordable, and the recorder shows its hint instead.
pub fn record(text: &str, ctrl: bool, alt: bool, shift: bool, win: bool) -> Option<Recorded> {
    let combo = if text.is_empty() {
        modifier_only(ctrl, alt, shift, win)?
    } else {
        with_main_key(text, ctrl, alt, shift, win)?
    };
    let system_conflict = system_conflict(&combo);
    Some(Recorded {
        combo,
        system_conflict,
    })
}

/// A hotkey of nothing but modifiers (Ctrl+Shift, Alt+Shift…). At least two are
/// required: a lone modifier is pressed constantly during ordinary typing.
fn modifier_only(ctrl: bool, alt: bool, shift: bool, win: bool) -> Option<KeyCombo> {
    if [ctrl, alt, shift, win].iter().filter(|held| **held).count() < 2 {
        return None;
    }
    Some(KeyCombo {
        vk: NO_KEY,
        ctrl,
        alt,
        shift,
        win,
        label: String::new(),
    })
}

/// A main key plus modifiers. Needs Ctrl/Alt/Win — Shift alone is normal typing
/// (same rule as the macOS recorder) — and a key with a VK on this layout.
fn with_main_key(text: &str, ctrl: bool, alt: bool, shift: bool, win: bool) -> Option<KeyCombo> {
    if !(ctrl || alt || win) {
        return None;
    }
    let (vk, label) = map_key(text.chars().next()?)?;
    Some(KeyCombo {
        vk,
        ctrl,
        alt,
        shift,
        win,
        label,
    })
}

fn map_key(ch: char) -> Option<(u16, String)> {
    if let Some((vk, label)) = special_key(ch) {
        return Some((vk, label.to_string()));
    }
    if ch.is_control() {
        return None; // Escape cancels in the UI; other control chars aren't keys
    }
    let vk = vk_for_char(ch)?;
    let label: String = ch.to_uppercase().collect();
    Some((vk, label))
}

/// Slint function-key code points → (VK, label). VK values from winuser.h.
fn special_key(ch: char) -> Option<(u16, &'static str)> {
    let table: &[(char, u16, &str)] = &[
        (' ', 0x20, "Space"),
        (char::from(Key::Tab), 0x09, "Tab"),
        (char::from(Key::Return), 0x0D, "Enter"),
        (char::from(Key::Backspace), 0x08, "Backspace"),
        (char::from(Key::Delete), 0x2E, "Delete"),
        (char::from(Key::Insert), 0x2D, "Insert"),
        (char::from(Key::Home), 0x24, "Home"),
        (char::from(Key::End), 0x23, "End"),
        (char::from(Key::PageUp), 0x21, "PgUp"),
        (char::from(Key::PageDown), 0x22, "PgDn"),
        (char::from(Key::UpArrow), 0x26, "↑"),
        (char::from(Key::DownArrow), 0x28, "↓"),
        (char::from(Key::LeftArrow), 0x25, "←"),
        (char::from(Key::RightArrow), 0x27, "→"),
    ];
    if let Some(&(_, vk, label)) = table.iter().find(|&&(c, ..)| c == ch) {
        return Some((vk, label));
    }
    function_key(ch)
}

/// F1–F24 (Slint assigns them contiguous code points; VK_F1 = 0x70).
fn function_key(ch: char) -> Option<(u16, &'static str)> {
    const LABELS: [&str; 24] = [
        "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12", "F13", "F14",
        "F15", "F16", "F17", "F18", "F19", "F20", "F21", "F22", "F23", "F24",
    ];
    let offset = (ch as u32).checked_sub(char::from(Key::F1) as u32)?;
    let label = LABELS.get(offset as usize)?;
    Some((0x70 + offset as u16, label))
}

/// VK of the key producing `ch` on the user's current layout (low byte of the
/// `VkKeyScanExW` result; -1 means no key maps to this char).
fn vk_for_char(ch: char) -> Option<u16> {
    let unit = u16::try_from(ch as u32).ok()?; // BMP only — keys always are
    let layout = unsafe { GetKeyboardLayout(0) };
    let scan = unsafe { VkKeyScanExW(unit, layout) };
    if scan == -1 {
        return None;
    }
    Some((scan as u16) & 0xFF)
}

/// Combos Windows itself acts on. Recording them still works, but the OS will
/// fight the hotkey (layout switcher, window menu, task switcher), so the UI
/// shows a warning — here at record time, and in `populate()` when Settings
/// reopens with a conflicting combo persisted.
pub fn system_conflict(c: &KeyCombo) -> bool {
    // Win+<anything> is OS-reserved almost without exception (Win+Space is the
    // layout switcher — the exact conflict this feature exists to avoid).
    if c.win {
        return true;
    }
    let alt_only = c.alt && !c.ctrl;
    (c.vk == 0x09 && alt_only) // Alt+Tab / Alt+Shift+Tab task switcher
        || (c.vk == 0x20 && alt_only && !c.shift) // Alt+Space window menu
        || (c.vk == 0x1B && c.ctrl) // Ctrl+Esc opens Start
        || (c.vk == 0x2E && c.ctrl && c.alt) // Ctrl+Alt+Del (unhookable anyway)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn maps_special_and_all_function_keys() {
        assert_eq!(special_key(char::from(Key::Space)), Some((0x20, "Space")));
        assert_eq!(special_key(char::from(Key::LeftArrow)), Some((0x25, "←")));
        assert_eq!(function_key(char::from(Key::F1)), Some((0x70, "F1")));
        assert_eq!(function_key(char::from(Key::F12)), Some((0x7B, "F12")));
        assert_eq!(function_key(char::from(Key::F24)), Some((0x87, "F24")));
    }

    #[test]
    fn rejects_shift_only_but_accepts_a_real_modifier() {
        assert!(record("V", false, false, true, false).is_none());
        let recorded = record(" ", true, false, true, false).unwrap();
        assert_eq!(recorded.combo.vk, 0x20);
        assert!(recorded.combo.ctrl);
        assert!(recorded.combo.shift);
        assert_eq!(recorded.combo.label, "Space");
    }

    #[test]
    fn records_a_bare_modifier_pair_but_not_a_lone_modifier() {
        let ctrl_shift = record("", true, false, true, false).unwrap();
        assert!(ctrl_shift.combo.is_modifier_only());
        assert_eq!(ctrl_shift.combo.caps(), ["Ctrl", "Shift"]);
        // One modifier is held constantly while typing — never a hotkey.
        assert!(record("", true, false, false, false).is_none());
        assert!(record("", false, false, true, false).is_none());
    }

    #[test]
    fn flags_system_shortcuts_without_blocking_them() {
        let win_space = record(" ", false, false, false, true).unwrap();
        assert!(win_space.system_conflict);

        let ctrl_space = record(" ", true, false, false, false).unwrap();
        assert!(!ctrl_space.system_conflict);
    }
}
