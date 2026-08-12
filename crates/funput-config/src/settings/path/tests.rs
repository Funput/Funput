use super::*;
use std::fs;

use crate::test_support::unique_dir;

fn tmp() -> PathBuf {
    unique_dir("settings-path")
}

#[test]
fn env_overrides_everything() {
    let root = tmp();
    let custom = root.join("custom.json");
    let exe = root.join("exe");
    let app = root.join("app").join(FILE);
    fs::create_dir_all(&exe).unwrap();
    assert_eq!(
        resolve(Some(custom.clone()), Some(&exe), true, Some(&app)),
        Some(custom)
    );
    let _ = fs::remove_dir_all(root);
}

#[test]
fn prefers_writable_exe_dir() {
    let root = tmp();
    let exe = root.join("exe");
    let app = root.join("app").join(FILE);
    fs::create_dir_all(&exe).unwrap();
    assert_eq!(
        resolve(None, Some(&exe), true, Some(&app)),
        Some(exe.join(FILE))
    );
    let _ = fs::remove_dir_all(root);
}

#[test]
fn falls_back_to_appdata_when_exe_not_writable() {
    let root = tmp();
    let exe = root.join("exe");
    let app = root.join("app").join(FILE);
    fs::create_dir_all(&exe).unwrap();
    assert_eq!(resolve(None, Some(&exe), false, Some(&app)), Some(app));
    let _ = fs::remove_dir_all(root);
}

/// A beside-exe install ignores AppData completely — it is neither copied from nor
/// touched, and a missing beside-exe file stays missing until something saves it.
/// An AppData file left over from an older install is simply not this install's.
#[test]
fn an_appdata_file_is_never_read_or_copied() {
    let root = tmp();
    let exe = root.join("exe");
    let app_dir = root.join("app");
    let app = app_dir.join(FILE);
    fs::create_dir_all(&exe).unwrap();
    fs::create_dir_all(&app_dir).unwrap();
    fs::write(&app, r#"{"method":"vni","enabled":true}"#).unwrap();

    let path = resolve(None, Some(&exe), true, Some(&app)).unwrap();
    assert_eq!(path, exe.join(FILE));
    assert!(
        !path.exists(),
        "resolve must not create the beside-exe file"
    );
    assert_eq!(
        fs::read_to_string(&app).unwrap(),
        r#"{"method":"vni","enabled":true}"#,
        "the AppData file must be left untouched"
    );
    let _ = fs::remove_dir_all(root);
}
