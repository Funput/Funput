//! The public [`Engine`] facade and the methods callers drive it with.
//!
//! This module is just the verbs — `config` (settings), `process` (feed keys), and
//! `editing` (backspace / flip). The state they mutate lives in `crate::model`; the
//! composition algorithms they call live in `crate::compose`.

mod config;
mod editing;
mod process;

use crate::model::Session;

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
