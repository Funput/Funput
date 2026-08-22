//! Guessing which charset a piece of text is written in, across the C ABI.

use funput_core::charset;

use crate::abi::{safe, string_from_utf32};

use super::FUNPUT_CHARSET_UNKNOWN;

/// Guess which charset `text` is written in. Returns an index into the charset
/// list, or [`FUNPUT_CHARSET_UNKNOWN`] when the evidence does not pick one out.
///
/// `FUNPUT_CHARSET_UNKNOWN` is not a failure and covers three honest situations: no
/// evidence at all (ASCII, digits), a genuine tie between charsets that spell this
/// text the same way, and text that reads identically under every one of them. A
/// host should offer the user the list rather than treat it as an error.
///
/// Detection reads a prefix rather than a whole document, so passing the whole text
/// costs nothing. A host that detects on a prefix and converts the whole thing is
/// doing the right thing.
///
/// # Safety
/// `text` must point to at least `text_len` readable `u32` values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_charset_detect(text: *const u32, text_len: usize) -> i32 {
    safe(FUNPUT_CHARSET_UNKNOWN, || {
        // SAFETY: forwarded from this function's own contract.
        let text = unsafe { string_from_utf32(text, text_len) };
        let Some(detected) = charset::detect(&text) else {
            return FUNPUT_CHARSET_UNKNOWN;
        };
        // The charset came from `ALL`, so it is always in it; the fallback is
        // unreachable rather than a policy.
        charset::ALL
            .iter()
            .position(|&c| c == detected)
            .and_then(|index| i32::try_from(index).ok())
            .unwrap_or(FUNPUT_CHARSET_UNKNOWN)
    })
}
