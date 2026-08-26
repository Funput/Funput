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
mod text;
mod write;

/// Re-exported so a consumer needs one dependency rather than two, and so the
/// `charset` feature is switched on in exactly one manifest.
pub use funput_core::charset;

pub use batch::{Entry, collect, measure, out_dir_label, ready, scan};
pub use text::{capped, warning};
pub use write::{OUT_DIR, Outcome, report, write_all};

/// Which of the three shapes the window is in.
///
/// **The content decides, not a mode switch.** What the user put in already says
/// whether this is a paragraph or a batch, so asking would be asking them to repeat
/// themselves.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Mode {
    /// Nothing yet: the drop zone.
    Empty,
    /// A pasted paragraph — **or exactly one file**. One file has nothing to compare
    /// against, so a one-row table would hide the thing the user actually wants to
    /// see; it gets the before/after panes instead.
    Text,
    /// Two or more files: a table, because the interesting thing is that the rows
    /// differ.
    Files,
}

impl Mode {
    pub fn of(files: &[Entry], input: &str) -> Self {
        match files.len() {
            0 if input.is_empty() => Self::Empty,
            0 | 1 => Self::Text,
            _ => Self::Files,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// The rule the Windows window expressed inline in `refresh()` and never tested:
    /// one file is not a one-row table.
    #[test]
    fn one_file_is_shown_as_text_and_two_as_a_table() {
        let entry = || Entry {
            path: std::path::PathBuf::from("a.txt"),
            text: String::new(),
            charset: None,
            unmapped: 0,
        };
        assert_eq!(Mode::of(&[], ""), Mode::Empty);
        assert_eq!(Mode::of(&[], "việt"), Mode::Text);
        assert_eq!(Mode::of(&[entry()], ""), Mode::Text);
        assert_eq!(Mode::of(&[entry(), entry()], ""), Mode::Files);
    }
}
