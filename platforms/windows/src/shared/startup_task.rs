//! Autostart for an MSIX-packaged install (`windows.startupTask`).
//!
//! The portable build writes `HKCU\…\Run`. A packaged install cannot: the
//! install directory is read-only, and Store policy wants the task declared in
//! the manifest. `TASK_ID` must match `AppxManifest.xml`.

/// Declared in `msix/AppxManifest.xml.template` as `desktop:StartupTask TaskId`.
pub const TASK_ID: &str = "FunputStartup";

/// What `sync` will ask WinRT to do. Split out so the mapping can be tested
/// without a package identity or the StartupTask API.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Action {
    Enable,
    Disable,
}

pub fn action_for(on: bool) -> Action {
    if on { Action::Enable } else { Action::Disable }
}

/// Enable or disable the manifest startup task. Failures are swallowed the
/// same way the portable `auto-launch` path ignores a registry write error.
pub fn sync(on: bool) {
    let _ = sync_inner(action_for(on));
}

#[cfg(windows)]
fn sync_inner(action: Action) -> windows::core::Result<()> {
    use windows::ApplicationModel::StartupTask;

    let (tx, rx) = std::sync::mpsc::channel();
    StartupTask::GetAsync(&TASK_ID.into())?.when({
        let tx = tx.clone();
        move |result| {
            let _ = tx.send(result);
        }
    })?;
    let task = rx.recv().expect("StartupTask GetAsync waiter dropped")?;
    match action {
        Action::Enable => {
            let (tx, rx) = std::sync::mpsc::channel();
            task.RequestEnableAsync()?.when(move |result| {
                let _ = tx.send(result);
            })?;
            let _state = rx
                .recv()
                .expect("StartupTask RequestEnable waiter dropped")?;
        }
        Action::Disable => task.Disable()?,
    }
    Ok(())
}

#[cfg(not(windows))]
fn sync_inner(_action: Action) -> Result<(), String> {
    Ok(())
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
    fn on_requests_enable() {
        assert_eq!(action_for(true), Action::Enable);
    }

    #[test]
    fn off_requests_disable() {
        assert_eq!(action_for(false), Action::Disable);
    }
}
