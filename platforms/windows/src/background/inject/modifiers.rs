//! Injecting while the user still holds the hotkey's modifier keys.

use funput_desktop::{InjectPlan, Mods};
use windows::Win32::UI::Input::KeyboardAndMouse::{
    VIRTUAL_KEY, VK_CONTROL, VK_LWIN, VK_MENU, VK_SHIFT,
};

use super::{raw_send, send_plan, vk_event};

/// Send a plan with the currently-held modifiers momentarily released.
///
/// The flip hotkey fires on its main key's *keydown*, so Ctrl (and Shift) are
/// still down when the plan goes out — and `VK_BACK` under Ctrl is
/// delete-previous-*word*, not delete-character. A correct 3-character plan then
/// wiped the whole word: "tiếng" flipped to "eengs" instead of "tieengs".
///
/// The releases and the restores both carry `INJECT_TAG`, so the hook ignores
/// them. Restoring matters: without it, a user holding Ctrl+Shift to flip a second
/// time would find the chord no longer matches, because the OS would believe those
/// keys had come up.
pub fn send_plan_unmodified(plan: &InjectPlan, held: Mods) {
    if plan.is_noop() {
        return;
    }
    let down: Vec<VIRTUAL_KEY> = [
        held.ctrl.then_some(VK_CONTROL),
        held.alt.then_some(VK_MENU),
        held.shift.then_some(VK_SHIFT),
        held.win.then_some(VK_LWIN),
    ]
    .into_iter()
    .flatten()
    .collect();
    if down.is_empty() {
        send_plan(plan);
        return;
    }

    let events = |up: bool| -> Vec<_> { down.iter().map(|&vk| vk_event(vk, up)).collect() };
    raw_send(&events(true));
    send_plan(plan);
    raw_send(&events(false));
}
