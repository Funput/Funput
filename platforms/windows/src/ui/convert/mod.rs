//! The Chuyển mã window: paste a paragraph or drop files, and convert them.
//!
//! Runs in its own short-lived process like Settings — see [`crate::ui`]. This file
//! owns the window's lifetime and its callbacks; everything else lives next door.
//!
//! - [`view`] — the state, and everything the window shows from it.
//! - [`text`] — what a pasted paragraph is, becomes, and costs, and saving it.
//! - [`files`] — the batch: reading each file on its own, and writing them out.
//! - [`win32`] — the two things Slint cannot do: file drop and clipboard buttons.

mod files;
mod text;
mod view;
mod win32;

use std::path::PathBuf;

use slint::{ComponentHandle, ModelRc, VecModel};

use funput_core::charset;

use crate::ui::{mica, system_accent};
use crate::{ConvertWindow, Theme};

use view::{STATE, WINDOW, at, current, refresh};

pub(super) fn open() {
    let window = ConvertWindow::new().expect("create convert window");
    system_accent::apply(&window.global::<Theme>());
    let names: Vec<slint::SharedString> = charset::ALL.iter().map(|c| c.name().into()).collect();
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
}

fn wire(window: &ConvertWindow) {
    window.on_text_changed(|typed| {
        STATE.with(|s| {
            let mut state = s.borrow_mut();
            state.input = typed.to_string();
            // A fresh paste is a fresh document: what the user chose for the last one
            // says nothing about this one, so identify it again.
            state.source = None;
        });
        refresh();
    });
    window.on_pick_source(|index| {
        STATE.with(|s| {
            let mut state = s.borrow_mut();
            let picked = usize::try_from(index).ok();
            // The same picker serves a pasted paragraph and a single file, so it has
            // to land on whichever one is on screen.
            match state.files.as_mut_slice() {
                [only] => only.charset = picked.map(at),
                _ => state.source = picked,
            }
        });
        refresh();
    });
    window.on_pick_target(|index| {
        STATE.with(|s| s.borrow_mut().target = usize::try_from(index).unwrap_or(0));
        refresh();
    });
    window.on_pick_file_source(|row, index| {
        STATE.with(|s| {
            let mut state = s.borrow_mut();
            let charset = at(usize::try_from(index).unwrap_or(0));
            if let Some(entry) = state.files.get_mut(usize::try_from(row).unwrap_or(0)) {
                entry.charset = Some(charset);
            }
        });
        refresh();
    });
    window.on_paste(|| {
        let Some(text) = win32::read() else { return };
        STATE.with(|s| {
            let mut state = s.borrow_mut();
            state.input = text.clone();
            state.source = None;
        });
        if let Some(window) = current() {
            window.set_input_text(text.into());
        }
        refresh();
    });
    window.on_pick_files(|| {
        if let Some(paths) = rfd::FileDialog::new().pick_files() {
            dropped(paths);
        }
    });
    // Converted from the state rather than read back off the window: the pane shows a
    // capped preview of a long document, and copying that would hand over a document
    // with its tail quietly missing.
    window.on_copy_result(|| {
        let converted = STATE.with(|s| {
            let state = s.borrow();
            let (from, input) = match state.files.as_slice() {
                [only] => (only.charset, only.text.as_str()),
                _ => (state.source.map(at), state.input.as_str()),
            };
            from.map(|from| text::preview(input, from, at(state.target)).text)
        });
        if let Some(converted) = converted {
            win32::write(&converted);
        }
    });
    window.on_save_result(text::save);
    window.on_convert_files(files::convert_all);
    window.on_restart(|| {
        STATE.with(|s| *s.borrow_mut() = view::State::new());
        if let Some(window) = current() {
            window.set_input_text(slint::SharedString::new());
        }
        refresh();
    });
}

/// Files arrived, by drop or by dialog. Reading a folder of documents is I/O bound,
/// so it happens off the UI thread — inline would hold the window still for the whole
/// drop.
fn dropped(paths: Vec<PathBuf>) {
    std::thread::spawn(move || {
        let entries = files::scan(&paths);
        let _ = slint::invoke_from_event_loop(move || {
            STATE.with(|s| s.borrow_mut().files = entries);
            refresh();
        });
    });
}
