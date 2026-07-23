//! Shared C-ABI plumbing every export is built on.
//!
//! - [`guard`] — the panic + null-handle guard (`safe`, `with_engine_*`).
//! - [`codec`] — UTF-32 ↔ `char` marshalling (`copy_codepoints`, `decode_codepoints`).
//!
//! Both engine subsystems ([`crate::engine`], [`crate::suggestion`]) build on these,
//! so each `funput_*` export stays a thin, uniform wrapper.

mod codec;
mod guard;

pub(crate) use codec::{copy_codepoints, decode_codepoints};
pub(crate) use guard::{safe, with_engine_mut, with_engine_ref};
