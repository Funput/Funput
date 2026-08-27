//! What the four buttons do.
//!
//! **The clipboard is never watched.** It is read when the button is pressed and
//! written when the result is copied, and at no other time. A Vietnamese input method
//! reading the clipboard in the background is exactly the thing users are right to be
//! suspicious of.

use std::rc::Rc;

use adw::prelude::*;
use gtk::{gio, glib};

use crate::convert::Convert;

/// Paste the clipboard into the text pane.
pub(in crate::convert) fn paste(convert: &Rc<Convert>) {
    let clipboard = convert.window().clipboard();
    let weak = Rc::downgrade(convert);
    glib::spawn_future_local(async move {
        let Ok(Some(text)) = clipboard.read_text_future().await else {
            return;
        };
        let Some(convert) = weak.upgrade() else {
            return;
        };
        convert.session.borrow_mut().set_input(text.to_string());
        convert.set_progress(String::new());
    });
}

/// Copy the converted document.
///
/// **From the session, not from the pane.** The pane holds a capped preview of a
/// long document, and copying that would hand over a document with its tail quietly
/// missing.
pub(in crate::convert) fn copy_result(convert: &Rc<Convert>) {
    let converted = convert.session.borrow().result_text();
    let Some(converted) = converted else { return };
    convert.window().clipboard().set_text(&converted);
    convert.set_progress("Đã chép kết quả".to_string());
}

/// Save the converted paragraph to a file the user picks.
///
/// Bytes, not text. A legacy target stores one byte per letter, and writing the same
/// characters as UTF-8 would spend two on each and produce a file `.VnTime` cannot
/// read back — the one mistake that would make the whole window pointless.
pub(in crate::convert) fn save_result(convert: &Rc<Convert>) {
    let bytes = convert.session.borrow().save_bytes();
    let Some(bytes) = bytes else { return };

    let dialog = gtk::FileDialog::builder()
        .title("Lưu tệp đã chuyển mã")
        .modal(true)
        .build();
    dialog.set_initial_name(Some("chuyen-ma.txt"));
    let weak = Rc::downgrade(convert);
    dialog.save(
        Some(convert.window()),
        gio::Cancellable::NONE,
        move |result| {
            let Some(path) = result.ok().and_then(|file| file.path()) else {
                return;
            };
            let Some(convert) = weak.upgrade() else {
                return;
            };
            convert.set_progress(match std::fs::write(&path, &bytes) {
                Ok(()) => format!("Đã lưu {}", path.display()),
                Err(error) => format!("Không lưu được: {error}"),
            });
        },
    );
}

/// Convert and write the batch, off the UI thread.
pub(in crate::convert) fn convert_files(convert: &Rc<Convert>) {
    let job = convert.session.borrow().batch_job();
    convert.set_busy(true);
    convert.set_progress("Đang chuyển…".to_string());

    let weak = Rc::downgrade(convert);
    glib::spawn_future_local(async move {
        let outcome = gio::spawn_blocking(move || job.run()).await;
        let Some(convert) = weak.upgrade() else {
            return;
        };
        convert.set_busy(false);
        convert.set_progress(match outcome {
            Ok(outcome) => funput_convert::report(&outcome),
            Err(_) => "Không chuyển được".to_string(),
        });
    });
}
