//! Fixture text for the conversion suite.
//!
//! The legacy sides are written with `\u{..}` escapes rather than literal
//! characters on purpose: a legacy document's bytes render as Latin-1 punctuation
//! in any editor, so `"Vi\u{D6}t"` says what is actually stored and `"Viքt"` would
//! only confuse the next reader. The escapes are also what a reviewer can check
//! against the tables in `src/charset/codecs/*/table.rs` without running anything.
//!
//! Both legacy spellings sit on one row so a reviewer sees them together, and so
//! the tests loop over charsets instead of growing a parallel copy per charset.
//! Note how the columns differ in length: TCVN3 spells every letter in one
//! character, VNI-Windows spells most of them in two.

/// Text that converts exactly in every direction: `(Unicode, TCVN3, VNI-Windows)`.
pub const EXACT: &[(&str, &str, &str)] = &[
    ("Việt Nam", "Vi\u{D6}t Nam", "Vi\u{65}\u{E4}t Nam"),
    (
        "Chào bạn",
        "Ch\u{B5}o b\u{B9}n",
        "Ch\u{61}\u{F8}o b\u{61}\u{EF}n",
    ),
    (
        "đường",
        "\u{AE}\u{AD}\u{EA}ng",
        "\u{F1}\u{F6}\u{F4}\u{F8}ng",
    ),
    (
        "Nghị định",
        "Ngh\u{DE} \u{AE}\u{DE}nh",
        "Ngh\u{F2} \u{F1}\u{F2}nh",
    ),
    (
        "Cộng hòa Xã hội Chủ nghĩa Việt Nam",
        "C\u{E9}ng h\u{DF}a X\u{B7} h\u{E9}i Ch\u{F1} ngh\u{DC}a Vi\u{D6}t Nam",
        "C\u{6F}\u{E4}ng h\u{6F}\u{F8}a X\u{61}\u{F5} h\u{6F}\u{E4}i Ch\u{75}\u{FB} \
         ngh\u{F3}a Vi\u{65}\u{E4}t Nam",
    ),
    // No Vietnamese at all: both charsets are ASCII below 0x80, so this is a no-op.
    ("Ha Noi 1945", "Ha Noi 1945", "Ha Noi 1945"),
];

/// What TCVN3 cannot spell exactly: `(Unicode, TCVN3, unmapped, what it reads back
/// as)`.
///
/// Both kinds of loss are here, and they are the only two TCVN3 has: an uppercase
/// toned vowel, which it leaves to the `.VnTimeH` font and so comes back
/// lowercase, and a character with no code at all, which survives as itself.
pub const TCVN3_LOSSY: &[(&str, &str, usize, &str)] = &[
    ("ĐIỀU 5", "\u{A7}I\u{D2}U 5", 1, "ĐIềU 5"),
    ("GIÁ 5₫", "GI\u{B8} 5₫", 2, "GIá 5₫"),
    ("HÀ NỘI", "H\u{B5} N\u{E9}I", 2, "Hà NộI"),
];

/// What VNI-Windows cannot spell exactly — a much shorter list, and deliberately
/// **not** merged with the one above.
///
/// The two charsets lose different things, so one table would be a lie: `ĐIỀU 5`
/// is lossy in TCVN3 and exact in VNI, which is the whole point. VNI has no
/// uppercase gap at all, so the only loss left is a character that was never
/// Vietnamese.
pub const VNI_LOSSY: &[(&str, &str, usize, &str)] = &[
    ("GIÁ 5₫", "GI\u{41}\u{D9} 5₫", 1, "GIÁ 5₫"),
    // `ø` is not Vietnamese, so it passes through — and its code point is VNI's
    // huyền mark, so reading the result back fuses it onto the `a` before it.
    ("a\u{F8}", "a\u{F8}", 1, "à"),
];

/// The same phrases in Unicode tổ hợp: `(Unicode, tổ hợp)`.
///
/// A different escaping rule applies here, and it matters. The legacy columns
/// escape everything because their characters are Latin-1 mojibake that mean
/// nothing on their own. This column's *bases* are real letters, so they stay
/// literal — but every **combining mark** is always `\u{..}`, because `"ệ"` and
/// `"ê\u{323}"` are indistinguishable on screen. A fixture that got silently
/// precomposed would make these tests assert nothing at all.
pub const COMBINING_EXACT: &[(&str, &str)] = &[
    ("Việt Nam", "Viê\u{323}t Nam"),
    ("Chào bạn", "Cha\u{300}o ba\u{323}n"),
    ("đường", "đươ\u{300}ng"),
    ("Nghị định", "Nghi\u{323} đi\u{323}nh"),
    (
        "Cộng hòa Xã hội Chủ nghĩa Việt Nam",
        "Cô\u{323}ng ho\u{300}a Xa\u{303} hô\u{323}i Chu\u{309} nghi\u{303}a Viê\u{323}t Nam",
    ),
    ("Ha Noi 1945", "Ha Noi 1945"),
];

/// Spellings Unicode tổ hợp reads but does not write: `(input, what it becomes,
/// normalized)`.
///
/// There is no `COMBINING_LOSSY`. This charset is Unicode — it spells everything,
/// so its only imperfection is a source written another way, and that costs
/// nothing. `unmapped` is zero on every row here; the count that moves is
/// `normalized`.
pub const NON_CANONICAL: &[(&str, &str, usize)] = &[
    ("a\u{302}\u{301}", "â\u{301}", 1), // full NFD
    ("a\u{323}\u{302}", "â\u{323}", 1), // full NFD, tone first
    ("ấ", "â\u{301}", 1),               // precomposed
    ("Viê\u{323}t", "Viê\u{323}t", 0),  // already canonical
];
