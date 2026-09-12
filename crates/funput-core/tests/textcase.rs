// Gated here, as an inner attribute on the crate root, for the reason spelled out
// in `charset.rs`: a per-`mod` gate would still let the crate root name
// `funput_core::textcase` and fail when the feature is off. Please do not "fix" it
// into per-module attributes.
#![cfg(feature = "textcase")]

//! Chuyển đổi kiểu chữ integration suite — one binary, modules under
//! `tests/textcase/`. `#[path]` is required because a `tests/*.rs` crate root looks
//! up plain `mod` in `tests/`, not a subdirectory.
//!
//! These use only the public API, which is also how they check that the public API
//! is enough: a shell has `Transform`, `Options` and `apply`, and nothing else.

#[path = "textcase/properties.rs"]
mod properties;
