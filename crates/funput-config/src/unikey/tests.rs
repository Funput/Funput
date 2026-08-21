use std::fs;

use funput_core::charset::Charset;

use super::*;
use crate::test_support::unique_dir;

/// Write `bytes` to a scratch file and read it back through the real entry point,
/// so BOM handling and decoding are exercised rather than bypassed.
fn read_bytes(bytes: &[u8]) -> Result<MacroImport, MacroError> {
    let dir = unique_dir("unikey");
    let path = dir.join("ukmacro.txt");
    fs::write(&path, bytes).expect("write scratch macro file");
    let result = read_macro_file(&path);
    let _ = fs::remove_dir_all(&dir);
    result
}

fn pairs(rows: &[PortableShortcut]) -> Vec<(&str, &str)> {
    rows.iter()
        .map(|r| (r.trigger.as_str(), r.expansion.as_str()))
        .collect()
}

/// The 59 bytes of a stock UniKey 4.6 RC2 `ukmacro.txt`, exactly as measured.
const SAMPLE: &[u8] =
    b"\xEF\xBB\xBF;DO NOT DELETE THIS LINE*** version=1 ***\r\nvn:vi\xE1\xBB\x87t nam";

#[test]
fn the_stock_unikey_file_yields_its_one_pair() {
    let rows = read_bytes(SAMPLE);
    let rows = rows.expect("the stock file must import").rows;
    assert_eq!(pairs(&rows), vec![("vn", "việt nam")]);
}

#[test]
fn the_bom_never_leaks_into_the_first_trigger() {
    // A stray U+FEFF on the trigger would make it unmatchable while looking right
    // in the settings list — the kind of bug that costs an afternoon.
    let rows = read_bytes(b"\xEF\xBB\xBFvn:viet nam");
    let rows = rows.expect("import").rows;
    assert_eq!(rows[0].trigger, "vn");
}

#[test]
fn comments_and_blank_lines_are_skipped() {
    let rows = parse_macros(";header\n\nvn:việt nam\n   \n; another comment\nct:công ty\n");
    assert_eq!(pairs(&rows), vec![("vn", "việt nam"), ("ct", "công ty")]);
}

#[test]
fn an_expansion_may_contain_colons() {
    // Split on the first colon only, or every URL in the table loses its scheme.
    let rows = parse_macros("url:https://funput.app\n");
    assert_eq!(pairs(&rows), vec![("url", "https://funput.app")]);
}

#[test]
fn lines_with_a_blank_side_are_dropped() {
    // The only guard there is: import writes through `replace_settings` and never
    // meets `ShellState::is_complete`, so a blank row would reach settings.json.
    let rows = parse_macros(":nothing\nvn:\n   :   \nno colon here\nok:fine\n");
    assert_eq!(pairs(&rows), vec![("ok", "fine")]);
}

#[test]
fn whitespace_around_the_separator_is_trimmed() {
    let rows = parse_macros("  vn  :  việt nam  \n");
    assert_eq!(pairs(&rows), vec![("vn", "việt nam")]);
}

#[test]
fn a_clean_case_pattern_folds_so_the_engine_can_smart_case_it() {
    let rows = parse_macros("VN:việt nam\nCt:công ty\nhn:hà nội\n");
    assert_eq!(
        pairs(&rows),
        vec![("vn", "việt nam"), ("ct", "công ty"), ("hn", "hà nội")]
    );
}

#[test]
fn a_deliberately_mixed_trigger_is_left_alone() {
    // `classify_case` gives up on these and looks them up verbatim, so folding
    // would store a row nothing can ever match.
    let rows = parse_macros("iOS:hệ điều hành iOS\nvNa:gì đó\n");
    assert_eq!(
        pairs(&rows),
        vec![("iOS", "hệ điều hành iOS"), ("vNa", "gì đó")]
    );
}

#[test]
fn digits_do_not_stop_a_trigger_from_folding() {
    let rows = parse_macros("K1:kho một\n");
    assert_eq!(pairs(&rows), vec![("k1", "kho một")]);
}

#[test]
fn a_repeated_trigger_keeps_its_last_value() {
    // Same rule the cross-file merge uses, applied within one file.
    let rows = parse_macros("vn:cũ\nvn:mới\n");
    assert_eq!(pairs(&rows), vec![("vn", "mới")]);
}

#[test]
fn folding_makes_two_spellings_of_one_trigger_collide() {
    let rows = parse_macros("vn:thường\nVN:hoa\n");
    assert_eq!(pairs(&rows), vec![("vn", "hoa")]);
}

#[test]
fn crlf_and_lf_parse_the_same() {
    let crlf = parse_macros("vn:việt nam\r\nct:công ty\r\n");
    let lf = parse_macros("vn:việt nam\nct:công ty\n");
    assert_eq!(pairs(&crlf), pairs(&lf));
}

#[test]
fn a_utf16_file_decodes_through_its_bom() {
    let mut bytes = vec![0xFF, 0xFE];
    for unit in "vn:việt nam".encode_utf16() {
        bytes.extend_from_slice(&unit.to_le_bytes());
    }
    let rows = read_bytes(&bytes);
    assert_eq!(pairs(&rows.expect("import").rows), vec![("vn", "việt nam")]);
}

#[test]
fn a_file_with_nothing_usable_is_an_error() {
    let rows = read_bytes(b"\xEF\xBB\xBF;DO NOT DELETE THIS LINE*** version=1 ***\r\n");
    assert_eq!(rows, Err(MacroError::NoEntries));
}

/// Detection now runs, and declines. `0xFF` is in no charset's table, so every
/// candidate scores alike and none wins outright.
///
/// This one survives on a three-word majority, so the test below fails for the
/// sturdier reason — a tie no amount of extra text can break.
#[test]
fn a_file_no_charset_explains_is_still_refused() {
    let rows = read_bytes(b"vn:vi\xFFt nam");
    assert_eq!(rows, Err(MacroError::UnknownEncoding));
}

#[test]
fn english_text_ties_every_candidate_and_is_refused() {
    let rows = read_bytes(b"note:the quick brown fox \xFF jumps over the lazy dog");
    assert_eq!(rows, Err(MacroError::UnknownEncoding));
}

#[test]
fn a_missing_file_is_unreadable() {
    let dir = unique_dir("unikey-missing");
    let result = read_macro_file(&dir.join("nope.txt"));
    let _ = fs::remove_dir_all(&dir);
    assert_eq!(result, Err(MacroError::Unreadable));
}

/// **The reason this feature exists.** A table written before Unicode used to be
/// refused with a message telling the user to go re-export from UniKey.
#[test]
fn a_tcvn3_table_imports_and_names_its_charset() {
    let file = b"vn:vi\xD6t nam\r\nhn:h\xB5 n\xE9i\r\n";
    let import = read_bytes(file).expect("a TCVN3 table must import");
    assert_eq!(
        pairs(&import.rows),
        vec![("vn", "việt nam"), ("hn", "hà nội")]
    );
    assert_eq!(import.charset, Some(Charset::Tcvn3));
}

#[test]
fn a_vni_table_imports_and_names_its_charset() {
    let file = b"vn:vie\xE4t nam\r\nhn:ha\xF8 no\xE4i\r\n";
    let import = read_bytes(file).expect("a VNI table must import");
    assert_eq!(
        pairs(&import.rows),
        vec![("vn", "việt nam"), ("hn", "hà nội")]
    );
    assert_eq!(import.charset, Some(Charset::VniWindows));
}

/// **The regression net for the worst thing this change could have done.**
///
/// A UTF-8 file with one continuation byte lost is not any charset. Detection would
/// happily answer `Unicode` — the surviving words still parse — and lossy decoding
/// would then import a row containing `U+FFFD`, straight into `settings.json`.
/// Bytes that failed a UTF-8 decode may only be answered with a byte-oriented
/// charset, and refusing beats importing damage.
#[test]
fn broken_utf8_is_refused_rather_than_imported_with_replacement_characters() {
    // `ệ` is E1 BB 87 and its last byte is gone, so the sequence runs into the `t`.
    // No assertion that this is invalid UTF-8: `invalid_from_utf8` proves it at
    // compile time, and writing the check anyway fails the build.
    let broken = b"vn:vi\xE1\xBBt nam\nhn:h\xC3\xA0 n\xE1\xBB\x99i\n";
    assert_eq!(read_bytes(broken), Err(MacroError::UnknownEncoding));
}

/// A file in Unicode tổ hợp is valid UTF-8 already, so it used to import with its
/// combining marks intact — expansions that render correctly but never match what
/// Funput itself types.
#[test]
fn a_combining_table_is_normalised_to_precomposed() {
    let file = "vn:viê\u{323}t nam".as_bytes();
    let import = read_bytes(file).expect("import");
    assert_eq!(pairs(&import.rows), vec![("vn", "việt nam")]);
    assert_eq!(import.charset, Some(Charset::UnicodeCombining));
}

/// A UTF-8 BOM is a hint, not a verdict — editors write one whatever follows. It is
/// stripped, and the rest is judged on its own.
#[test]
fn a_utf8_bom_in_front_of_legacy_bytes_does_not_block_detection() {
    let file = b"\xEF\xBB\xBFvn:vi\xD6t nam\r\nhn:h\xB5 n\xE9i\r\n";
    let import = read_bytes(file).expect("import");
    assert_eq!(import.charset, Some(Charset::Tcvn3));
}

/// Nothing to decide, so nothing is claimed. Reporting `Unicode` here would invent
/// a certainty the file does not carry.
#[test]
fn an_ascii_table_imports_unchanged_and_names_no_charset() {
    let import = read_bytes(b"vn:viet nam\nhn:ha noi\n").expect("import");
    assert_eq!(
        pairs(&import.rows),
        vec![("vn", "viet nam"), ("hn", "ha noi")]
    );
    assert_eq!(import.charset, None);
}

/// **What judging only the values buys.** Triggers are keys — `vn`, `url`, `sdt` —
/// and none is a Vietnamese syllable. Counting them drags every candidate below the
/// detector's majority rule, and a table mixing Vietnamese with URLs and phone
/// numbers stops being detectable at all. Judging the right-hand side is not a
/// workaround; it is the right question.
#[test]
fn a_table_mixing_vietnamese_with_urls_is_still_detected() {
    let file = b"vn:vi\xD6t nam\nurl:https://funput.app\nsdt:0912345678\nhn:h\xB5 n\xE9i\n";
    let import = read_bytes(file).expect("import");
    assert_eq!(import.charset, Some(Charset::Tcvn3));
    assert_eq!(import.rows.len(), 4);
}

/// Emptiness beats encoding: a file with no pairs is `NoEntries`, never a charset
/// complaint. Pins the ordering inside `read_macro_file`.
#[test]
fn a_file_with_no_pairs_is_reported_as_empty_not_as_an_encoding_problem() {
    assert_eq!(read_bytes(b""), Err(MacroError::NoEntries));
    assert_eq!(read_bytes(b";just a comment\n"), Err(MacroError::NoEntries));
}

/// **A known limitation, pinned rather than hidden.**
///
/// VISCII shares Latin-1's letters with TCVN3 and nobody has implemented it, so
/// detection answers with the nearest charset it does know. `cà phê` imports as
/// `cà phờ`. The charset report exists precisely so the user is told which bảng mã
/// was assumed; the real fix is implementing VISCII, in `funput-core`.
#[test]
fn a_charset_nobody_implemented_imports_as_its_nearest_neighbour() {
    let import = read_bytes(b"cf:c\xE0 ph\xEA").expect("import");
    assert_eq!(import.charset, Some(Charset::Tcvn3));
    assert_eq!(pairs(&import.rows), vec![("cf", "cà phờ")]);
}
