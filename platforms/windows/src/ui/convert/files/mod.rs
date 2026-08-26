//! Running the batch from the window.
//!
//! Reading each file on its own terms, measuring what a target will cost, and writing
//! the copies out all live in [`funput_convert`], shared with the GTK window on Linux.
//! What is left here is the part that cannot be: getting the work off the UI thread
//! and putting the result back on it.
//!
//! - [`rows`] — turning entries into what the window shows.

mod rows;

use super::view;

pub(super) use rows::{show_many, show_one};

/// Convert and write the batch, off the UI thread — it is file I/O over every entry.
pub(super) fn convert_all() {
    let Some(window) = view::current() else {
        return;
    };
    let (entries, target) = view::STATE.with(|s| {
        let state = s.borrow();
        (state.files.clone(), view::at(state.target))
    });
    window.set_can_convert(false);
    window.set_progress("Đang chuyển…".into());
    std::thread::spawn(move || {
        let outcome = funput_convert::write_all(&entries, target);
        let _ = slint::invoke_from_event_loop(move || {
            let Some(window) = view::current() else {
                return;
            };
            window.set_progress(funput_convert::report(&outcome).into());
            window.set_can_convert(true);
        });
    });
}

/// Read every dropped path, off the UI thread. A folder of a few hundred documents is
/// I/O bound, and doing it inline would freeze the window for the whole drop.
pub(super) fn scan(paths: &[std::path::PathBuf]) -> Vec<funput_convert::Entry> {
    funput_convert::scan(&funput_convert::collect(paths))
}
