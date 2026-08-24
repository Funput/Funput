//! Charset-conversion C ABI: turning Vietnamese text between Unicode and the
//! legacy encodings, for a shell that cannot link `funput-core` directly.
//!
//! Behind the `charset` cargo feature, **off by default**. The iOS and Android
//! keyboards link this crate and have no use for the tables, so they never carry
//! them; a desktop shell asks for the feature and gets the whole door.
//!
//! # Naming a charset
//!
//! A charset is an **index into `funput_core::charset::ALL`**, not a name the host
//! spells for itself. `Charset` is `#[non_exhaustive]`, so no code outside
//! `funput-core` can enumerate it — a host writing its own list would silently miss
//! a charset added later. [`funput_charset_count`] and [`funput_charset_name`] are
//! between them enough to build a menu, and a charset added to core turns up in that
//! menu without a line changing here.
//!
//! **That list is append-only, and it is what makes the index an identity.** A host
//! may store the index as the user's saved choice. Reordering `ALL` would quietly
//! turn a saved TCVN3 into VNI-Windows, so core does not reorder it.
//!
//! # Text, not files
//!
//! Both entry points take text, matching `funput_core::charset::convert`. Core has a
//! second door for raw bytes and it is deliberately **not** here: a shell reading a
//! *file* needs the whole cascade — UTF-16 byte-order marks, a UTF-8 attempt, and
//! the rule that bytes which failed UTF-8 may only be answered with a byte-oriented
//! charset. Exporting those three primitives would hand the subtle part to the
//! caller and get it reimplemented once per platform, which is the duplication the
//! shared core exists to prevent. When a file-reading shell needs it, that cascade
//! belongs in core as one call, and this module gets one more export.

mod convert;
mod detect;

use funput_core::charset::{self, Charset};

use crate::abi::safe;

pub use convert::{FunputConversion, funput_charset_convert};
pub use detect::funput_charset_detect;

/// [`funput_charset_detect`] when the evidence does not pick a charset out — the
/// text is ASCII, or reads the same under every candidate. Not an error.
pub const FUNPUT_CHARSET_UNKNOWN: i32 = -1;

/// How many charsets there are. Valid indices run `0..funput_charset_count()`.
#[unsafe(no_mangle)]
pub extern "C" fn funput_charset_count() -> usize {
    charset::ALL.len()
}

/// Write the display name of the charset at `index` into `out` as UTF-32, returning
/// its length in codepoints. An index out of range gives 0.
///
/// The name comes from core so that two platforms' menus cannot drift apart. It is
/// the encoding's own name (`TCVN3 (ABC)`, `VNI-Windows`), which reads the same in
/// any interface language.
///
/// Sizing works the same way as [`funput_charset_convert`]: the length is returned
/// whether or not it fit, and nothing is written unless all of it fits.
///
/// # Safety
/// `out` must point to at least `cap` writable `u32` values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_charset_name(index: usize, out: *mut u32, cap: usize) -> usize {
    safe(0, || {
        let Some(charset) = charset_at(index) else {
            return 0;
        };
        unsafe { write_text(charset.name(), out, cap) }
    })
}

/// The charset an ABI index names, or `None` when the host passed one out of range.
pub(crate) fn charset_at(index: usize) -> Option<Charset> {
    charset::ALL.get(index).copied()
}

/// Write `text` into `out` as UTF-32, returning its length in codepoints — whether
/// or not it fit.
///
/// **All-or-nothing, unlike [`crate::abi::copy_codepoints`], which truncates.** That
/// one serves a 64-character composing buffer where a clipped tail is visible
/// immediately. Here the text is a document, and half of one written into a
/// too-small buffer is worse than none: the caller reads a success it did not get.
/// Returning the required length lets it size the buffer and call again.
///
/// # Safety
/// `out` must point to at least `cap` writable `u32` values, or be null.
pub(crate) unsafe fn write_text(text: &str, out: *mut u32, cap: usize) -> usize {
    let len = text.chars().count();
    if out.is_null() || cap < len {
        return len;
    }
    // SAFETY: the caller promises `cap` writable values, and `len <= cap` here.
    let dst = unsafe { std::slice::from_raw_parts_mut(out, len) };
    for (slot, ch) in dst.iter_mut().zip(text.chars()) {
        *slot = ch as u32;
    }
    len
}

#[cfg(test)]
mod tests;
