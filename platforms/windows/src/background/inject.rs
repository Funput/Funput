//! Emit an [`InjectPlan`] to the focused app: Backspace presses, then Unicode
//! characters, via `SendInput`. Every synthesized event carries [`INJECT_TAG`] in
//! `dwExtraInfo` so the hook ignores them (no re-entrancy).
//!
//! Backspace is the only deletion key that ever goes out, never `Delete`.
//! Everything a plan replaces sits *behind* the caret, so an injection must be
//! incapable of touching what is in front of it — see [`send_plan`].

mod modifiers;

use funput_desktop::InjectPlan;
use windows::Win32::UI::Input::KeyboardAndMouse::{
    SendInput, INPUT, INPUT_0, INPUT_KEYBOARD, KEYBDINPUT, KEYBD_EVENT_FLAGS, KEYEVENTF_KEYUP,
    KEYEVENTF_UNICODE, VIRTUAL_KEY, VK_BACK,
};

pub use modifiers::send_plan_unmodified;

use crate::shared::shell::INJECT_TAG;

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

/// Send the deletions then the new text as one atomic `SendInput` batch. The only
/// injection route there is: every app takes it, and it reaches no further than the
/// `backspaces` characters behind the caret.
///
/// # Why there is no `Delete` primer
///
/// Chrome's omnibox and Firefox's address bar inline-autofill a *selected* suffix —
/// "go" displays as "go[ogle.com]" — and a Backspace fired at that selection deletes
/// the **selection** instead of the base character, so the new glyph piles on top:
/// "go" + `w` gives "goơ" rather than "gơ".
///
/// A leading `Delete` press dismisses that selection, and those browsers were once
/// routed through one. It cannot stay: `Delete` is harmless only with the caret at
/// end-of-text, and nothing here can tell an autofill selection apart from ordinary
/// text in front of the caret — a hook shell cannot read the document. In a browser
/// *page* field, where no omnibox autofill exists, the primer therefore ate the
/// character to the right on every injected keystroke, so moving the caret back into
/// a finished line to insert a word silently destroyed the text after it.
///
/// Narrowing the primer to the URL bar would need the focused *control*, which
/// Windows will not give up: Chrome draws its whole UI in one HWND, so
/// `GetGUIThreadInfo` reports that top-level window for the omnibox and for a page
/// field alike, with no caret. Only UI Automation can tell them apart, and a
/// `WH_KEYBOARD_LL` callback cannot afford a cross-process COM call.
///
/// So the pile-on is left standing for now — it is one visibly wrong syllable in an
/// address bar, where the primer was deleting text the user wrote and never typed
/// over.
pub fn send_plan(plan: &InjectPlan) {
    if plan.is_noop() {
        return;
    }
    let mut inputs = deletions(plan.backspaces);
    inputs.extend(text(&plan.units));
    raw_send(&inputs);
}

#[cfg(test)]
mod tests {
    use windows::Win32::UI::Input::KeyboardAndMouse::VK_DELETE;

    use super::*;

    /// The batch [`send_plan`] would emit, as `(wVk, wScan, keyup)` per event.
    /// Built from the same two halves rather than by calling `send_plan`, which
    /// would `SendInput` into whatever window the test runner has focused.
    fn batch(backspaces: usize, output: &str) -> Vec<(u16, u16, bool)> {
        let units: Vec<u16> = output.encode_utf16().collect();
        let mut inputs = deletions(backspaces);
        inputs.extend(text(&units));
        inputs
            .iter()
            .map(|i| {
                let ki = unsafe { i.Anonymous.ki };
                (ki.wVk.0, ki.wScan, (ki.dwFlags.0 & KEYEVENTF_KEYUP.0) != 0)
            })
            .collect()
    }

    #[test]
    fn plan_is_backspaces_then_text() {
        assert_eq!(
            batch(2, "ơ"),
            vec![
                (VK_BACK.0, 0, false),
                (VK_BACK.0, 0, true),
                (VK_BACK.0, 0, false),
                (VK_BACK.0, 0, true),
                (0, 'ơ' as u16, false),
                (0, 'ơ' as u16, true),
            ]
        );
    }

    #[test]
    fn nothing_deletes_forward() {
        // The regression this file guards: a plan only ever describes characters
        // *behind* the caret, so no batch may carry a key that removes what is in
        // front of it. A `Delete` primer here once cost one character of existing
        // text per keystroke typed into the middle of a line — see [`send_plan`].
        for (backspaces, output) in [(0, "à"), (1, "ộ"), (3, "card ")] {
            let emitted = batch(backspaces, output);
            assert!(
                emitted.iter().all(|&(vk, ..)| vk != VK_DELETE.0),
                "forward delete in plan ({backspaces}, {output:?})"
            );
            assert_eq!(emitted.len(), (backspaces + output.chars().count()) * 2);
        }
    }
}
