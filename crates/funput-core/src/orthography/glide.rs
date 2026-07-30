//! Âm đệm — the medial glide of a `qu` / `gi` onset.
//!
//! The `u` of `qu` and the `i` of `gi` are written like vowels but belong to the
//! **onset**: they never carry the tone (`quá`, not `qúa`; `giá`, not `gía`) and
//! they are not part of the rhyme (`quán` rhymes `an`, not `uan`).
//!
//! Two properties are easy to get wrong when this is re-derived ad hoc, and both
//! are settled here once:
//!
//! - **The glide may already carry a tone.** A tone key pressed before the
//!   nucleus parks on the glide as a transient (`gi` + `s` → `gí`) and must be
//!   moved once the nucleus arrives (`gí` + `a` → `giá`). So the vowel is matched
//!   on its stem, tone-blind by construction.
//! - **`q`/`g` must be the whole onset.** `ngi` is `ng` + nucleus `i`
//!   (`nghĩa`-style), not a `gi` onset that happens to end in `g` + `i`.

use crate::unicode::marks::vowel_stem;

/// Which onset the glide belongs to.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Glide {
    /// The `u` of `qu` (`qua`, `quyên`).
    Qu,
    /// The `i` of `gi` (`gia`, `giết`).
    Gi,
}

/// The glide `vowel` forms when the text before it inside the syllable is
/// `before`, if any.
///
/// The stem match keeps the vowel *shape*, which is what rejects `ư`:
/// `vowel_stem('ứ')` is `ư`, never `u`, so `qư` is not a `qu` glide.
pub(crate) fn after(before: &str, vowel: char) -> Option<Glide> {
    let stem = vowel_stem(vowel)?;
    // A leading consonant can never carry a diacritic, so ASCII folding is the
    // whole story on that side.
    if before.eq_ignore_ascii_case("q") && stem.eq_ignore_ascii_case(&'u') {
        Some(Glide::Qu)
    } else if before.eq_ignore_ascii_case("g") && stem.eq_ignore_ascii_case(&'i') {
        Some(Glide::Gi)
    } else {
        None
    }
}

/// The glide at byte `offset` of `buffer`, if the char there is one.
pub(crate) fn at(buffer: &str, offset: usize) -> Option<Glide> {
    let vowel = buffer[offset..].chars().next()?;
    after(&buffer[..offset], vowel)
}

/// The glide of a complete onset slice (`"qu"`, `"gi"`, and their toned
/// transients `"qú"`, `"gí"`), if it has one.
pub(crate) fn in_onset(onset: &str) -> Option<Glide> {
    let vowel = onset.chars().next_back()?;
    at(onset, onset.len() - vowel.len_utf8())
}
