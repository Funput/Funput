// The eight combining marks are a fact about the language rather than about any
// encoding, so they live here rather than in either reader: the charset codec that
// decodes Unicode tổ hợp, and `textcase`, where bỏ dấu drops a mark without knowing
// what a charset is. Gated because nothing in the default keyboard build spells a
// letter that way.
#[cfg(any(feature = "charset", feature = "textcase"))]
pub(crate) mod combining;
pub mod marks;
pub mod shapes;
// `charset` reads the vowel inventory through the accessors in `vowels` rather
// than keeping its own copy, so the module has to reach past `unicode`.
pub(crate) mod vowels;
