//! The caller's choices: plain values, no logic beyond building them.
//!
//! What a keystroke is composed under lives here as data; the rules that read it
//! live in [`crate::composition`] and [`crate::validation`].

mod compose;
mod method;

pub use compose::ComposeOptions;
pub use method::{InputMethod, ToneStyle};
