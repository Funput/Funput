//! The pasted-text state: what it is, what it will become, what it will cost.
//!
//! No byte door here. Text arriving through the clipboard is already characters —
//! a TCVN3 paragraph pasted out of Word is code points `U+0020..=U+00FF` standing in
//! for bytes, which is exactly what [`charset::convert`] takes. Only a *file* has to
//! go through `charset::document`, and that is the batch's problem.

use funput_core::charset::{self, Charset, Conversion};

use super::view;

/// How many characters to name before the warning gets longer than it is useful.
const NAMED: usize = 6;

/// Convert `text` for the preview pane.
pub(super) fn preview(text: &str, from: Charset, to: Charset) -> Conversion {
    charset::convert(text, from, to)
}

/// The bytes to save, for a target that stores one byte per letter.
pub(super) fn bytes(text: &str, from: Charset, to: Charset) -> Vec<u8> {
    let unicode = charset::convert(text, from, Charset::Unicode);
    charset::encode_bytes(&unicode.text, to).0
}

/// The warning line, or empty when nothing will be lost.
///
/// **Names the characters**, which is the point. A count alone tells the user
/// something is wrong without telling them where to look, and a conversion that
/// quietly drops `Ề` from a letterhead is the failure this whole window exists to
/// prevent. `normalized` is deliberately not mentioned: converting to Unicode tổ hợp
/// respells every toned vowel and loses nothing, so counting it would teach people to
/// ignore this line.
pub(super) fn loss(text: &str, from: Charset, to: Charset) -> String {
    let unicode = charset::convert(text, from, Charset::Unicode);
    let lost = unrepresentable(&unicode.text, to);
    if lost.is_empty() {
        return String::new();
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
    format!(
        "{} chữ không có trong {}:  {shown}{tail}",
        lost.len(),
        to.name()
    )
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


/// Save the converted paragraph to a file the user picks.
///
/// Bytes, not text. A legacy target stores one byte per letter, and writing the same
/// characters as UTF-8 would spend two on each and produce a file `.VnTime` cannot
/// read back — the one mistake that would make the whole window pointless.
pub(super) fn save() {
    let Some(window) = view::current() else { return };
    let (input, from, to) = view::STATE.with(|s| {
        let state = s.borrow();
        (
            state.input.clone(),
            state.source.map(view::at),
            view::at(state.target),
        )
    });
    let Some(from) = from else { return };
    let Some(path) = rfd::FileDialog::new()
        .set_file_name("chuyen-ma.txt")
        .add_filter("Văn bản", &["txt"])
        .save_file()
    else {
        return;
    };
    let message = match std::fs::write(&path, bytes(&input, from, to)) {
        Ok(()) => format!("Đã lưu {}", path.display()),
        Err(err) => format!("Không lưu được: {err}"),
    };
    window.set_progress(message.into());
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

    /// A legacy target is written as bytes, not as UTF-8 of the same code points.
    #[test]
    fn saving_to_a_legacy_charset_writes_one_byte_per_letter() {
        assert_eq!(bytes("Việt", Charset::Unicode, Charset::Tcvn3), b"Vi\xD6t");
    }
}
