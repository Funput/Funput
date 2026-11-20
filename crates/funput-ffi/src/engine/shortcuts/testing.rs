//! What the gõ tắt C ABI tests share: building a table, typing into the engine, and
//! receiving UTF-8 through the callback every text-returning call uses.

use std::ffi::c_void;

use crate::*;

/// The `receive` callback: overwrites the `String` behind `context` with the text.
pub(super) unsafe extern "C" fn collect(context: *mut c_void, bytes: *const u8, len: usize) {
    unsafe {
        let output = &mut *context.cast::<String>();
        *output = String::from_utf8(std::slice::from_raw_parts(bytes, len).to_vec()).unwrap();
    }
}

pub(super) fn add(engine: *mut FunputEngine, trigger: &str, expansion: &str) {
    let trigger: Vec<u32> = trigger.chars().map(u32::from).collect();
    let expansion: Vec<u32> = expansion.chars().map(u32::from).collect();
    unsafe {
        funput_add_shortcut(
            engine,
            trigger.as_ptr(),
            trigger.len(),
            expansion.as_ptr(),
            expansion.len(),
        );
    }
}

pub(super) fn type_keys(engine: *mut FunputEngine, keys: &str) {
    for key in keys.chars() {
        unsafe { funput_process_char(engine, key as u32) };
    }
}

/// The word being composed, as the host reads it.
pub(super) fn buffer(engine: *const FunputEngine) -> String {
    let mut scalars = [0u32; CHARS_CAP];
    let n = unsafe { funput_buffer(engine, scalars.as_mut_ptr(), CHARS_CAP) };
    scalars[..n]
        .iter()
        .filter_map(|&c| char::from_u32(c))
        .collect()
}
