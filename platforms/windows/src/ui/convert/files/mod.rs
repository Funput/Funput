//! Running the batch from the window.
//!
//! Reading each file on its own terms, measuring what a target will cost, and writing
//! the copies out all live in [`funput_convert`], shared with the GTK window on Linux.
//! What is left here is the part that cannot be: getting the work off the UI thread
//! and putting the result back on it.

mod rows;

use super::view;

pub(super) use rows::show_many;

/// Convert and write the batch, off the UI thread — it is file I/O over every entry.
///
/// The session stays on this thread; what leaves it is a `Job`, which is exactly why
/// the crate hands one out instead of doing the writing itself.
pub(super) fn convert_all() {
    let Some(window) = view::current() else {
        return;
    };
    let job = view::ask(funput_convert::Session::batch_job);
    window.set_can_convert(false);
    window.set_progress("Đang chuyển…".into());
    std::thread::spawn(move || {
        let outcome = job.run();
        let _ = slint::invoke_from_event_loop(move || {
            let Some(window) = view::current() else {
                return;
            };
            window.set_progress(funput_convert::report(&outcome).into());
            window.set_can_convert(true);
        });
    });
}
