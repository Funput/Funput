//! Vietnamese vowel queries (base + tone variants), O(1) per call.
//!
//! These run on every keystroke — often once per buffer character — so each
//! query is a single lookup in the compile-time table built in [`table`]: no
//! family-list scan, no case folding, no allocation. The human-readable vowel
//! inventory lives in [`table`] as the single source of truth.

mod table;

use table::{entry, glyph};

/// Returns true if `c` is a vowel (ASCII or precomposed Vietnamese).
pub fn is_vowel(c: char) -> bool {
    entry(c).is_some()
}

/// Strip tone from a vowel, keeping shape (e.g. `á` → `a`, `ấ` → `â`).
pub fn vowel_stem(c: char) -> Option<char> {
    let e = entry(c)?;
    Some(glyph(e.family, 0, e.upper))
}

/// Apply tone by index: 0 = sắc … 4 = nặng. `base` must be toneless (`a`, `ơ`,
/// …); a vowel already carrying a tone yields `None`.
pub fn toned_vowel(base: char, tone_index: usize) -> Option<char> {
    let e = entry(base)?;
    if e.index != 0 || tone_index >= 5 {
        return None;
    }
    Some(glyph(e.family, tone_index + 1, e.upper))
}

/// Tone index on a vowel, if any (0 = sắc … 4 = nặng).
pub fn tone_index_on_vowel(c: char) -> Option<usize> {
    entry(c)?.index.checked_sub(1)
}

/// How many families the inventory holds, derived from the table rather than
/// written down twice. Charset tables index by family, so each one asserts its
/// own row count against this — adding a 13th family becomes a build error
/// instead of a silent mis-encoding.
#[cfg(feature = "charset")]
pub(crate) const FAMILY_COUNT: usize = table::VOWEL_FAMILIES.len();

/// A vowel's coordinates: family, slot within it (0 = toneless base, 1–5 = sắc,
/// huyền, hỏi, ngã, nặng), and case.
///
/// This and [`vowel_glyph`] are the whole of what charset mapping needs from the
/// inventory. Every legacy Vietnamese encoding assigns its codes to exactly these
/// coordinates, which is what lets a charset table be twelve rows of six rather
/// than a list of sixty-seven glyphs.
#[cfg(feature = "charset")]
pub(crate) fn vowel_coords(c: char) -> Option<(usize, usize, bool)> {
    let e = entry(c)?;
    Some((e.family, e.index, e.upper))
}

/// The glyph at a set of coordinates — the inverse of [`vowel_coords`].
#[cfg(feature = "charset")]
pub(crate) fn vowel_glyph(family: usize, index: usize, upper: bool) -> Option<char> {
    (family < FAMILY_COUNT && index < 6).then(|| glyph(family, index, upper))
}

#[cfg(test)]
mod tests {
    use super::table::VOWEL_FAMILIES;
    use super::*;

    #[test]
    fn table_covers_all_stems_and_tones() {
        for family in VOWEL_FAMILIES {
            let (base, tones) = (family[0], &family[1..]);
            assert!(is_vowel(base));
            for (i, &toned) in tones.iter().enumerate() {
                assert!(is_vowel(toned));
                assert_eq!(vowel_stem(toned), Some(base));
                assert_eq!(toned_vowel(base, i), Some(toned));
                assert_eq!(tone_index_on_vowel(toned), Some(i));
            }
        }
    }

    #[test]
    fn uppercase_round_trip() {
        assert_eq!(toned_vowel('A', 0), Some('Á'));
        assert_eq!(vowel_stem('Ắ'), Some('Ă'));
        assert_eq!(tone_index_on_vowel('Ứ'), Some(0));
    }

    #[test]
    fn toned_input_rejected_by_toned_vowel() {
        // `toned_vowel` takes a toneless base; re-toning goes through
        // `vowel_stem` first (see `marks::apply_tone_to_vowel`).
        assert_eq!(toned_vowel('á', 1), None);
        assert_eq!(toned_vowel('a', 5), None); // tone index out of range
    }

    #[test]
    fn non_vowels_rejected() {
        for ch in ['b', 'đ', 'Đ', '9', ' '] {
            assert!(!is_vowel(ch), "{ch}");
            assert_eq!(vowel_stem(ch), None, "{ch}");
            assert_eq!(tone_index_on_vowel(ch), None, "{ch}");
        }
    }
}
