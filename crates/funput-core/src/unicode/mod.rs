// The eight combining marks are a fact about the language rather than about any
// encoding, so they live here with one reader today and a second one coming. Gated
// because nothing in the default keyboard build spells a letter that way.
#[cfg(feature = "charset")]
pub(crate) mod combining;
pub mod marks;
pub mod shapes;
// `charset` reads the vowel inventory through the accessors in `vowels` rather
// than keeping its own copy, so the module has to reach past `unicode`.
pub(crate) mod vowels;
