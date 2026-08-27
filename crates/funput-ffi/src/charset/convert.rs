//! Converting text from one charset to another across the C ABI.

use funput_core::charset;

use crate::abi::{safe, string_from_utf32, write_text};

use super::charset_at;

/// What a conversion produced. POD, returned by value — nothing is allocated and
/// there is nothing to free.
///
/// The two counters are separate because the problems are: `unmapped` is text that
/// came out **wrong or guessed at**, and is the number worth putting in front of a
/// user. `normalized` is text understood exactly whose spelling changed — converting
/// to Unicode tổ hợp changes every toned vowel and none of it is a loss.
#[repr(C)]
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct FunputConversion {
    /// Codepoints the conversion produced, whether or not they fit in `out`.
    pub len: usize,
    /// Characters the target charset could not represent, or represented by
    /// guessing. Non-zero means the result is worth showing the user before use.
    pub unmapped: usize,
    /// Characters understood exactly whose spelling the target charset writes
    /// differently. Not a loss, and converting back returns the original.
    pub normalized: usize,
}

/// Convert UTF-32 `text` from one charset to another, writing the result into `out`.
///
/// `from` and `to` are indices into the charset list — see the module doc. Either
/// one out of range yields a zeroed [`FunputConversion`], as does a panic anywhere
/// inside.
///
/// # Sizing
///
/// `len` is the length of the result whether or not it fit, and **nothing is written
/// unless all of it fits**. So a host either guesses a buffer generously and is done
/// in one call, or passes `out = NULL, cap = 0` to learn the length and calls again.
/// A conversion is not always shorter than its input: VNI-Windows spells most toned
/// letters as two characters, so a guess of `text_len` is not enough.
///
/// `unmapped` and `normalized` are filled in on the sizing call too, so a host can
/// warn about what will be lost before it allocates anything.
///
/// `from` must be what the text actually **is**; guessing it is
/// [`funput_charset_detect`](super::funput_charset_detect)'s job, not this one's.
///
/// # Safety
/// `text` must point to at least `text_len` readable `u32` values, or be null.
/// `out` must point to at least `cap` writable `u32` values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_charset_convert(
    text: *const u32,
    text_len: usize,
    from: usize,
    to: usize,
    out: *mut u32,
    cap: usize,
) -> FunputConversion {
    safe(FunputConversion::default(), || {
        let (Some(from), Some(to)) = (charset_at(from), charset_at(to)) else {
            return FunputConversion::default();
        };
        // SAFETY: forwarded from this function's own contract.
        let text = unsafe { string_from_utf32(text, text_len) };
        let converted = charset::convert(&text, from, to);
        FunputConversion {
            // SAFETY: likewise — `out` is the caller's, `cap` describes it.
            len: unsafe { write_text(&converted.text, out, cap) },
            unmapped: converted.unmapped,
            normalized: converted.normalized,
        }
    })
}
