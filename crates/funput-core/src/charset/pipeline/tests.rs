use super::super::Charset;
use super::{read, render};

/// **The bug this module exists to remove.** A pane showed one thing while the file
/// received another, because the preview took the one-pass route and the save took
/// the two-step one. `₫` has no byte in TCVN3, so the file gets `?` — and now the
/// pane says so.
#[test]
fn the_text_shown_is_the_bytes_written() {
    for to in [Charset::Tcvn3, Charset::VniWindows] {
        let rendered = render(&read("giá 5₫", Charset::Unicode), to);
        assert_eq!(
            rendered.text.chars().map(|c| c as u32).collect::<Vec<_>>(),
            rendered
                .bytes
                .iter()
                .map(|&b| u32::from(b))
                .collect::<Vec<_>>(),
            "{to:?} shows text its own bytes do not hold",
        );
        assert!(rendered.text.contains('?'), "{:?}", rendered.text);
    }
}

/// The other half: a target that is not made of bytes gets UTF-8, and the text is
/// the text — no narrowing, and `₫` survives.
#[test]
fn a_target_that_is_not_bytes_keeps_every_character() {
    let rendered = render(&read("giá 5₫", Charset::Unicode), Charset::UnicodeCombining);
    assert!(rendered.text.contains('₫'));
    assert_eq!(rendered.bytes, rendered.text.as_bytes());
}

/// Reading is the target-independent half, which is the whole reason it is a step of
/// its own: a window changing the target must not pay for it twice.
#[test]
fn one_read_serves_every_target() {
    let pivoted = read(
        b"vi\xD6t nam"
            .iter()
            .map(|&b| char::from(b))
            .collect::<String>()
            .as_str(),
        Charset::Tcvn3,
    );
    assert_eq!(pivoted.text, "việt nam");
    for to in [Charset::Unicode, Charset::Tcvn3, Charset::VniWindows] {
        assert_eq!(render(&pivoted, to).cost.undefined, 0, "{to:?}");
    }
}

/// A code the source never defined is counted, and stays counted no matter which
/// target is asked for — it is a fact about the *reading*, not about the writing.
#[test]
fn a_code_the_source_never_defined_is_reported_for_every_target() {
    let pivoted = read("\u{C2}", Charset::Tcvn3);
    assert_eq!(pivoted.undefined, 1);
    for to in [Charset::Unicode, Charset::Tcvn3, Charset::VniWindows] {
        assert_eq!(render(&pivoted, to).cost.undefined, 1, "{to:?}");
    }
}

/// The two numbers behind the two sentences a consumer writes: how many *distinct*
/// characters cannot be spelled, and how many times they occur. A menu names the
/// first; a terminal counts the second.
#[test]
fn distinct_characters_and_total_occurrences_are_counted_apart() {
    let cost = render(&read("₫₫₫ Ề", Charset::Unicode), Charset::Tcvn3).cost;
    assert_eq!(cost.lost, vec!['₫', 'Ề']);
    assert_eq!(cost.unrepresentable, 4, "three ₫ and one Ề");
    assert!(!cost.is_clean());
}

/// Respelling is not losing.
///
/// It is counted on the **reading** side, because that is the only side that can see
/// it: writing always starts from precomposed Unicode, which is spelled one way. So
/// this reads ordinary precomposed text *as tổ hợp* — understood exactly, spelled
/// differently — and the two loss numbers must stay at zero.
#[test]
fn respelling_is_not_counted_as_loss() {
    let cost = render(
        &read("Việt Nam", Charset::UnicodeCombining),
        Charset::Unicode,
    )
    .cost;
    assert!(cost.is_clean(), "{cost:?}");
    assert!(cost.normalized > 0, "the spelling did change: {cost:?}");
}
