//! Shared panic guard for the C ABI boundary.

use std::panic::{AssertUnwindSafe, catch_unwind};

/// Run `operation`; if it panics, swallow the unwind and return `default` so the
/// panic never crosses the `extern "C"` boundary into the host (Swift/C++). An
/// engine bug thus drops one keystroke instead of aborting the IME process.
pub(crate) fn safe<T>(default: T, operation: impl FnOnce() -> T) -> T {
    catch_unwind(AssertUnwindSafe(operation)).unwrap_or(default)
}
