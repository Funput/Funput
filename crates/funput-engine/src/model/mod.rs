//! Data types that flow through the engine.
//!
//! - `session` — the internal, mutable per-session state (crate-private).
//! - `config` — the user-configurable options ([`EngineConfig`]) grouped out of
//!   `session`; the seam for a future public `Engine::configure` API.
//! - `key_source` — the input model: where a keystroke physically came from.
//! - `result` — the output model: the [`crate::Action`] / [`crate::ImeResult`] a
//!   platform applies.
//!
//! `key_source`, `result`, and `config` ([`EngineConfig`]) are re-exported as the
//! crate's public types from the crate root; `Session` stays crate-internal.

mod config;
pub(crate) mod key_source;
pub(crate) mod result;
mod session;

pub use config::EngineConfig;
pub(crate) use session::Session;
