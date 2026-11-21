//! Data types that flow through the engine.
//!
//! - `session` — the internal, mutable per-session state (crate-private).
//! - `config` — the user-configurable options ([`EngineConfig`]) grouped out of
//!   `session`; the seam for a future public `Engine::configure` API.
//! - `lead` — what sits right before the current word: whether it is glued to text
//!   on screen, which keeps it from matching a gõ tắt trigger (`500k`, `abc.vn`).
//! - `sentence` — auto-capitalize's scanner, and the keys Backspace can undo.
//! - `key_source` — the input model: where a keystroke physically came from.
//! - `result` — the output model: the [`crate::Action`] / [`crate::ImeResult`] a
//!   platform applies.
//!
//! `key_source`, `result`, and `config` ([`EngineConfig`]) are re-exported as the
//! crate's public types from the crate root; `Session` stays crate-internal.

mod config;
pub(crate) mod key_source;
mod lead;
pub(crate) mod result;
mod sentence;
mod session;

pub use config::EngineConfig;
pub(crate) use lead::Lead;
pub(crate) use sentence::SentenceTrace;
pub(crate) use session::Session;
