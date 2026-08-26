//! The two places this window reaches past Slint.
//!
//! Both are input/output the toolkit does not offer, not styling or layout — nothing
//! here draws anything. Keeping them together makes the surface easy to audit: if
//! Slint grows either capability, these are the two files that go away.
//!
//! - [`drop`] — files dropped on the window. Slint 1.17 has a `DropArea`, but only
//!   for drags that start inside the application; the winit backend never forwards
//!   the OS's own file drop.
//! - [`clipboard`] — the "Dán văn bản" and "Chép kết quả" buttons. Slint keeps
//!   clipboard access on its `Platform` trait, which application code cannot reach,
//!   so Ctrl+C/Ctrl+V work inside a text box but a button does not.

mod clipboard;
mod drop;

pub(super) use clipboard::{read, write};
pub(super) use drop::accept;
