pub mod marks;
pub mod shapes;
// `charset` reads the vowel inventory through the accessors in `vowels` rather
// than keeping its own copy, so the module has to reach past `unicode`.
pub(crate) mod vowels;
