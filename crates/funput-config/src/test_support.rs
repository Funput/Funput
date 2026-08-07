//! Shared helpers for this crate's tests.

use std::path::PathBuf;
use std::sync::atomic::{AtomicU64, Ordering};

/// Distinguishes concurrent callers. A timestamp alone is not enough: `cargo test`
/// runs these on parallel threads, and two that start within the same clock tick
/// get the same name — then one test's cleanup deletes the other's directory.
static COUNTER: AtomicU64 = AtomicU64::new(0);

/// A fresh empty directory under the system temp dir, unique per call.
pub fn unique_dir(tag: &str) -> PathBuf {
    let dir = std::env::temp_dir().join(format!(
        "funput-{tag}-{}-{}",
        std::process::id(),
        COUNTER.fetch_add(1, Ordering::Relaxed)
    ));
    let _ = std::fs::remove_dir_all(&dir);
    std::fs::create_dir_all(&dir).expect("temp dir");
    dir
}
