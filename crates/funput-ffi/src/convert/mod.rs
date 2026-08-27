//! Stateful `funput-convert` C ABI for desktop shells.
//!
//! # Safety
//! Every pointer must be null or a live handle of the declared type; text and byte
//! pointers must cover their stated length. Every export catches panics and treats
//! null as a no-op. Handles are single-owner and must be driven serially.

#![allow(
    clippy::missing_safety_doc,
    reason = "the common ABI safety contract is documented once above"
)]

mod handle;
mod state;
mod text;
mod work;

pub use handle::{FunputConvertSession, funput_convert_session_free, funput_convert_session_new};
pub use state::*;
pub use text::*;
pub use work::*;

pub const FUNPUT_CONVERT_MODE_EMPTY: u8 = 0;
pub const FUNPUT_CONVERT_MODE_TEXT: u8 = 1;
pub const FUNPUT_CONVERT_MODE_FILES: u8 = 2;
pub const FUNPUT_CONVERT_UNKNOWN: i32 = -1;

#[cfg(test)]
mod tests;
