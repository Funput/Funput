//! The Chuyển mã window: paste a paragraph or drop files, and convert them.
//!
//! Runs in its own short-lived process like Settings — see [`crate::ui`]. This file
//! owns the window's lifetime and its callbacks; everything else lives next door.
//!
//! Every decision a callback would otherwise make belongs to
//! [`funput_convert::Session`], shared with the GTK window on Linux so the two cannot
//! drift. What is here is this shell's half of it.
//!
//! - [`view`] — the session, and everything the window shows from it.
//! - [`text`] — saving a document through a Save dialog.
//! - [`files`] — running the batch off the UI thread.
//! - [`win32`] — the two things Slint cannot do: file drop and clipboard buttons.

mod files;
mod text;
mod view;
mod win32;

use std::path::PathBuf;

use slint::{ComponentHandle, ModelRc, VecModel};

use crate::ui::{mica, system_accent};
use crate::{ConvertWindow, Theme};

use view::{ask, current, edit, WINDOW};

/// How many rows to build at once.
///
/// The number is this shell's call, not the crate's — Slint's model virtualizes, so
/// it can afford more than a GTK `ListBox`. The counts and the writing run over the
/// whole batch either way, so a capped list stays honest.
const ROWS: usize = 2_000;

pub(super) fn open() {
    let window = ConvertWindow::new().expect("create convert window");
    system_accent::apply(&window.global::<Theme>());
    let names: Vec<slint::SharedString> = funput_convert::charset_names()
        .into_iter()
        .map(Into::into)
        .collect();
    window.set_charset_names(ModelRc::new(VecModel::from(names)));
    wire(&window);
    let _ = window.show();

    // Deferred for the same reason `mica` defers: winit has not created the native
    // window yet when `show()` returns, so asking for the HWND here gets nothing and
    // the drop registration silently does nothing at all.
    let weak = window.as_weak();
    let _ = slint::invoke_from_event_loop(move || {
        if let Some(window) = weak.upgrade() {
            let active = mica::apply(window.window());
            window.global::<Theme>().set_mica(active);
            win32::accept(window.window(), dropped);
        }
    });
    WINDOW.with(|cell| *cell.borrow_mut() = Some(window.as_weak()));
    edit(|session| session.set_row_window(0, ROWS));
}

fn wire(window: &ConvertWindow) {
    window.on_text_changed(|typed| edit(|s| s.set_input(typed.to_string())));
    window.on_pick_source(|index| edit(|s| s.pick_source(usize::try_from(index).ok())));
    window.on_pick_target(|index| edit(|s| s.set_target(usize::try_from(index).unwrap_or(0))));
    window.on_pick_file_source(|row, index| {
        edit(|s| {
            s.pick_row_source(
                usize::try_from(row).unwrap_or(0),
                Some(usize::try_from(index).unwrap_or(0)),
            );
        });
    });
    window.on_paste(|| {
        let Some(text) = win32::read() else { return };
        if let Some(window) = current() {
            window.set_input_text(text.clone().into());
        }
        edit(|s| s.set_input(text));
    });
    window.on_pick_files(|| {
        if let Some(paths) = rfd::FileDialog::new().pick_files() {
            dropped(paths);
        }
    });
    // Converted from the session rather than read back off the window: the pane shows
    // a capped preview of a long document, and copying that would hand over a
    // document with its tail quietly missing.
    window.on_copy_result(|| {
        if let Some(converted) = ask(funput_convert::Session::result_text) {
            win32::write(&converted);
        }
    });
    window.on_save_result(text::save);
    window.on_convert_files(files::convert_all);
    window.on_restart(|| {
        if let Some(window) = current() {
            window.set_input_text(slint::SharedString::new());
        }
        edit(|s| {
            s.reset();
            s.set_row_window(0, ROWS);
        });
    });
}

/// Files arrived, by drop or by dialog. Reading a folder of documents is I/O bound,
/// so it happens off the UI thread — inline would hold the window still for the whole
/// drop. The session stays here; only the paths and the result cross.
fn dropped(paths: Vec<PathBuf>) {
    std::thread::spawn(move || {
        let scan = funput_convert::scan(&paths);
        let _ = slint::invoke_from_event_loop(move || edit(|s| s.adopt(scan)));
    });
}
