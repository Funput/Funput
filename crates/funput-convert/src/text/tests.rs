use funput_core::charset::{Charset, read, render};

use super::{capped, warning};

/// The sentence for a document converted between two charsets, the way a window
/// asks for it.
fn line(text: &str, from: Charset, to: Charset) -> String {
    warning(&render(&read(text, from), to).cost, from, to)
}

/// The whole reason the line exists: it says *which* characters, not how many.
#[test]
fn the_warning_names_the_characters_that_will_not_survive() {
    let out = line("Ề 5₫", Charset::Unicode, Charset::Tcvn3);
    assert!(out.contains('Ề'), "{out}");
    assert!(out.contains('₫'), "{out}");
    assert!(out.contains("TCVN3"), "{out}");
}

/// The gap a screenshot found. A character the *source* charset does not define
/// used to pass through in silence, so picking the wrong source looked like a
/// clean conversion. `U+0131` has no TCVN3 code.
#[test]
fn a_character_the_source_charset_does_not_define_is_reported() {
    let out = line("ngh\u{131}a", Charset::Tcvn3, Charset::Unicode);
    assert!(out.contains("không thuộc"), "{out}");
    assert!(out.contains("TCVN3"), "{out}");
}

/// The two problems are separate sentences, because they call for different
/// things: one says the source is wrong, the other says the target cannot cope.
#[test]
fn a_source_and_a_target_problem_are_reported_apart() {
    let out = line("ngh\u{131}a Ề", Charset::Tcvn3, Charset::Tcvn3);
    assert_eq!(out.lines().count(), 2, "{out}");
}

/// Silent when nothing is lost, so the line keeps meaning something.
#[test]
fn nothing_is_said_when_nothing_is_lost() {
    assert_eq!(line("việt nam", Charset::Unicode, Charset::Tcvn3), "");
}

/// Respelling is not losing. Every toned vowel changes shape going to tổ hợp and
/// the round trip is exact, so the warning must stay quiet.
#[test]
fn respelling_is_not_reported_as_loss() {
    assert_eq!(
        line("Việt Nam", Charset::Unicode, Charset::UnicodeCombining),
        ""
    );
}

/// Long lists get a tail rather than a paragraph — six names is where a warning
/// stops helping and starts being skipped.
#[test]
fn a_long_list_is_cut_short_and_says_so() {
    let out = line("日本語のテキストです", Charset::Unicode, Charset::Tcvn3);
    assert!(out.contains("chữ khác"), "{out}");
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

/// The regression that shipped: `\r` is a control character, so sanitising them all
/// put a `�` at the end of every line of every CRLF document — which is every
/// Windows document, the entire reason this tool exists.
#[test]
fn a_windows_line_ending_is_not_marked_as_damage() {
    let shown = capped("Cộng hòa\r\nĐộc lập\r\n");
    assert!(!shown.contains('\u{FFFD}'), "{shown:?}");
    assert_eq!(shown, "Cộng hòa\nĐộc lập\n");
}

/// Normalised rather than passed through: a raw `\r\n` risks drawing as two breaks
/// in GTK, and a lone `\r` must still separate lines rather than disappear.
#[test]
fn every_line_ending_becomes_exactly_one_break() {
    assert_eq!(capped("a\r\nb").lines().count(), 2);
    assert_eq!(capped("a\rb").lines().count(), 2);
    assert_eq!(capped("a\nb").lines().count(), 2);
}
