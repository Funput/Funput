//! Telex integration suite — a single test binary whose modules live under
//! `tests/telex/`. Grouping them here (instead of many flat `tests/*.rs` files)
//! keeps the suite organised and compiles to one binary. `#[path]` is required
//! because a `tests/*.rs` crate root looks up plain `mod` in `tests/`, not a
//! subdirectory.

#[path = "support/mod.rs"]
mod support;

// Fixture data (shared by the test modules below).
#[path = "telex/cases.rs"]
mod cases;
#[path = "telex/parity_data.rs"]
mod parity_data;

// Test modules.
#[path = "telex/basic.rs"]
mod basic;
#[path = "telex/corpus.rs"]
mod corpus;
#[path = "telex/parity.rs"]
mod parity;
#[path = "telex/properties.rs"]
mod properties;
#[path = "telex/regression.rs"]
mod regression;
