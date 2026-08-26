//! The parts that are this platform's, not the feature's.
//!
//! Drag-and-drop, the clipboard, the two dialogs, and getting file work off the UI
//! thread. Everything they hand around — what a document is, what it becomes, where
//! the copy goes — comes from [`funput_convert`].
//!
//! Here: how files get in, and how reading them stays off the UI thread. The four
//! things the buttons do are [`act`]'s.

use std::path::PathBuf;
use std::rc::{Rc, Weak};

use adw::prelude::*;
use gtk::{gdk, gio, glib};

mod act;

use super::Convert;

pub(super) use act::{convert_files, copy_result, paste, save_result};

/// Accept files dropped anywhere on the window.
///
/// Two targets, not one. `GdkFileList` is what a modern file manager sends, but some
/// sources — older Qt apps, a few portal implementations — hand over a single `GFile`
/// instead, and a window that only listens for the list silently rejects those.
pub(super) fn accept_drops(convert: &Rc<Convert>) {
    let list = gtk::DropTarget::new(gdk::FileList::static_type(), gdk::DragAction::COPY);
    let weak = Rc::downgrade(convert);
    list.connect_drop(move |_, value, _, _| {
        let Ok(files) = value.get::<gdk::FileList>() else {
            return false;
        };
        let paths = files.files().iter().filter_map(gio::File::path).collect();
        take(&weak, paths);
        true
    });
    convert.window().add_controller(list);

    let single = gtk::DropTarget::new(gio::File::static_type(), gdk::DragAction::COPY);
    let weak = Rc::downgrade(convert);
    single.connect_drop(move |_, value, _, _| {
        let Ok(file) = value.get::<gio::File>() else {
            return false;
        };
        let Some(path) = file.path() else {
            return false;
        };
        take(&weak, vec![path]);
        true
    });
    convert.window().add_controller(single);
}

pub(super) fn pick_files(convert: &Rc<Convert>) {
    let dialog = gtk::FileDialog::builder()
        .title("Chọn tệp cần chuyển mã")
        .modal(true)
        .build();
    let weak = Rc::downgrade(convert);
    dialog.open_multiple(
        Some(convert.window()),
        gio::Cancellable::NONE,
        move |result| {
            let Ok(files) = result else { return };
            let paths = (0..files.n_items())
                .filter_map(|i| files.item(i))
                .filter_map(|obj| obj.downcast::<gio::File>().ok())
                .filter_map(|file| file.path())
                .collect();
            take(&weak, paths);
        },
    );
}

/// Read what arrived, off the UI thread.
///
/// A folder of a few hundred documents is I/O bound, and doing it inline would hold
/// the window still for the whole drop. `gio::spawn_blocking` hands back a future, so
/// the result lands on the main context with no channel in between —
/// `glib::MainContext::channel` was removed in glib 0.20.
fn take(convert: &Weak<Convert>, paths: Vec<PathBuf>) {
    let weak = convert.clone();
    glib::spawn_future_local(async move {
        let scan = gio::spawn_blocking(move || funput_convert::scan(&paths)).await;
        let Some(convert) = weak.upgrade() else {
            return;
        };
        let Ok(scan) = scan else { return };
        convert.session.borrow_mut().adopt(scan);
        convert.set_progress(String::new());
    });
}
