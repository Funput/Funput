//! Single-instance gate for the tray/hook process.
//!
//! UI child processes (`--settings`, …) skip this module. A second main launch
//! signals `ActivateSettings` so the running instance opens Settings, then exits.

use std::sync::OnceLock;
use std::time::Duration;

use windows::core::w;
use windows::Win32::Foundation::{
    CloseHandle, GetLastError, ERROR_ALREADY_EXISTS, HANDLE, WAIT_EVENT, WAIT_OBJECT_0,
};
use windows::Win32::System::Threading::{
    CreateEventW, CreateMutexW, OpenEventW, ResetEvent, SetEvent, WaitForSingleObject,
    EVENT_MODIFY_STATE, INFINITE,
};

const MUTEX_NAME: windows::core::PCWSTR = w!("Local\\Funput.SingleInstance");
const EVENT_NAME: windows::core::PCWSTR = w!("Local\\Funput.ActivateSettings");

struct Handles {
    /// Kept alive for the process lifetime so the mutex stays held.
    _mutex: HANDLE,
    event: HANDLE,
}

// HANDLE is a kernel object pointer; we only touch these from the tray/hook
// thread after `claim()`, and `signal_activate` opens its own handle.
unsafe impl Send for Handles {}
unsafe impl Sync for Handles {}

static HANDLES: OnceLock<Handles> = OnceLock::new();

/// Claim the tray singleton. `true` = this process is the primary instance.
pub fn claim() -> bool {
    unsafe {
        let Ok(event) = CreateEventW(None, false, false, EVENT_NAME) else {
            return false;
        };
        // Clear stale last-error so ALREADY_EXISTS only reflects CreateMutexW.
        let _ = windows::Win32::Foundation::SetLastError(
            windows::Win32::Foundation::WIN32_ERROR(0),
        );
        let Ok(mutex) = CreateMutexW(None, false, MUTEX_NAME) else {
            let _ = CloseHandle(event);
            return false;
        };
        if GetLastError() == ERROR_ALREADY_EXISTS {
            let _ = CloseHandle(mutex);
            let _ = CloseHandle(event);
            return false;
        }
        let _ = HANDLES.set(Handles {
            _mutex: mutex,
            event,
        });
        true
    }
}

/// Wake the primary instance so it opens Settings (second-launch path).
pub fn signal_activate() {
    for _ in 0..2 {
        if unsafe { try_signal() } {
            return;
        }
        std::thread::sleep(Duration::from_millis(50));
    }
}

unsafe fn try_signal() -> bool {
    let Ok(event) = OpenEventW(EVENT_MODIFY_STATE, false, EVENT_NAME) else {
        return false;
    };
    let ok = SetEvent(event).is_ok();
    let _ = CloseHandle(event);
    ok
}

/// Event handle for `MsgWaitForMultipleObjects` on the hook thread.
pub fn activate_handle() -> Option<HANDLE> {
    HANDLES.get().map(|h| h.event)
}

/// Non-blocking: consume a pending activate signal.
pub fn poll_activate() -> bool {
    let Some(event) = activate_handle() else {
        return false;
    };
    unsafe {
        if WaitForSingleObject(event, 0) != WAIT_OBJECT_0 {
            return false;
        }
        let _ = ResetEvent(event);
        true
    }
}

/// Block until the activate event or a thread message arrives.
pub fn wait_activate_or_message() -> WaitKind {
    let Some(event) = activate_handle() else {
        return WaitKind::Failed;
    };
    unsafe {
        let handles = [event];
        let result = windows::Win32::UI::WindowsAndMessaging::MsgWaitForMultipleObjects(
            Some(&handles),
            false,
            INFINITE,
            windows::Win32::UI::WindowsAndMessaging::QS_ALLINPUT,
        );
        // nCount=1 → index 0 is the event; index 1 means the thread message queue.
        if result == WAIT_OBJECT_0 {
            let _ = ResetEvent(event);
            WaitKind::Activate
        } else if result == WAIT_EVENT(WAIT_OBJECT_0.0 + 1) {
            WaitKind::Message
        } else {
            WaitKind::Failed
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum WaitKind {
    Activate,
    Message,
    Failed,
}
