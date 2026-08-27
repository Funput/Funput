use crate::abi::{string_from_utf32, write_bytes, write_text};

use super::handle::{FunputConvertSession, with_mut, with_ref};

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_set_input(
    session: *mut FunputConvertSession,
    text: *const u32,
    len: usize,
) {
    let input = unsafe { string_from_utf32(text, len) };
    unsafe { with_mut(session, |value| value.set_input(input)) };
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_output_preview(
    session: *const FunputConvertSession,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe {
        with_ref(session, |value| {
            write_text(&value.view().output_preview, out, cap)
        })
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_warning(
    session: *const FunputConvertSession,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe { with_ref(session, |value| write_text(&value.view().warning, out, cap)) }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_file_name(
    session: *const FunputConvertSession,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe {
        with_ref(session, |value| {
            write_text(value.view().file_name.as_deref().unwrap_or(""), out, cap)
        })
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_input_preview(
    session: *const FunputConvertSession,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe {
        with_ref(session, |value| {
            write_text(
                value.view().input_preview.as_deref().unwrap_or(""),
                out,
                cap,
            )
        })
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_unreadable_line(
    session: *const FunputConvertSession,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe {
        with_ref(session, |value| {
            write_text(
                &funput_convert::unreadable_line(&value.view().unreadable),
                out,
                cap,
            )
        })
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_row_name(
    session: *const FunputConvertSession,
    index: usize,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe {
        with_ref(session, |value| {
            value
                .view()
                .rows
                .get(index)
                .map_or(0, |row| write_text(&row.name, out, cap))
        })
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_row_note(
    session: *const FunputConvertSession,
    index: usize,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe {
        with_ref(session, |value| {
            value
                .view()
                .rows
                .get(index)
                .map_or(0, |row| write_text(&row.note, out, cap))
        })
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_result_text(
    session: *const FunputConvertSession,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe {
        with_ref(session, |value| {
            value
                .result_text()
                .map_or(0, |text| write_text(&text, out, cap))
        })
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_save_bytes(
    session: *const FunputConvertSession,
    out: *mut u8,
    cap: usize,
) -> usize {
    unsafe {
        with_ref(session, |value| {
            value
                .save_bytes()
                .map_or(0, |bytes| write_bytes(&bytes, out, cap))
        })
    }
}
