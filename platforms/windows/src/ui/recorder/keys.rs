//! Turning one Slint key event into a Win32 virtual-key + display label.
//!
//! Slint hands non-printable keys over as Private-Use-Area code points, so F-keys
//! and the navigation cluster need their own tables; everything else is asked of
//! the *user's current layout* rather than assumed to be US QWERTY.

use slint::platform::Key;
use windows::Win32::UI::Input::KeyboardAndMouse::{GetKeyboardLayout, VkKeyScanExW};

pub(super) fn map_key(ch: char) -> Option<(u16, String)> {
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
}
