// The whole suite is gated here, as an inner attribute on the crate root, rather
// than on each `mod` below. That is what makes `cargo test --workspace` compile
// this to an empty binary when the feature is off — a per-`mod` gate would still
// let the crate root reference `funput_core::charset` and fail. Please do not
// "fix" it into per-module attributes.
#![cfg(feature = "charset")]

//! Charset conversion integration suite — one binary, modules under
//! `tests/charset/`. `#[path]` is required because a `tests/*.rs` crate root looks
//! up plain `mod` in `tests/`, not a subdirectory.
//!
//! The split from the tests inside `src/` is deliberate: those sweep the letter
//! inventory and need the crate's private accessors to enumerate it, while these
//! only ever touch the public API — which means they also check that the public
//! API is enough to do the job.

// Fixture data (shared by the test modules below).
#[path = "charset/cases.rs"]
mod cases;

// Test modules.
#[path = "charset/properties.rs"]
mod properties;
#[path = "charset/round_trip.rs"]
mod round_trip;
