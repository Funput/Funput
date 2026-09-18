//! Stateful orchestration between `funput-core` and platform inject instructions.
//!
//! Transform rules stay in core; this crate owns raw keys, composition state,
//! boundaries, restore, Flip, backspace, and minimal text diffs.
//!
//! # Layout
//!
//! The source is grouped by concern so the crate stays approachable as it grows:
//!
//! - `engine/` — the public [`Engine`] facade: configuration, key processing, and
//!   editing (backspace / flip). This is the whole caller-facing surface.
//! - `compose/` — the internal composition pipeline (keystroke → [`ImeResult`]):
//!   word boundaries, English restore, Flip, and buffer diffs.
//! - `model/` — the data types that flow through: session state, the input
//!   [`KeySource`], and the platform [`ImeResult`] / [`Action`].
//! - `correction/` — typo correction: the touch evidence a host reports, and the
//!   candidate words it makes reachable when a word ends on a slip.

mod compose;
mod correction;
mod engine;
mod model;

pub use correction::{CorrectionCandidate, KeyTouch, MAX_ALTERNATES};
pub use engine::Engine;
pub use model::EngineConfig;
pub use model::key_source::KeySource;
pub use model::result::{Action, ImeResult};
