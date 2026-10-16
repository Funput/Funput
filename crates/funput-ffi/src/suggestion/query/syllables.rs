//! The shipped syllable bands, asked without a handle.
//!
//! Both calls read a table compiled into the library, so there is no engine to
//! hold and nothing to free: a host asks them beside `funput_suggestion_frequency`
//! for each candidate and adds the two numbers before `funput_engine_choose_correction`.
//! The text is decoded onto the stack, so the call allocates nothing after the
//! table's first use in the process.

use funput_suggestions::{syllable_tier, syllable_weight};

use super::super::engine::codepoints_from_raw;
use crate::abi::safe;

/// Four bytes per scalar of the longest token the suggestion store accepts; a
/// longer word is not a syllable on any list and answers as unlisted.
const WORD_BYTES: usize = 128;

/// How many pseudo-uses `word`'s band in the shipped syllable list is worth, and 0
/// for a word that is not on it — what a host adds to `funput_suggestion_frequency`
/// to form the `uses` entry of a candidate. Case and normalization do not matter.
///
/// No handle, no I/O, no allocation. Malformed text answers 0.
///
/// # Safety
/// `word` must point to `word_len` readable codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_syllable_weight(
    word: *const u32,
    word_len: usize,
) -> u32 {
    with_word(word, word_len, syllable_weight)
}

/// The band `word` sits in on the shipped syllable list: 1 for the most common,
/// 4 for the rare but real, and 0 for a word that is not on it.
///
/// No handle, no I/O, no allocation. Malformed text answers 0.
///
/// # Safety
/// `word` must point to `word_len` readable codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_syllable_tier(word: *const u32, word_len: usize) -> u32 {
    with_word(word, word_len, |text| {
        syllable_tier(text).map_or(0, u32::from)
    })
}

/// Decode the UTF-32 word onto the stack and hand it to `ask`; 0 when the pointer,
/// a codepoint, or the length is not something a syllable could be.
fn with_word(word: *const u32, word_len: usize, ask: impl FnOnce(&str) -> u32) -> u32 {
    safe(0, || {
        let Some(codepoints) = codepoints_from_raw(word, word_len) else {
            return 0;
        };
        let mut buffer = [0u8; WORD_BYTES];
        let mut len = 0;
        for codepoint in codepoints {
            let Some(scalar) = char::from_u32(*codepoint) else {
                return 0;
            };
            if len + scalar.len_utf8() > buffer.len() {
                return 0;
            }
            len += scalar.encode_utf8(&mut buffer[len..]).len();
        }
        std::str::from_utf8(&buffer[..len]).map_or(0, ask)
    })
}
