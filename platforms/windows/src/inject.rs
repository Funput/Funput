//! Emit an [`InjectPlan`] to the focused app: Backspace presses, then Unicode
//! characters, via `SendInput`. Every synthesized event carries [`INJECT_TAG`] in
//! `dwExtraInfo` so the hook ignores them (no re-entrancy).

use funput_desktop::InjectPlan;
use windows::Win32::Foundation::{HWND, LPARAM, WPARAM};
use windows::Win32::UI::Input::KeyboardAndMouse::{
    SendInput, INPUT, INPUT_0, INPUT_KEYBOARD, KEYBDINPUT, KEYBD_EVENT_FLAGS, KEYEVENTF_KEYUP,
    KEYEVENTF_UNICODE, VIRTUAL_KEY, VK_BACK, VK_DELETE,
};
use windows::Win32::UI::WindowsAndMessaging::{PostMessageW, WM_CHAR, WM_KEYDOWN, WM_KEYUP};

use crate::shell::INJECT_TAG;

fn vk_event(vk: VIRTUAL_KEY, up: bool) -> INPUT {
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
                wScan: 0,
                dwFlags: dw_flags,
                time: 0,
                dwExtraInfo: INJECT_TAG,
            },
        },
    }
}

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
fn deletions(n: usize) -> Vec<INPUT> {
    let mut v = Vec::with_capacity(n * 2);
    for _ in 0..n {
        v.push(vk_event(VK_BACK, false));
        v.push(vk_event(VK_BACK, true));
    }
    v
}

/// Unicode key presses (down+up) for the composed `units`.
fn text(units: &[u16]) -> Vec<INPUT> {
    let mut v = Vec::with_capacity(units.len() * 2);
    for &unit in units {
        v.push(unicode_event(unit, false));
        v.push(unicode_event(unit, true));
    }
    v
}

fn raw_send(inputs: &[INPUT]) {
    if inputs.is_empty() {
        return;
    }
    unsafe { SendInput(inputs, std::mem::size_of::<INPUT>() as i32) };
}

/// Send the deletions then the new text as one atomic `SendInput` batch. This is
/// the default path used for every app except Chrome (see [`send_plan_chrome`]).
pub fn send_plan(plan: &InjectPlan) {
    if plan.is_noop() {
        return;
    }
    let mut inputs = deletions(plan.backspaces);
    inputs.extend(text(&plan.units));
    raw_send(&inputs);
}

/// Deliver a plan to one of Funput's **own** windows (the Slint Settings UI).
///
/// winit (Slint's backend) does not consume the synthetic Unicode key events that
/// `SendInput`/`KEYEVENTF_UNICODE` produce — it maps input by physical scancode — so
/// composed diacritics never land in our own `LineEdit`. Instead we post the real
/// window messages winit *does* read: `WM_KEYDOWN`/`WM_KEYUP` (VK_BACK) for the
/// deletions and `WM_CHAR` for each UTF-16 unit. `PostMessage` bypasses the low-level
/// hook entirely, so no `INJECT_TAG` round-trip is needed.
///
/// `hwnd` is our foreground top-level window; winit routes the messages to the
/// focused control (the expansion field).
pub fn send_plan_to_window(plan: &InjectPlan, hwnd: HWND) {
    if plan.is_noop() {
        return;
    }
    // Backspace, with the standard scancode (0x0E) in the lParam so winit recognises
    // the physical key; bits 30/31 mark the key-up transition.
    const BACK_SCAN: isize = 0x0E << 16;
    let down = LPARAM(BACK_SCAN | 1);
    let up = LPARAM(BACK_SCAN | 1 | (1 << 30) | (1 << 31));
    for _ in 0..plan.backspaces {
        unsafe {
            let _ = PostMessageW(hwnd, WM_KEYDOWN, WPARAM(VK_BACK.0 as usize), down);
            let _ = PostMessageW(hwnd, WM_KEYUP, WPARAM(VK_BACK.0 as usize), up);
        }
    }
    for &unit in &plan.units {
        unsafe {
            let _ = PostMessageW(hwnd, WM_CHAR, WPARAM(unit as usize), LPARAM(0));
        }
    }
}

/// Like [`send_plan`] but prepends a single `Delete` press before the deletions,
/// for Chrome's omnibox.
///
/// The omnibox shows an inline-autocomplete *suffix that is selected* (e.g. after
/// "bo" it displays "bo[okmarks]"). A Backspace fired against that selection deletes
/// the **selection**, not the base character, so the vowel we meant to replace
/// survives and the new glyph piles on top: "bộ" → "boộ". The leading `Delete`
/// dismisses that autocomplete selection first; the Backspaces then bite real
/// characters. When there is no autocomplete and the caret sits at the end of the
/// field (the normal case while typing), `Delete` is a no-op, so this stays safe.
///
/// Sent as one synchronous `SendInput` batch — autocomplete is recomputed
/// asynchronously, so it will not re-appear between the `Delete` and the Backspaces
/// within a single burst. Only used when `backspaces > 0` (a pure insert has no
/// Backspace to lose).
///
/// Caveat: in a Chrome **web** field the omnibox autocomplete does not exist, so the
/// `Delete` is wanted only at end-of-text; if the caret is in the middle of existing
/// text it will eat the following character. See `shell::foreground_is_chrome`.
pub fn send_plan_chrome(plan: &InjectPlan) {
    if plan.is_noop() {
        return;
    }
    if plan.backspaces == 0 {
        send_plan(plan); // nothing to lose to autocomplete; plain insert
        return;
    }
    let mut inputs = Vec::with_capacity(2 + plan.backspaces * 2 + plan.units.len() * 2);
    inputs.push(vk_event(VK_DELETE, false)); // dismiss the inline-autocomplete selection
    inputs.push(vk_event(VK_DELETE, true));
    inputs.extend(deletions(plan.backspaces));
    inputs.extend(text(&plan.units));
    raw_send(&inputs);
}
