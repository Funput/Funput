//! Reading UniKey's gõ tắt table into Funput's shortcut rows.
//!
//! UniKey keeps its macros in `ukmacro.txt` beside the executable. The format is
//! not documented anywhere; this is what a real file from UniKey 4.6 RC2 contains,
//! byte for byte:
//!
//! ```text
//! EF BB BF                                   UTF-8 BOM
//! ;DO NOT DELETE THIS LINE*** version=1 ***  header, CRLF
//! vn:việt nam                                one pair, split at the first colon
//! ```
//!
//! Only the pair lines matter. The header is not required — a hand-edited file or
//! another UniKey build should still import — so anything starting with `;` is
//! simply skipped as a comment.
//!
//! What comes out is [`PortableShortcut`], the same shape Export/Import already
//! carries, so the caller can hand it to [`crate::transfer::apply`] and inherit the
//! merge rule and the counters instead of restating them.

mod encoding;

use std::fmt;
use std::path::Path;

use crate::transfer::PortableShortcut;

/// Why a macro table could not be turned into shortcut rows.
///
/// Separate from [`crate::transfer::ConfigError`] because that one's messages name
/// "cấu hình", which would read wrong for a gõ tắt file.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MacroError {
    /// The file could not be opened or read at all.
    Unreadable,
    /// Read, but the bytes are not text this can decode — most likely a legacy
    /// 8-bit Vietnamese encoding (TCVN3, VNI-Windows).
    UnknownEncoding,
    /// Decoded, but held no usable `trigger:expansion` line.
    NoEntries,
}

impl fmt::Display for MacroError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let text = match self {
            Self::Unreadable => "Không đọc được tệp bảng gõ tắt.",
            Self::UnknownEncoding => {
                "Bảng mã của tệp này không đọc được. Hãy mở lại bằng UniKey và ghi \
                 bảng gõ tắt ra tệp mới (Unicode)."
            }
            Self::NoEntries => "Tệp không có mục gõ tắt nào.",
        };
        f.write_str(text)
    }
}

impl std::error::Error for MacroError {}

/// Read a UniKey macro file and turn it into shortcut rows.
pub fn read_macro_file(path: &Path) -> Result<Vec<PortableShortcut>, MacroError> {
    let bytes = std::fs::read(path).map_err(|_| MacroError::Unreadable)?;
    let text = encoding::decode(&bytes).ok_or(MacroError::UnknownEncoding)?;
    let rows = parse_macros(&text);
    if rows.is_empty() {
        return Err(MacroError::NoEntries);
    }
    Ok(rows)
}

/// Parse the text of a macro table. Unusable lines are dropped, not reported: a
/// table is a list of independent pairs, and one odd line is no reason to refuse
/// the rest.
///
/// A trigger repeated inside one file keeps its last value, matching the
/// "incoming wins" rule the merge downstream applies between files.
pub fn parse_macros(text: &str) -> Vec<PortableShortcut> {
    let mut rows: Vec<PortableShortcut> = Vec::new();
    for line in text.trim_start_matches('\u{feff}').lines() {
        let Some(row) = parse_line(line) else {
            continue;
        };
        match rows.iter_mut().find(|kept| kept.trigger == row.trigger) {
            Some(kept) => kept.expansion = row.expansion,
            None => rows.push(row),
        }
    }
    rows
}

/// One `trigger:expansion` line, or `None` for a blank line, a `;` comment, a line
/// with no colon, or one whose sides are blank.
///
/// Dropping blank sides here is load-bearing, not tidiness: import writes straight
/// through `replace_settings` and never passes `ShellState`'s `is_complete` filter,
/// so an empty row would reach `settings.json` and the engine's table.
fn parse_line(line: &str) -> Option<PortableShortcut> {
    let line = line.trim();
    if line.is_empty() || line.starts_with(';') {
        return None;
    }
    // The first colon only: an expansion is free to contain more of them, which is
    // how `url:https://funput.app` keeps its scheme.
    let (trigger, expansion) = line.split_once(':')?;
    let trigger = normalize_trigger(trigger.trim());
    let expansion = expansion.trim();
    if trigger.is_empty() || expansion.is_empty() {
        return None;
    }
    Some(PortableShortcut {
        trigger,
        expansion: expansion.to_string(),
    })
}

/// Store the trigger the way the engine expects to find it.
///
/// `funput_engine`'s `classify_case` folds a typed word to lowercase before looking
/// it up whenever its letters form a clean pattern, so `vn`, `Vn` and `VN` all reach
/// a trigger stored as `vn` and re-case the expansion to match. A trigger stored in
/// any other casing is only ever found by an exact match — so folding is what makes
/// an imported `VN` usable, and *not* folding is what keeps a deliberate `iOS` from
/// becoming unreachable.
fn normalize_trigger(trigger: &str) -> String {
    if folds_to_lowercase(trigger) {
        trigger.to_lowercase()
    } else {
        trigger.to_string()
    }
}

/// Mirrors `classify_case` returning `Some`: letters only, digits and punctuation
/// ignored; first letter lowercase with the rest lowercase, or first letter
/// uppercase with the rest all one case. A trigger with no letters folds to itself.
fn folds_to_lowercase(trigger: &str) -> bool {
    let mut letters = trigger.chars().filter(|c| c.is_alphabetic());
    let Some(first) = letters.next() else {
        return true;
    };
    let rest: Vec<char> = letters.collect();
    if first.is_lowercase() {
        rest.iter().all(|c| c.is_lowercase())
    } else {
        rest.iter().all(|c| c.is_uppercase()) || rest.iter().all(|c| c.is_lowercase())
    }
}

#[cfg(test)]
mod tests;
