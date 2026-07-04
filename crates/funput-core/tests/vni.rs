//! VNI integration suite — a single test binary whose modules live under
//! `tests/vni/` (mirrors the Telex suite layout). `#[path]` is required because a
//! `tests/*.rs` crate root looks up plain `mod` in `tests/`, not a subdirectory.

#[path = "support/mod.rs"]
mod support;

#[path = "vni/cases.rs"]
mod cases;

#[path = "vni/basic.rs"]
mod basic;
#[path = "vni/regression.rs"]
mod regression;
