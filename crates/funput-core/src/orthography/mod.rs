//! Vietnamese syllable structure — the orthographic layer.
//!
//! A syllable (âm tiết) is `onset + medial + nucleus + coda`, with the tone
//! (thanh điệu) riding on one nucleus vowel:
//!
//! ```text
//!   q u  y ê   n        onset  (âm đầu)   q
//!   ─ ─  ─ ─   ─        medial (âm đệm)   u   ← belongs to the onset, never toned
//!   │ │  └─┼───┼──      nucleus (âm chính) yê  ← the tone lands here
//!   │ └────┼───┼──      coda   (âm cuối)  n
//! ```
//!
//! This module owns the *structural* rules (which vowel is the medial glide,
//! where the nucleus starts, which vowel carries the tone). It sits above
//! [`crate::unicode`] — pure glyph tables — and below [`crate::validation`] and
//! [`crate::composition`], which both consume it. Keeping the rules here is what
//! stops each consumer from re-deriving them, which is how `gi`/`qu` placement
//! drifted apart in the first place.

pub(crate) mod glide;

mod cluster;
mod tone_position;

pub(crate) use tone_position::{reposition_existing_tone, tone_target_vowel, tone_vowel_index};

#[cfg(test)]
mod tests;
