//! Shared C-ABI plumbing every export is built on.
//!
//! - [`guard`] — the panic + null-handle guard (`safe`, `with_engine_*`).
//! - [`codec`] — UTF-32 ↔ `char` marshalling (`copy_codepoints`, `decode_codepoints`).
//!
//! Both engine subsystems ([`crate::engine`], [`crate::suggestion`]) build on these,
//! so each `funput_*` export stays a thin, uniform wrapper.

mod codec;
mod guard;

#[cfg(test)]
pub(crate) use codec::decode_codepoints;
pub(crate) use codec::{copy_codepoints, string_from_utf32};
pub(crate) use guard::{safe, with_engine_mut, with_engine_ref};
