//! Diacritic mechanics: tone placement, tone removal, and đ-stroke position.

mod support;

#[path = "diacritics/placement.rs"]
mod placement;
#[path = "diacritics/remove_tone.rs"]
mod remove_tone;
#[path = "diacritics/stroke.rs"]
mod stroke;
