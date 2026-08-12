//! Starting a UI child, and owning it afterwards.
//!
//! This exists instead of `std::process::Command` for one reason: the startup
//! feedback cursor. `CreateProcess` makes the shell show the busy pointer until
//! the new process goes idle, and for a Slint + Skia window that is long enough to
//! read as "loading" on what is an ordinary tray click. Turning it off is a
//! `STARTUPINFOW::dwFlags` bit, and `Command` only reaches `dwCreationFlags` —
//! `startupinfo_force_feedback` is still unstable (rust-lang/rust#141010) and the
//! toolchain is pinned to stable.
//!
//! So the spawn is done by hand, and [`UiProcess`] takes over what `Child` was
//! holding: the process handle, closed on drop.

mod buffers;

use std::path::Path;

use windows::core::PWSTR;
use windows::Win32::Foundation::{CloseHandle, HANDLE, WAIT_TIMEOUT};
use windows::Win32::System::Threading::{
    CreateProcessW, GetExitCodeProcess, TerminateProcess, WaitForSingleObject,
    CREATE_UNICODE_ENVIRONMENT, INFINITE, PROCESS_INFORMATION, STARTF_FORCEOFFFEEDBACK,
    STARTUPINFOW,
};

use buffers::{command_line, environment};

/// A running UI child process. Owns the handle; closes it on drop.
pub(super) struct UiProcess(HANDLE);

impl UiProcess {
    /// Launch `exe arg` with `extra` added to this process's environment, without
    /// the startup feedback cursor.
    ///
    /// Handles are not inherited: the children are GUI processes that need none,
    /// and a debug build's console reaches them by attachment rather than through
    /// `STARTUPINFO`, so `eprintln!` still lands.
    pub(super) fn spawn(exe: &Path, arg: &str, extra: &[(&str, &str)]) -> Option<Self> {
        let mut line = command_line(exe, arg);
        let mut env = environment(extra);
        let startup = STARTUPINFOW {
            cb: size_of::<STARTUPINFOW>() as u32,
            dwFlags: STARTF_FORCEOFFFEEDBACK,
            ..Default::default()
        };
        let mut info = PROCESS_INFORMATION::default();
        unsafe {
            CreateProcessW(
                None,
                Some(PWSTR(line.as_mut_ptr())),
                None,
                None,
                false,
                CREATE_UNICODE_ENVIRONMENT,
                Some(env.as_mut_ptr().cast()),
                None,
                &startup,
                &mut info,
            )
            .ok()?;
            // Only the process is tracked; the initial thread's handle is dead weight.
            let _ = CloseHandle(info.hThread);
        }
        Some(Self(info.hProcess))
    }

    /// Raw handle for the pump's wait set. Borrowed — this type stays the owner.
    pub(super) fn handle(&self) -> HANDLE {
        self.0
    }

    /// The exit code once it has finished, `None` while it is still running.
    ///
    /// The wait is what answers "finished", not `GetExitCodeProcess` alone: a
    /// process that exits with `STILL_ACTIVE` would be indistinguishable from a
    /// live one. A failed wait also reports finished — an unusable handle must not
    /// stay in the pump's wait set, where it would spin the loop.
    pub(super) fn exit_code(&self) -> Option<u32> {
        if unsafe { WaitForSingleObject(self.0, 0) } == WAIT_TIMEOUT {
            return None;
        }
        let mut code = 0u32;
        let _ = unsafe { GetExitCodeProcess(self.0, &mut code) };
        Some(code)
    }

    /// Kill it and wait for it to actually go, so the caller can drop the slot
    /// knowing nothing is left half-dead in the wait set.
    pub(super) fn kill(&self) {
        unsafe {
            let _ = TerminateProcess(self.0, 1);
            WaitForSingleObject(self.0, INFINITE);
        }
    }
}

impl Drop for UiProcess {
    fn drop(&mut self) {
        unsafe {
            let _ = CloseHandle(self.0);
        }
    }
}

#[cfg(test)]
mod tests {
    use std::path::PathBuf;
    use std::time::{Duration, Instant};

    use super::*;

    fn comspec() -> PathBuf {
        std::env::var_os("COMSPEC")
            .map(PathBuf::from)
            .unwrap_or_else(|| r"C:\Windows\System32\cmd.exe".into())
    }

    /// Block until the child is gone, so a hung spawn fails the test instead of
    /// hanging the run.
    fn wait(child: &UiProcess) -> u32 {
        let deadline = Instant::now() + Duration::from_secs(10);
        loop {
            if let Some(code) = child.exit_code() {
                return code;
            }
            assert!(Instant::now() < deadline, "child never exited");
            std::thread::sleep(Duration::from_millis(10));
        }
    }

    #[test]
    fn a_child_starts_and_reports_its_exit_code() {
        // The Control Center answers through its exit code, so this round trip is
        // the whole contract with the flyout.
        let child = UiProcess::spawn(&comspec(), "/c exit 7", &[]).expect("spawn failed");
        assert_eq!(wait(&child), 7);
    }

    #[test]
    fn extra_variables_reach_the_child() {
        // The hand-built environment block is the riskiest part of not using
        // `Command`: get it subtly wrong and the child starts with no variables at
        // all, which would strand the flyout with no tray rect and no parent pid.
        let child = UiProcess::spawn(
            &comspec(),
            r#"/c if "%FUNPUT_SPAWN_TEST%"=="ok" (exit 5) else (exit 1)"#,
            &[("FUNPUT_SPAWN_TEST", "ok")],
        )
        .expect("spawn failed");
        assert_eq!(wait(&child), 5, "the child did not see FUNPUT_SPAWN_TEST");
    }

    #[test]
    fn the_inherited_environment_survives_alongside_them() {
        // `extra` is layered on top of this process's environment, not a
        // replacement for it — the children still need PATH, TEMP and the rest.
        let child = UiProcess::spawn(
            &comspec(),
            r#"/c if defined SystemRoot (exit 4) else (exit 1)"#,
            &[("FUNPUT_SPAWN_TEST", "ok")],
        )
        .expect("spawn failed");
        assert_eq!(wait(&child), 4, "the inherited environment was dropped");
    }

    #[test]
    fn kill_ends_a_running_child() {
        // Opening Settings kills the flyout first; that has to actually finish, or
        // the pump keeps waiting on a handle that never signals.
        let child = UiProcess::spawn(&comspec(), "/c ping -n 20 127.0.0.1 >nul", &[])
            .expect("spawn failed");
        child.kill();
        assert!(
            child.exit_code().is_some(),
            "kill returned before the child was gone"
        );
    }
}
