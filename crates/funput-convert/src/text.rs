//! Saying what a conversion costs, in the words a window uses.
//!
//! The measuring is [`charset::Cost`]'s, in core, because a terminal needs the same
//! numbers and words them differently. What is here is the sentence — one copy, so
//! the window on Windows and the window on Linux cannot word it for themselves.

use funput_core::charset::{Charset, Cost};

use crate::Unreadable;

/// How many characters to name before the warning gets longer than it is useful.
const NAMED: usize = 6;

/// How much of a document to put in a pane.
///
/// Panes are for *looking* at, and nobody reads a megabyte in one. The conversion and
/// the warning still run over the whole document — only the display is cut, so a large
/// file cannot make the window crawl while the counts stay honest.
const PREVIEW: usize = 20_000;

/// The warning line, or empty when nothing will be lost.
///
/// **Two different problems, and they are not the same sentence.** Reading can fail —
/// characters the *source* charset does not define, which means the wrong source was
/// picked or the document is damaged, and the user can act on that. Writing can fail —
/// characters the *target* cannot represent, which is a cost to accept or avoid by
/// choosing elsewhere. Reporting only the second is what let a wrong source guess pass
/// silently, which is the thing this window was built to stop.
///
/// The target line **names the characters**. A count tells someone something is wrong
/// without telling them where to look, and a conversion that quietly drops `Ề` from a
/// letterhead is the failure the whole window exists to prevent.
///
/// `normalized` is deliberately not mentioned: converting to Unicode tổ hợp respells
/// every toned vowel and loses nothing, so counting it would teach people to ignore
/// this line.
pub fn warning(cost: &Cost, from: Charset, to: Charset) -> String {
    let mut lines = Vec::new();
    if cost.undefined > 0 {
        lines.push(format!(
            "{} chữ không thuộc {} — có thể bảng mã nguồn chọn sai, hoặc tệp đã hỏng",
            cost.undefined,
            from.name()
        ));
    }
    if !cost.lost.is_empty() {
        lines.push(target_line(&cost.lost, to));
    }
    lines.join("\n")
}

/// What the target cannot represent, named rather than counted.
fn target_line(lost: &[char], to: Charset) -> String {
    let shown: String = lost
        .iter()
        .take(NAMED)
        .map(char::to_string)
        .collect::<Vec<_>>()
        .join("  ");
    let rest = lost.len().saturating_sub(NAMED);
    let tail = if rest > 0 {
        format!(" và {rest} chữ khác")
    } else {
        String::new()
    };
    format!(
        "{} chữ không có trong {}:  {shown}{tail}",
        lost.len(),
        to.name()
    )
}

/// The files a drop could not read, named — or empty when it read them all.
///
/// **Named, not counted**, for the same reason the warning above names characters:
/// ten files dropped and eight rows shown is a question a number cannot answer, and
/// the reason separates "fix the permissions" from "that file is not what you think
/// it is".
pub fn unreadable_line(files: &[Unreadable]) -> String {
    if files.is_empty() {
        return String::new();
    }
    let named: Vec<String> = files
        .iter()
        .take(NAMED)
        .map(|file| format!("{} ({})", file.name, file.reason))
        .collect();
    let rest = files.len().saturating_sub(NAMED);
    let tail = if rest > 0 {
        format!(" và {rest} tệp khác")
    } else {
        String::new()
    };
    format!("Không đọc được: {}{tail}", named.join(", "))
}

/// A document trimmed to what a pane can usefully show, and made safe to show.
///
/// **The control characters are the Linux half of this.** A damaged legacy file
/// decodes into a `String` that may hold `U+0000` or `U+001A`; GTK's `TextBuffer`
/// takes the length rather than stopping at a NUL, so it measures and draws the rest
/// wrongly and Pango complains in the log. Windows never noticed, which is exactly
/// why the sanitising belongs here rather than in one shell.
///
/// A carriage return is **not** one of them. `\r` is a control character, but it is
/// also how every Windows document ends a line, and the whole point of this tool is
/// Windows documents — marking each one would put a `�` at the end of every line of
/// every file it was built for. Line endings are normalised to `\n` instead: GTK and
/// Slint both draw that as one break, where a raw `\r\n` risks two.
///
/// Only the *preview* is sanitised. The conversion and every counter still run over
/// the real text, so nothing this hides can hide a lost character.
pub fn capped(text: &str) -> String {
    let cut = text
        .char_indices()
        .nth(PREVIEW)
        .map_or(text.len(), |(index, _)| index);
    let mut out = String::with_capacity(cut);
    let mut chars = text[..cut].chars().peekable();
    while let Some(c) = chars.next() {
        out.push(match c {
            '\r' => {
                // A lone `\r` still becomes a break rather than vanishing.
                chars.next_if_eq(&'\n');
                '\n'
            }
            '\n' | '\t' => c,
            c if c.is_control() => char::REPLACEMENT_CHARACTER,
            c => c,
        });
    }
    if cut < text.len() {
        out.push('…');
    }
    out
}

#[cfg(test)]
mod tests;
