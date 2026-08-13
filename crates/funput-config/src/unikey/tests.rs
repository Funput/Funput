use std::fs;

use super::*;
use crate::test_support::unique_dir;

/// Write `bytes` to a scratch file and read it back through the real entry point,
/// so BOM handling and decoding are exercised rather than bypassed.
fn read_bytes(bytes: &[u8]) -> Result<Vec<PortableShortcut>, MacroError> {
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
    let rows = rows.expect("the stock file must import");
    assert_eq!(pairs(&rows), vec![("vn", "việt nam")]);
}

#[test]
fn the_bom_never_leaks_into_the_first_trigger() {
    // A stray U+FEFF on the trigger would make it unmatchable while looking right
    // in the settings list — the kind of bug that costs an afternoon.
    let rows = read_bytes(b"\xEF\xBB\xBFvn:viet nam");
    let rows = rows.expect("import");
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
    assert_eq!(pairs(&rows.expect("import")), vec![("vn", "việt nam")]);
}

#[test]
fn a_file_with_nothing_usable_is_an_error() {
    let rows = read_bytes(b"\xEF\xBB\xBF;DO NOT DELETE THIS LINE*** version=1 ***\r\n");
    assert_eq!(rows, Err(MacroError::NoEntries));
}

#[test]
fn a_legacy_encoding_is_named_rather_than_guessed_at() {
    // 0xFF is not valid UTF-8 and carries no BOM: report it instead of importing
    // mojibake the user would have to hunt down row by row.
    let rows = read_bytes(b"vn:vi\xFFt nam");
    assert_eq!(rows, Err(MacroError::UnknownEncoding));
}

#[test]
fn a_missing_file_is_unreadable() {
    let dir = unique_dir("unikey-missing");
    let result = read_macro_file(&dir.join("nope.txt"));
    let _ = fs::remove_dir_all(&dir);
    assert_eq!(result, Err(MacroError::Unreadable));
}
