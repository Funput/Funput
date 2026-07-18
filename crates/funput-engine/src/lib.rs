//! Stateful orchestration between `funput-core` and platform inject instructions.
//!
//! Transform rules stay in core; this crate owns raw keys, composition state,
//! boundaries, restore, Flip, backspace, and minimal text diffs.

mod boundary;
mod config;
mod diff;
mod editing;
mod flip;
mod input;
mod pipeline;
mod result;
mod session;

pub use result::{Action, ImeResult};

use session::Session;

/// Vietnamese IME engine — single source of truth for composition state.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Engine {
    session: Session,
}

impl Engine {
    /// New engine with composition enabled and standard Telex selected.
    pub fn new() -> Self {
        Self {
            session: Session::new(),
        }
    }
}

impl Default for Engine {
    fn default() -> Self {
        Self::new()
    }
}
