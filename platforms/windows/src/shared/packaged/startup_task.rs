//! Autostart for an MSIX-packaged install (`windows.startupTask`).
//!
//! The portable build writes `HKCU\…\Run`. A packaged install cannot: the
//! install directory is read-only, and Store policy wants the task declared in
//! the manifest. `TASK_ID` must match `AppxManifest.xml`.
//!
//! Unlike the Run key, the task has an owner besides Funput: the user can switch
//! it off under Settings → Apps → Startup (or Task Manager), and an organization
//! can pin it either way. Funput cannot override those, so every call here
//! reports the state Windows actually settled on instead of assuming success.

/// Declared in `msix/AppxManifest.xml.template` as `desktop:StartupTask TaskId`.
pub const TASK_ID: &str = "FunputStartup";

/// `Windows.ApplicationModel.StartupTaskState`, by the same numbering.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum State {
    Disabled,
    DisabledByUser,
    Enabled,
    DisabledByPolicy,
    EnabledByPolicy,
}

impl State {
    pub fn from_raw(raw: i32) -> Option<Self> {
        Some(match raw {
            0 => Self::Disabled,
            1 => Self::DisabledByUser,
            2 => Self::Enabled,
            3 => Self::DisabledByPolicy,
            4 => Self::EnabledByPolicy,
            _ => return None,
        })
    }

    pub fn is_enabled(self) -> bool {
        matches!(self, Self::Enabled | Self::EnabledByPolicy)
    }
}

/// Title and body for the Settings notice when Windows refused `wanted`.
pub fn refusal(wanted: bool) -> (&'static str, &'static str) {
    if wanted {
        (
            "Windows đang chặn Funput khởi động cùng máy",
            "Bật lại Funput trong Cài đặt Windows → Ứng dụng → Khởi động. \
             Nếu mục đó bị khoá, tổ chức của bạn đang quản lý cài đặt này.",
        )
    } else {
        (
            "Không tắt được khởi động cùng máy",
            "Tổ chức của bạn đang yêu cầu Funput khởi động cùng Windows.",
        )
    }
}

/// The task's state as Windows reports it, without changing anything. `None`
/// when WinRT cannot answer.
pub fn current() -> Option<State> {
    query(None)
}

/// Ask Windows to enable or disable the task; returns where it settled, which
/// can differ from `on`. `None` when WinRT cannot answer.
pub fn request(on: bool) -> Option<State> {
    query(Some(on))
}

#[cfg(windows)]
fn query(change: Option<bool>) -> Option<State> {
    use windows::ApplicationModel::{StartupTask, StartupTaskState};

    // `join` blocks on a Win32 event, not the calling thread's message loop, and
    // the desktop StartupTask never shows UI — so this is safe on the Slint thread.
    let settle = || -> windows::core::Result<StartupTaskState> {
        let task = StartupTask::GetAsync(&TASK_ID.into())?.join()?;
        let state = task.State()?;
        match change {
            // Returns the resulting state: DisabledByUser/-ByPolicy stay put.
            Some(true) if !State::from_raw(state.0).is_some_and(State::is_enabled) => {
                task.RequestEnableAsync()?.join()
            }
            // Disable only moves Enabled; the other states are not Funput's to change.
            Some(false) if state == StartupTaskState::Enabled => {
                task.Disable()?;
                task.State()
            }
            _ => Ok(state),
        }
    };
    settle().ok().and_then(|state| State::from_raw(state.0))
}

#[cfg(not(windows))]
fn query(_change: Option<bool>) -> Option<State> {
    None
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn task_id_matches_manifest_contract() {
        assert_eq!(TASK_ID, "FunputStartup");
        assert!(!TASK_ID.contains(' '));
    }

    #[test]
    fn raw_states_follow_winrt_numbering() {
        let states = [
            State::Disabled,
            State::DisabledByUser,
            State::Enabled,
            State::DisabledByPolicy,
            State::EnabledByPolicy,
        ];
        for (raw, state) in states.into_iter().enumerate() {
            assert_eq!(State::from_raw(raw as i32), Some(state));
        }
        assert_eq!(State::from_raw(5), None);
        assert_eq!(State::from_raw(-1), None);
    }

    #[test]
    fn only_enabled_states_count_as_on() {
        assert!(State::Enabled.is_enabled());
        assert!(State::EnabledByPolicy.is_enabled());
        assert!(!State::Disabled.is_enabled());
        assert!(!State::DisabledByUser.is_enabled());
        assert!(!State::DisabledByPolicy.is_enabled());
    }

    #[test]
    fn refusal_explains_the_direction_windows_refused() {
        assert!(refusal(true).1.contains("Khởi động"));
        assert!(refusal(false).1.contains("Tổ chức"));
        assert_ne!(refusal(true).0, refusal(false).0);
    }
}
