//! Compile-time codepoint → vowel table behind the O(1) queries in [`super`].
//!
//! [`VOWEL_FAMILIES`] stays the single human-readable source of truth. At
//! compile time it is flattened into a dense byte table indexed by codepoint,
//! so per-keystroke lookups never scan the family list or case-fold.

/// The 12 vowel families, lowercase. Per row, index 0 is the toneless base and
/// 1–5 carry the tone marks in order: sắc, huyền, hỏi, ngã, nặng.
pub(super) const VOWEL_FAMILIES: [[char; 6]; 12] = [
    ['a', 'á', 'à', 'ả', 'ã', 'ạ'],
    ['ă', 'ắ', 'ằ', 'ẳ', 'ẵ', 'ặ'],
    ['â', 'ấ', 'ầ', 'ẩ', 'ẫ', 'ậ'],
    ['e', 'é', 'è', 'ẻ', 'ẽ', 'ẹ'],
    ['ê', 'ế', 'ề', 'ể', 'ễ', 'ệ'],
    ['i', 'í', 'ì', 'ỉ', 'ĩ', 'ị'],
    ['o', 'ó', 'ò', 'ỏ', 'õ', 'ọ'],
    ['ô', 'ố', 'ồ', 'ổ', 'ỗ', 'ộ'],
    ['ơ', 'ớ', 'ờ', 'ở', 'ỡ', 'ợ'],
    ['u', 'ú', 'ù', 'ủ', 'ũ', 'ụ'],
    ['ư', 'ứ', 'ừ', 'ử', 'ữ', 'ự'],
    ['y', 'ý', 'ỳ', 'ỷ', 'ỹ', 'ỵ'],
];

// Vietnamese precomposed vowels span three Unicode blocks: Basic Latin +
// Latin-1 (plain vowels, acute/grave/circumflex/tilde), Latin Extended-A/B
// (ă ĩ ũ ơ ư), and Latin Extended Additional (the rest). Two dense ranges
// cover all of them.
const BASIC_END: usize = 0x1B1; // one past 'ư' (U+01B0)
const EXTRA_START: usize = 0x1EA0; // 'Ạ'
const EXTRA_END: usize = 0x1EFA; // one past 'ỹ' (U+1EF9)
const SLOTS: usize = BASIC_END + (EXTRA_END - EXTRA_START);

static TABLE: [u8; SLOTS] = build();

/// A vowel's coordinates: family, index within it (0 = base, 1–5 = tone), case.
pub(super) struct Entry {
    pub(super) family: usize,
    pub(super) index: usize,
    pub(super) upper: bool,
}

/// Decode the table entry for `c`, or `None` when it is not a Vietnamese vowel.
pub(super) fn entry(c: char) -> Option<Entry> {
    let packed = match slot(c as usize) {
        Some(s) => TABLE[s],
        None => return None,
    };
    let v = packed.checked_sub(1)? as usize;
    Some(Entry {
        family: v / 12,
        index: (v % 12) / 2,
        upper: v % 2 == 1,
    })
}

/// The glyph at (family, index) in the requested case.
pub(super) fn glyph(family: usize, index: usize, upper: bool) -> char {
    let lower = VOWEL_FAMILIES[family][index];
    if upper { to_upper(lower) } else { lower }
}

/// Table slot for a codepoint, or `None` outside the two mapped ranges.
const fn slot(cp: usize) -> Option<usize> {
    if cp < BASIC_END {
        Some(cp)
    } else if cp >= EXTRA_START && cp < EXTRA_END {
        Some(BASIC_END + (cp - EXTRA_START))
    } else {
        None
    }
}

/// Uppercase counterpart of a [`VOWEL_FAMILIES`] char (not general Unicode):
/// Latin-1 uppers sit 0x20 below their lower; in the extended blocks the upper
/// is the direct predecessor. Cross-checked against `char::to_uppercase` in tests.
const fn to_upper(lower: char) -> char {
    let cp = lower as u32;
    let up = if cp < 0x100 { cp - 0x20 } else { cp - 1 };
    match char::from_u32(up) {
        Some(upper) => upper,
        None => lower,
    }
}

/// Pack (family, index, case) into a non-zero byte; 0 means "not a vowel".
const fn pack(family: usize, index: usize, upper: bool) -> u8 {
    1 + (family * 12 + index * 2 + upper as usize) as u8
}

const fn build() -> [u8; SLOTS] {
    let mut table = [0u8; SLOTS];
    let mut family = 0;
    while family < VOWEL_FAMILIES.len() {
        let mut index = 0;
        while index < 6 {
            let lower = VOWEL_FAMILIES[family][index];
            store(&mut table, lower, pack(family, index, false));
            store(&mut table, to_upper(lower), pack(family, index, true));
            index += 1;
        }
        family += 1;
    }
    table
}

const fn store(table: &mut [u8; SLOTS], ch: char, value: u8) {
    match slot(ch as usize) {
        Some(s) => table[s] = value,
        // Unreachable for the current inventory; a bad edit fails the build.
        None => panic!("vowel outside the mapped Unicode ranges"),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn every_glyph_round_trips_and_uppercases_correctly() {
        for (family, glyphs) in VOWEL_FAMILIES.iter().enumerate() {
            for (index, &ch) in glyphs.iter().enumerate() {
                let upper = to_upper(ch);
                assert_eq!(Some(upper), ch.to_uppercase().next(), "{ch}");
                for (want, case) in [(ch, false), (upper, true)] {
                    let e = entry(want).expect("vowel missing from table");
                    assert_eq!((e.family, e.index, e.upper), (family, index, case));
                    assert_eq!(glyph(family, index, case), want);
                }
            }
        }
    }

    #[test]
    fn non_vowels_have_no_entry() {
        for ch in ['b', 'Z', 'đ', 'Đ', '1', ' ', 'ǎ', '漢'] {
            assert!(entry(ch).is_none(), "{ch}");
        }
    }
}
