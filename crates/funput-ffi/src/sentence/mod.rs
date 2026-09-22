//! Where a sentence begins, across the C ABI.
//!
//! Stateless, so unlike every other door in this crate these take no handle: they
//! read the text a host hands them and answer about the position it ends at.
//!
//! A soft keyboard is what needs them. It draws its own Shift key, so it has to
//! know on every caret move whether that key should be up, and it cannot keep a
//! running answer — a caret also moves for a paste, a tap elsewhere, or a field
//! that opened with text already in it, none of which a keystroke explains. The
//! Android keyboard asks the same two questions through `funput-jni`.
//!
//! Not behind a cargo feature, unlike [`crate::charset`]: the keyboards build this
//! crate with default features only.

use funput_core::sentence::{self, Rules};

use crate::abi::{safe, string_from_utf32};

/// Whether a caret sitting after `text` starts a sentence.
///
/// An empty or null `text` is the start of the document, which is the start of a
/// sentence. Always applies the typing reading of a full stop, which treats a
/// repeated one as an abbreviation: `v.v. ` does not start a sentence, `TS. ` does.
///
/// # Safety
/// `text` must point to at least `text_len` readable `u32` values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_starts_sentence(text: *const u32, text_len: usize) -> bool {
    safe(false, || {
        // SAFETY: forwarded from this function's own contract.
        let text = unsafe { string_from_utf32(text, text_len) };
        sentence::starts_sentence(&text, Rules::TYPING)
    })
}

/// Whether a caret sitting after `text` starts a word.
///
/// True at the start of the document and after anything that is not a letter or a
/// digit, so after a space, a hyphen, or an opening bracket.
///
/// # Safety
/// `text` must point to at least `text_len` readable `u32` values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_starts_word(text: *const u32, text_len: usize) -> bool {
    safe(false, || {
        // SAFETY: forwarded from this function's own contract.
        let text = unsafe { string_from_utf32(text, text_len) };
        sentence::starts_word(&text)
    })
}

#[cfg(test)]
mod tests;
