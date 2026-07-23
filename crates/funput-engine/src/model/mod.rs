//! Data types that flow through the engine.
//!
//! - `session` — the internal, mutable per-session state (crate-private).
//! - `key_source` — the input model: where a keystroke physically came from.
//! - `result` — the output model: the [`crate::Action`] / [`crate::ImeResult`] a
//!   platform applies.
//!
//! `key_source` and `result` are re-exported as the crate's public types from the
//! crate root; `Session` stays crate-internal.

pub(crate) mod key_source;
pub(crate) mod result;
mod session;

pub(crate) use session::Session;
