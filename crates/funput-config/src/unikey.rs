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
mod trigger;

use std::fmt;
use std::path::Path;

use funput_core::charset::Charset;

use crate::transfer::PortableShortcut;
use trigger::normalize_trigger;

/// Why a macro table could not be turned into shortcut rows.
///
/// Separate from [`crate::transfer::ConfigError`] because that one's messages name
/// "cấu hình", which would read wrong for a gõ tắt file.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MacroError {
    /// The file could not be opened or read at all.
    Unreadable,
    /// Read, but no encoding explains the bytes.
    ///
    /// The legacy Vietnamese charsets **were** tried, so reaching this means
    /// detection declined — an encoding nobody has implemented, a genuine tie, or
    /// too little Vietnamese to judge. Not that guessing was never attempted.
    UnknownEncoding,
    /// Decoded, but held no usable `trigger:expansion` line.
    NoEntries,
}

impl fmt::Display for MacroError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let text = match self {
            Self::Unreadable => "Không đọc được tệp bảng gõ tắt.",
            // Naming the charsets that were tried would rot the day another is
            // added, and the advice is the same either way.
            Self::UnknownEncoding => {
                "Không nhận ra bảng mã của tệp này. Hãy mở lại bằng UniKey và ghi \
                 bảng gõ tắt ra tệp mới (Unicode)."
            }
            Self::NoEntries => "Tệp không có mục gõ tắt nào.",
        };
        f.write_str(text)
    }
}

impl std::error::Error for MacroError {}

/// What a macro file yielded.
///
/// `#[non_exhaustive]`: the shell will want the conversion's own counts the first
/// time a TCVN3 file with an uppercase toned vowel turns up, and that is a matter of
/// when rather than whether.
#[derive(Debug, PartialEq, Eq)]
#[non_exhaustive]
pub struct MacroImport {
    pub rows: Vec<PortableShortcut>,
    /// The charset the file turned out to be in, or `None` when nothing needed
    /// deciding — an ASCII table reads the same either way, and claiming Unicode
    /// there would invent a certainty nobody has.
    ///
    /// Worth showing the user: detection can be confidently wrong about an encoding
    /// nobody has implemented, and naming its guess is the only warning available.
    pub charset: Option<Charset>,
}

/// Read a UniKey macro file and turn it into shortcut rows.
pub fn read_macro_file(path: &Path) -> Result<MacroImport, MacroError> {
    let bytes = std::fs::read(path).map_err(|_| MacroError::Unreadable)?;
    let decoded = encoding::decode(&bytes).ok_or(MacroError::UnknownEncoding)?;
    let rows = parse_macros(&decoded.text);
    if rows.is_empty() {
        return Err(MacroError::NoEntries);
    }
    Ok(MacroImport {
        rows,
        charset: decoded.charset,
    })
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

#[cfg(test)]
mod tests;
