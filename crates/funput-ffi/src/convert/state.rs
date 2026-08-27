use funput_convert::Mode;

use crate::abi::{safe, write_text};

use super::handle::{FunputConvertSession, with_mut, with_ref};
use super::{FUNPUT_CONVERT_MODE_EMPTY, FUNPUT_CONVERT_MODE_FILES, FUNPUT_CONVERT_MODE_TEXT};

#[repr(C)]
#[derive(Clone, Copy, Default)]
pub struct FunputConvertView {
    pub mode: u8,
    pub target: usize,
    pub source: i32,
    pub from_file: bool,
    pub has_input_preview: bool,
    pub rows_first: usize,
    pub rows_count: usize,
    pub rows_total: usize,
    pub ready: usize,
    pub unreadable_count: usize,
}

#[repr(C)]
#[derive(Clone, Copy, Default)]
pub struct FunputConvertRow {
    pub source: i32,
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_reset(session: *mut FunputConvertSession) {
    unsafe { with_mut(session, |value| value.reset()) };
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_refresh(session: *mut FunputConvertSession) {
    unsafe { with_mut(session, |value| value.refresh()) };
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_set_target(
    session: *mut FunputConvertSession,
    index: usize,
) {
    unsafe { with_mut(session, |value| value.set_target(index)) };
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_pick_source(
    session: *mut FunputConvertSession,
    index: i32,
) {
    unsafe {
        with_mut(session, |value| {
            value.pick_source(usize::try_from(index).ok())
        })
    };
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_pick_row_source(
    session: *mut FunputConvertSession,
    row: usize,
    index: usize,
) {
    unsafe { with_mut(session, |value| value.pick_row_source(row, index)) };
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_set_row_window(
    session: *mut FunputConvertSession,
    first: usize,
    len: usize,
) {
    unsafe { with_mut(session, |value| value.set_row_window(first, len)) };
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_view(
    session: *const FunputConvertSession,
) -> FunputConvertView {
    unsafe { with_ref(session, snapshot) }
}

fn snapshot(session: &funput_convert::Session) -> FunputConvertView {
    let view = session.view();
    FunputConvertView {
        mode: match view.mode {
            Mode::Empty => FUNPUT_CONVERT_MODE_EMPTY,
            Mode::Text => FUNPUT_CONVERT_MODE_TEXT,
            Mode::Files => FUNPUT_CONVERT_MODE_FILES,
        },
        target: view.target,
        source: view
            .source
            .and_then(|v| i32::try_from(v).ok())
            .unwrap_or(-1),
        from_file: view.from_file,
        has_input_preview: view.input_preview.is_some(),
        rows_first: view.rows_first,
        rows_count: view.rows.len(),
        rows_total: view.rows_total,
        ready: view.ready,
        unreadable_count: view.unreadable.len(),
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_row(
    session: *const FunputConvertSession,
    index: usize,
) -> FunputConvertRow {
    safe(FunputConvertRow::default(), || {
        unsafe { session.as_ref() }
            .and_then(|s| s.inner.view().rows.get(index))
            .map_or(FunputConvertRow::default(), |row| FunputConvertRow {
                source: row
                    .charset
                    .and_then(|v| i32::try_from(v).ok())
                    .unwrap_or(-1),
            })
    })
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_out_dir(
    session: *const FunputConvertSession,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe { with_ref(session, |value| write_text(&value.view().out_dir, out, cap)) }
}
