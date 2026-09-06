//! Moving text on and off the clipboard.
//!
//! Slint keeps `set_clipboard_text` and `clipboard_text` on its `Platform` trait,
//! which the winit backend implements and application code cannot reach. Ctrl+C and
//! Ctrl+V inside a text box therefore work already; the two *buttons* do not, and
//! that is the whole reason for the Win32 calls.

use windows::Win32::Foundation::{HANDLE, HGLOBAL};
use windows::Win32::System::DataExchange::{
    CloseClipboard, EmptyClipboard, GetClipboardData, OpenClipboard, SetClipboardData,
};
use windows::Win32::System::Memory::{GHND, GlobalAlloc, GlobalLock, GlobalUnlock};
use windows::Win32::System::Ole::CF_UNICODETEXT;

/// Replace the clipboard with `text`. Silently does nothing if Windows refuses —
/// another process can hold the clipboard open, and a failed copy is not worth
/// interrupting the user over.
pub(in crate::ui::convert) fn write(text: &str) {
    // UTF-16 with the terminating NUL the format requires.
    let mut units: Vec<u16> = text.encode_utf16().collect();
    units.push(0);
    let bytes = std::mem::size_of_val(units.as_slice());

    // SAFETY: the clipboard is opened for this thread and closed on every path
    // below; nothing between the two calls can return early.
    unsafe {
        if OpenClipboard(None).is_err() {
            return;
        }
        if let Some(handle) = allocate(&units, bytes) {
            // Ownership of the handle passes to the clipboard on success, so it must
            // not be freed here. On failure Windows keeps nothing and the block is
            // abandoned — one lost allocation on a path that already failed.
            let _ = EmptyClipboard();
            let _ = SetClipboardData(CF_UNICODETEXT.0.into(), Some(HANDLE(handle.0)));
        }
        let _ = CloseClipboard();
    }
}

/// A moveable global block holding `units`, as `CF_UNICODETEXT` requires.
///
/// # Safety
/// Must be called with the clipboard open, so the caller owns the close.
unsafe fn allocate(units: &[u16], bytes: usize) -> Option<HGLOBAL> {
    // SAFETY: GHND zero-initialises; the size is the slice's own byte length.
    let handle = unsafe { GlobalAlloc(GHND, bytes) }.ok()?;
    // SAFETY: `handle` came from GlobalAlloc and is unlocked.
    let target = unsafe { GlobalLock(handle) };
    if target.is_null() {
        return None;
    }
    // SAFETY: the block is `bytes` long, which is exactly what is copied in.
    unsafe {
        std::ptr::copy_nonoverlapping(units.as_ptr(), target.cast::<u16>(), units.len());
        let _ = GlobalUnlock(handle);
    }
    Some(handle)
}

/// The clipboard's text, if it holds any.
///
/// Read only when the user asks — pressing "Dán văn bản" or, later, a hotkey. Funput
/// never watches the clipboard: it already holds a keyboard hook, and quietly reading
/// everything the user copies is not a thing to spend that trust on.
pub(in crate::ui::convert) fn read() -> Option<String> {
    // SAFETY: opened here and closed on every path out.
    unsafe {
        if OpenClipboard(None).is_err() {
            return None;
        }
        let text = read_open();
        let _ = CloseClipboard();
        text
    }
}

/// # Safety
/// Must be called with the clipboard open.
unsafe fn read_open() -> Option<String> {
    // SAFETY: the handle belongs to the clipboard and stays valid until it is closed;
    // it must not be freed here.
    let handle = unsafe { GetClipboardData(CF_UNICODETEXT.0.into()) }.ok()?;
    let block = HGLOBAL(handle.0);
    // SAFETY: `block` is the clipboard's own moveable block.
    let source = unsafe { GlobalLock(block) };
    if source.is_null() {
        return None;
    }
    // SAFETY: `CF_UNICODETEXT` is NUL-terminated UTF-16 by definition of the format.
    let text = unsafe { wide_string(source.cast::<u16>()) };
    // SAFETY: paired with the lock above.
    unsafe {
        let _ = GlobalUnlock(block);
    }
    Some(text)
}

/// # Safety
/// `ptr` must point at a NUL-terminated UTF-16 string.
unsafe fn wide_string(ptr: *const u16) -> String {
    let mut len = 0;
    // SAFETY: the format guarantees the terminator, so the walk ends.
    while unsafe { *ptr.add(len) } != 0 {
        len += 1;
    }
    // SAFETY: `len` units were just walked and found readable.
    String::from_utf16_lossy(unsafe { std::slice::from_raw_parts(ptr, len) })
}
