//! The Win32 side of injection: turning key presses into `INPUT` records and
//! handing a batch to `SendInput`.
//!
//! Nothing here knows what a plan means — [`super::send_plan`] owns that. What this
//! module guarantees is that every record carries [`INJECT_TAG`] in `dwExtraInfo`,
//! which is what stops the keyboard hook from composing its own output.

use windows::Win32::UI::Input::KeyboardAndMouse::{
    SendInput, INPUT, INPUT_0, INPUT_KEYBOARD, KEYBDINPUT, KEYBD_EVENT_FLAGS, KEYEVENTF_KEYUP,
    KEYEVENTF_UNICODE, VIRTUAL_KEY, VK_BACK,
};

use crate::shared::shell::INJECT_TAG;

/// Backspace's scan code. A position on the keyboard rather than a character, so
/// it is the same on every layout, which is why it can be a constant here instead
/// of a `MapVirtualKeyW` call on the hook's hot path.
///
/// Windows dispatches these events by `wVk` — `wScan` is along for the ride, and
/// with the flags we use it changes nothing about *delivery*. What it changes is
/// what the app reads: the scan code lands in the low-level hook struct and in
/// `WM_KEYDOWN`'s `lParam`, and an app that keys off that instead of the virtual
/// key sees a hardware Backspace rather than one claiming to come from nowhere.
/// Costs nothing to be right about.
pub(super) const VK_BACK_SCAN: u16 = 0x0E;

/// One virtual-key press or release. `scan` is the key's scan code, or 0 when the
/// event only has to reach apps that read `wVk` (see [`VK_BACK_SCAN`]).
pub(super) fn vk_event(vk: VIRTUAL_KEY, scan: u16, up: bool) -> INPUT {
    let dw_flags = if up {
        KEYEVENTF_KEYUP
    } else {
        KEYBD_EVENT_FLAGS(0)
    };
    INPUT {
        r#type: INPUT_KEYBOARD,
        Anonymous: INPUT_0 {
            ki: KEYBDINPUT {
                wVk: vk,
                wScan: scan,
                dwFlags: dw_flags,
                time: 0,
                dwExtraInfo: INJECT_TAG,
            },
        },
    }
}

/// One UTF-16 code unit, delivered as text rather than as a key on some layout.
fn unicode_event(unit: u16, up: bool) -> INPUT {
    let mut dw_flags = KEYEVENTF_UNICODE;
    if up {
        dw_flags |= KEYEVENTF_KEYUP;
    }
    INPUT {
        r#type: INPUT_KEYBOARD,
        Anonymous: INPUT_0 {
            ki: KEYBDINPUT {
                wVk: VIRTUAL_KEY(0),
                wScan: unit,
                dwFlags: dw_flags,
                time: 0,
                dwExtraInfo: INJECT_TAG,
            },
        },
    }
}

/// Backspace key presses (down+up) for `n` deletions.
pub(super) fn deletions(n: usize) -> Vec<INPUT> {
    let mut v = Vec::with_capacity(n * 2);
    for _ in 0..n {
        v.push(vk_event(VK_BACK, VK_BACK_SCAN, false));
        v.push(vk_event(VK_BACK, VK_BACK_SCAN, true));
    }
    v
}

/// Unicode key presses (down+up) for the composed `units`.
pub(super) fn text(units: &[u16]) -> Vec<INPUT> {
    let mut v = Vec::with_capacity(units.len() * 2);
    for &unit in units {
        v.push(unicode_event(unit, false));
        v.push(unicode_event(unit, true));
    }
    v
}

/// Hand a batch to the OS. Empty batches are dropped rather than sent — `SendInput`
/// treats a zero-length array as an error, and a no-op plan reaches here.
pub(super) fn raw_send(inputs: &[INPUT]) {
    if inputs.is_empty() {
        return;
    }
    unsafe { SendInput(inputs, std::mem::size_of::<INPUT>() as i32) };
}
