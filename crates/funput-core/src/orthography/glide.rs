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

use crate::unicode::marks::{tone_on_vowel, vowel_stem};

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
    // Consonant first: it is a one-byte compare that rejects almost every call,
    // and it keeps the vowel-table lookup off the common path. A leading
    // consonant can never carry a diacritic, so ASCII folding settles that side.
    let (expected, glide) = match before.as_bytes() {
        [b'q' | b'Q'] => ('u', Glide::Qu),
        [b'g' | b'G'] => ('i', Glide::Gi),
        _ => return None,
    };
    vowel_stem(vowel)?
        .eq_ignore_ascii_case(&expected)
        .then_some(glide)
}

/// The glide at byte `offset` of `buffer`, if the char there is one.
pub(crate) fn at(buffer: &str, offset: usize) -> Option<Glide> {
    // Cheap reject before either slice: only a one-char onset can be a glide's.
    if offset != 1 {
        return None;
    }
    let vowel = buffer[1..].chars().next()?;
    after(&buffer[..1], vowel)
}

/// The glide of a complete onset slice (`"qu"`, `"gi"`, and their toned
/// transients `"qú"`, `"gí"`), if it has one.
pub(crate) fn in_onset(onset: &str) -> Option<Glide> {
    let vowel = onset.chars().next_back()?;
    at(onset, onset.len() - vowel.len_utf8())
}

/// True when the onset's glide carries a tone.
///
/// Legal only *mid-composition*, as the transient a tone key typed before the
/// nucleus leaves behind (`gí` on the way to `giá`). A finished syllable never
/// looks like this — `qúy` is a misspelling of `quý` — so the word-boundary check
/// [`crate::is_complete_syllable`] rejects it.
pub(crate) fn onset_holds_tone(onset: &str) -> bool {
    in_onset(onset).is_some() && onset.chars().any(|ch| tone_on_vowel(ch).is_some())
}
