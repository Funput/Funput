//! The Chuyển mã window, minus the window.
//!
//! Every desktop shell that offers charset conversion needs the same four things,
//! and none of them are graphics: read a dropped batch file by file, say what a
//! conversion will cost, write the copies out without ever touching an original,
//! and report what happened. This crate is those four things.
//!
//! **Why they live here rather than in a shell.** The Windows window had them first,
//! as plain Rust with no Win32 and no Slint in sight. Copying them into the GTK
//! window would put the wording of the loss warning, the `vanban (2).txt` rule and
//! the name of the output folder in two places at once — and
//! `platforms/linux/README.md` already tells the story of what happens next, about a
//! typing decision tree that existed once per shell and drifted. The precedent for
//! the strings is [`funput_core::charset::Charset::name`], which lives in core for
//! exactly this reason.
//!
//! There is a second gain, and it is the larger one. `platforms/windows` and
//! `platforms/linux/settings-gtk` are both excluded from the cargo workspace, so
//! `cargo clippy --workspace`, `cargo test --workspace` and `scripts/check-loc.sh`
//! reach neither. Here, all three do — on every pull request, for both shells.
//!
//! **What stays in a shell**: the file dialogs, the clipboard, the drop target, and
//! whatever that platform does to get work off its UI thread. Those differ per
//! platform by nature; nothing below does.
//!
//! - [`text`] — a paragraph: what it becomes, and what it costs.
//! - [`batch`] — a drop: what each file turned out to be.
//! - [`write`] — the copies, written beside the originals and never over them.

mod batch;
mod session;
mod text;

/// Re-exported so a consumer needs one dependency rather than two, and so the
/// `charset` feature is switched on in exactly one manifest.
pub use funput_core::charset;

use funput_core::charset::Charset;

pub use batch::{OUT_DIR, Outcome, Scan, report};
pub use session::{Job, Mode, Row, Session, Unreadable, View};
pub use text::{capped, unreadable_line, warning};

/// The charset a menu position names, clamped.
///
/// An index can only come from a menu built out of [`charset::ALL`], so clamping
/// keeps a future mistake a wrong entry rather than a panic.
pub fn at(index: usize) -> Charset {
    charset::ALL[clamp(index)]
}

/// A menu position that exists, for the same reason [`at`] clamps.
pub(crate) fn clamp(index: usize) -> usize {
    index.min(charset::ALL.len() - 1)
}

pub fn index_of(charset: Charset) -> Option<usize> {
    charset::ALL.iter().position(|&c| c == charset)
}

/// The display names for every dropdown, in `ALL`'s order.
///
/// The one thing a shell cannot work out for itself: `Charset` is
/// `#[non_exhaustive]`, so code outside core must write a wildcard arm and would
/// silently miss a charset added later.
pub fn charset_names() -> Vec<&'static str> {
    charset::ALL.iter().map(|c| c.name()).collect()
}

/// Read everything that was dropped, one file at a time.
///
/// **Off the UI thread.** A folder of a few hundred documents is I/O bound, and
/// doing it inline holds a window still for the whole drop — so this is a free
/// function returning a `Send` result, and [`Session::adopt`] takes it afterwards.
///
/// Expands a dropped folder one level. Not recursively: dropping a folder of
/// documents is the ordinary case and has to work, while walking a home directory
/// because someone let go over the wrong icon is what makes a tool feel dangerous.
pub fn scan(paths: &[std::path::PathBuf]) -> Scan {
    batch::scan(&batch::collect(paths))
}
