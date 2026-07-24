//! Panic + `Outcome` guards for the JNI boundary.
//!
//! Every export runs its body through [`safe`], so an engine panic is caught here
//! and turned into a default value instead of unwinding across the JNI boundary
//! into the Android runtime (which would crash the process). [`neutral`] does the
//! same for a `jni` [`Outcome`], collapsing an error/panic to `T::default()`.

use std::panic::{AssertUnwindSafe, catch_unwind};

use jni::Outcome;

pub(crate) fn safe<T>(default: T, operation: impl FnOnce() -> T) -> T {
    catch_unwind(AssertUnwindSafe(operation)).unwrap_or(default)
}

pub(crate) fn neutral<T: Default, E>(outcome: Outcome<T, E>) -> T {
    match outcome {
        Outcome::Ok(value) => value,
        Outcome::Err(_) | Outcome::Panic(_) => T::default(),
    }
}
