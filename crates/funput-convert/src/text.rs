//! A paragraph: what it becomes, and what it costs.
//!
//! No byte door here. Text arriving through the clipboard is already characters —
//! a TCVN3 paragraph pasted out of Word is code points `U+0020..=U+00FF` standing in
//! for bytes, which is exactly what [`charset::convert`] takes. Only a *file* has to
//! go through `charset::document`, and that is [`crate::batch`]'s problem.

use funput_core::charset::{self, Charset, Conversion};

/// How many characters to name before the warning gets longer than it is useful.
const NAMED: usize = 6;

/// How much of a document to put in a pane.
///
/// Panes are for *looking* at, and nobody reads a megabyte in one. The conversion and
/// the warning still run over the whole document — only the display is cut, so a large
/// file cannot make the window crawl while the counts stay honest.
const PREVIEW: usize = 20_000;

/// Convert `text` for the preview pane.
pub fn preview(text: &str, from: Charset, to: Charset) -> Conversion {
    charset::convert(text, from, to)
}

/// The bytes to save, for a target that stores one byte per letter.
pub fn bytes(text: &str, from: Charset, to: Charset) -> Vec<u8> {
    let unicode = charset::convert(text, from, Charset::Unicode);
    charset::encode_bytes(&unicode.text, to).0
}

/// The warning, or empty when nothing will be lost.
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
pub fn loss(text: &str, from: Charset, to: Charset) -> String {
    let unicode = charset::convert(text, from, Charset::Unicode);
    let mut lines = Vec::new();
    if unicode.unmapped > 0 {
        lines.push(format!(
            "{} chữ không thuộc {} — có thể bảng mã nguồn chọn sai, hoặc tệp đã hỏng",
            unicode.unmapped,
            from.name()
        ));
    }
    if let Some(line) = target_line(&unicode.text, to) {
        lines.push(line);
    }
    lines.join("\n")
}

/// What the target cannot represent, named rather than counted.
fn target_line(unicode: &str, to: Charset) -> Option<String> {
    let lost = unrepresentable(unicode, to);
    if lost.is_empty() {
        return None;
    }
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
    Some(format!(
        "{} chữ không có trong {}:  {shown}{tail}",
        lost.len(),
        to.name()
    ))
}

/// The distinct characters `to` cannot represent, in the order they first appear.
///
/// Asked one character at a time because the counter core returns is a total, not a
/// list. Distinct characters in a document are few — a few hundred at most — so this
/// costs a great deal less than it looks like it does.
fn unrepresentable(unicode: &str, to: Charset) -> Vec<char> {
    let mut seen = std::collections::HashSet::new();
    let mut lost = Vec::new();
    for ch in unicode.chars() {
        if !seen.insert(ch) {
            continue;
        }
        if charset::convert(&ch.to_string(), Charset::Unicode, to).unmapped > 0 {
            lost.push(ch);
        }
    }
    lost
}

/// A document trimmed to what a pane can usefully show, and made safe to show.
///
/// **The control characters are the Linux half of this.** A damaged legacy file
/// decodes into a `String` that may hold `U+0000` or `U+001A`; GTK's `TextBuffer`
/// takes the length rather than stopping at a NUL, so it measures and draws the rest
/// wrongly and Pango complains in the log. Windows never noticed, which is exactly
/// why the sanitising belongs here rather than in one shell.
///
/// Only the *preview* is sanitised. The conversion and both counters still run over
/// the real text, so nothing this hides can hide a lost character.
pub fn capped(text: &str) -> String {
    let cut = text
        .char_indices()
        .nth(PREVIEW)
        .map_or(text.len(), |(index, _)| index);
    let mut out: String = text[..cut]
        .chars()
        .map(|c| match c {
            '\n' | '\t' => c,
            c if c.is_control() => char::REPLACEMENT_CHARACTER,
            c => c,
        })
        .collect();
    if cut < text.len() {
        out.push('…');
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    /// The whole reason the line exists: it says *which* characters, not how many.
    #[test]
    fn the_warning_names_the_characters_that_will_not_survive() {
        let line = loss("Ề 5₫", Charset::Unicode, Charset::Tcvn3);
        assert!(line.contains('Ề'), "{line}");
        assert!(line.contains('₫'), "{line}");
        assert!(line.contains("TCVN3"), "{line}");
    }

    /// The gap a screenshot found. A character the *source* charset does not define
    /// used to pass through in silence, so picking the wrong source looked like a
    /// clean conversion. `U+0131` has no TCVN3 code.
    #[test]
    fn a_character_the_source_charset_does_not_define_is_reported() {
        let line = loss("ngh\u{131}a", Charset::Tcvn3, Charset::Unicode);
        assert!(line.contains("không thuộc"), "{line}");
        assert!(line.contains("TCVN3"), "{line}");
    }

    /// The two problems are separate sentences, because they call for different
    /// things: one says the source is wrong, the other says the target cannot cope.
    #[test]
    fn a_source_and_a_target_problem_are_reported_apart() {
        let line = loss("ngh\u{131}a Ề", Charset::Tcvn3, Charset::Tcvn3);
        assert_eq!(line.lines().count(), 2, "{line}");
    }

    /// Silent when nothing is lost, so the line keeps meaning something.
    #[test]
    fn nothing_is_said_when_nothing_is_lost() {
        assert_eq!(loss("việt nam", Charset::Unicode, Charset::Tcvn3), "");
    }

    /// Respelling is not losing. Every toned vowel changes shape going to tổ hợp and
    /// the round trip is exact, so the warning must stay quiet.
    #[test]
    fn respelling_is_not_reported_as_loss() {
        assert_eq!(
            loss("Việt Nam", Charset::Unicode, Charset::UnicodeCombining),
            ""
        );
    }

    /// A NUL reaching a GTK `TextBuffer` makes it measure and draw the rest of the
    /// document wrongly, so the preview never carries one.
    #[test]
    fn a_control_character_cannot_reach_the_preview() {
        let shown = capped("vi\u{0}\u{1a}t\ttab\nline");
        assert!(!shown.contains('\u{0}'), "{shown:?}");
        assert!(!shown.contains('\u{1a}'), "{shown:?}");
        assert!(shown.contains('\t') && shown.contains('\n'), "{shown:?}");
    }

    /// A legacy target is written as bytes, not as UTF-8 of the same code points.
    #[test]
    fn saving_to_a_legacy_charset_writes_one_byte_per_letter() {
        assert_eq!(bytes("Việt", Charset::Unicode, Charset::Tcvn3), b"Vi\xD6t");
    }
}
