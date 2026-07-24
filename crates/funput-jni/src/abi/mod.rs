//! Shared JNI-boundary plumbing every export is built on.
//!
//! - [`guard`] — the panic + `Outcome` guards (`safe`, `neutral`).
//! - [`jstring`] — Java string/object marshalling (`string_result`, `JavaObject`).
//!
//! Both JNI subsystems ([`crate::engine`], [`crate::suggestion`]) build on these,
//! so each `native*` export stays a thin, uniform wrapper.

mod guard;
mod jstring;

pub(crate) use guard::{neutral, safe};
pub(crate) use jstring::{JavaObject, string_result};
