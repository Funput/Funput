//! UTF-32 ↔ `char` marshalling across the C ABI.
//!
//! The FFI carries text as `u32` codepoint arrays; these are the single truncating
//! copy / decode behind every buffer read, result, and shortcut write.

/// Write `chars` into `dst` as UTF-32, up to `dst.len()`; returns the count written.
/// The single truncating copy behind both `FunputResult::from_ime` and
/// `funput_buffer`.
pub(crate) fn copy_codepoints(dst: &mut [u32], chars: impl Iterator<Item = char>) -> usize {
    let mut n = 0;
    for ch in chars {
        if n >= dst.len() {
            break;
        }
        dst[n] = ch as u32;
        n += 1;
    }
    n
}

/// Decode UTF-32 codepoints into a `String`, skipping invalid scalars.
pub(crate) fn decode_codepoints(codepoints: &[u32]) -> String {
    codepoints
        .iter()
        .filter_map(|&c| char::from_u32(c))
        .collect()
}
