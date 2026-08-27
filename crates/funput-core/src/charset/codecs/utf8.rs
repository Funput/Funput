//! Accounting for what lossy UTF-8 decoding destroyed.
//!
//! Only [`super::decode_bytes`] needs this, and only on the branch where the bytes
//! were supposed to be UTF-8. It lives apart because it is a different subject from
//! the rest of `codecs`: nothing here knows what a charset is.

/// How many characters lossy UTF-8 decoding had to replace. Zero for valid input,
/// and replacement characters the source genuinely encoded are discounted so a
/// document that legitimately contains one is not reported as damaged.
pub(super) fn broken_sequences(bytes: &[u8]) -> usize {
    if std::str::from_utf8(bytes).is_ok() {
        return 0;
    }
    String::from_utf8_lossy(bytes)
        .matches(char::REPLACEMENT_CHARACTER)
        .count()
        .saturating_sub(genuine_replacements(bytes))
}

/// How many `U+FFFD` the bytes genuinely encode.
fn genuine_replacements(bytes: &[u8]) -> usize {
    bytes
        .windows(3)
        .filter(|window| *window == [0xEF, 0xBF, 0xBD])
        .count()
}
