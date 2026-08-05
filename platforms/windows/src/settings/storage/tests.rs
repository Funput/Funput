use super::*;
use std::fs;

fn tmp() -> PathBuf {
    let dir = std::env::temp_dir().join(format!(
        "funput-settings-{}-{}",
        std::process::id(),
        std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_nanos()
    ));
    fs::create_dir_all(&dir).unwrap();
    dir
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

#[test]
fn migrates_appdata_once_when_beside_exe_is_missing() {
    let root = tmp();
    let exe = root.join("exe");
    let app_dir = root.join("app");
    let app = app_dir.join(FILE);
    fs::create_dir_all(&exe).unwrap();
    fs::create_dir_all(&app_dir).unwrap();
    fs::write(&app, r#"{"method":"vni","enabled":true}"#).unwrap();

    let path = resolve(None, Some(&exe), true, Some(&app)).unwrap();
    assert_eq!(path, exe.join(FILE));
    assert_eq!(
        fs::read_to_string(&path).unwrap(),
        fs::read_to_string(&app).unwrap()
    );
    // AppData is left in place.
    assert!(app.is_file());
    let _ = fs::remove_dir_all(root);
}

#[test]
fn does_not_overwrite_existing_beside_exe_file() {
    let root = tmp();
    let exe = root.join("exe");
    let app_dir = root.join("app");
    let app = app_dir.join(FILE);
    let beside = exe.join(FILE);
    fs::create_dir_all(&exe).unwrap();
    fs::create_dir_all(&app_dir).unwrap();
    fs::write(&beside, "beside").unwrap();
    fs::write(&app, "appdata").unwrap();

    let path = resolve(None, Some(&exe), true, Some(&app)).unwrap();
    assert_eq!(fs::read_to_string(path).unwrap(), "beside");
    let _ = fs::remove_dir_all(root);
}
