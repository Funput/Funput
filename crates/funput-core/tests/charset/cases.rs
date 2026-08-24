//! Fixture text for the conversion suite.
//!
//! The TCVN3 side is written with `\u{..}` escapes rather than literal characters
//! on purpose: a `.VnTime` document's bytes render as Latin-1 punctuation in any
//! editor, so `"Vi\u{D6}t"` says what is actually stored and `"Viքt"` would only
//! confuse the next reader. The escapes are also what a reviewer can check against
//! the table in `src/charset/codecs/tcvn3/table.rs` without running anything.

/// Text that converts exactly both ways: `(Unicode, TCVN3)`.
pub const EXACT: &[(&str, &str)] = &[
    ("Việt Nam", "Vi\u{D6}t Nam"),
    ("Chào bạn", "Ch\u{B5}o b\u{B9}n"),
    ("đường", "\u{AE}\u{AD}\u{EA}ng"),
    ("Nghị định", "Ngh\u{DE} \u{AE}\u{DE}nh"),
    (
        "Cộng hòa Xã hội Chủ nghĩa Việt Nam",
        "C\u{E9}ng h\u{DF}a X\u{B7} h\u{E9}i Ch\u{F1} ngh\u{DC}a Vi\u{D6}t Nam",
    ),
    // No Vietnamese at all: TCVN3 is ASCII below 0x80, so this is a no-op.
    ("Ha Noi 1945", "Ha Noi 1945"),
];

/// Text TCVN3 cannot spell exactly: `(Unicode, TCVN3, unmapped, what it reads back
/// as)`.
///
/// Both kinds of loss are here, and they are the only two kinds there are: an
/// uppercase toned vowel, which TCVN3 leaves to the `.VnTimeH` font and so comes
/// back lowercase, and a character with no TCVN3 code at all, which survives as
/// itself.
pub const LOSSY: &[(&str, &str, usize, &str)] = &[
    ("ĐIỀU 5", "\u{A7}I\u{D2}U 5", 1, "ĐIềU 5"),
    ("GIÁ 5₫", "GI\u{B8} 5₫", 2, "GIá 5₫"),
    ("HÀ NỘI", "H\u{B5} N\u{E9}I", 2, "Hà NộI"),
];
