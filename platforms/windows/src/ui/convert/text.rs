//! Saving a pasted paragraph to a file the user picks.
//!
//! Everything else about a paragraph — what it becomes, and what it costs — lives in
//! [`funput_convert`], shared with the GTK window on Linux. What is left here is the
//! one part that cannot be: the Save dialog.

use funput_convert::charset;

use super::view;

/// Save the converted paragraph to a file the user picks.
///
/// Bytes, not text. A legacy target stores one byte per letter, and writing the same
/// characters as UTF-8 would spend two on each and produce a file `.VnTime` cannot
/// read back — the one mistake that would make the whole window pointless.
pub(super) fn save() {
    let Some(window) = view::current() else {
        return;
    };
    let (input, from, to) = view::STATE.with(|s| {
        let state = s.borrow();
        (
            state.input.clone(),
            state.source.map(view::at),
            view::at(state.target),
        )
    });
    let Some(from) = from else { return };
    let Some(path) = rfd::FileDialog::new()
        .set_file_name("chuyen-ma.txt")
        .add_filter("Văn bản", &["txt"])
        .save_file()
    else {
        return;
    };
    let message = match std::fs::write(
        &path,
        charset::render(&charset::read(&input, from), to).bytes,
    ) {
        Ok(()) => format!("Đã lưu {}", path.display()),
        Err(err) => format!("Không lưu được: {err}"),
    };
    window.set_progress(message.into());
}
