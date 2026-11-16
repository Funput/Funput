//! Sending Enter or Tab on after the gõ tắt expansion it triggered.

use funput_desktop::TriggerKey;
use windows::Win32::UI::Input::KeyboardAndMouse::{VK_RETURN, VK_TAB};

use super::events::{raw_send, vk_event};

/// Enter's and Tab's scan codes: key positions, the same on every layout — see
/// [`super::events::VK_BACK_SCAN`]. Numpad Enter goes out as the main Enter.
const VK_RETURN_SCAN: u16 = 0x1C;
const VK_TAB_SCAN: u16 = 0x0F;

/// Press and release `key` after the expansion, tagged so the hook lets it pass.
/// It is sent on its own, after the plan's batch, so it lands after the text even
/// when that batch was split with a pause.
pub fn send_key(key: TriggerKey) {
    let (vk, scan) = match key {
        TriggerKey::Enter => (VK_RETURN, VK_RETURN_SCAN),
        TriggerKey::Tab => (VK_TAB, VK_TAB_SCAN),
    };
    raw_send(&[vk_event(vk, scan, false), vk_event(vk, scan, true)]);
}
