use funput_core::InputMethod;

use super::lookup::SortedLines;
use super::*;
use crate::compose::pipeline;

const FRUIT: SortedLines = SortedLines::new("apple\nbanana\ncherry\ndate\nfig\n");

/// Feed `keys` the way `Engine::compose_key` does: raw key first, then the pipeline.
fn typed(method: InputMethod, keys: &str) -> Session {
    let mut session = Session::new();
    session.config.method = method;
    for key in keys.chars() {
        session.keys.push(key);
        note_key(&mut session, key);
        pipeline::process(&mut session, key, key, false);
    }
    session
}

#[test]
fn finds_every_line_and_nothing_between() {
    for word in ["apple", "banana", "cherry", "date", "fig"] {
        assert!(FRUIT.contains(word), "{word}");
    }
    for word in ["", "a", "app", "apples", "blueberry", "egg", "zebra"] {
        assert!(!FRUIT.contains(word), "{word}");
    }
}

#[test]
fn ignores_ascii_case_and_refuses_non_ascii() {
    assert!(FRUIT.contains("Banana"));
    assert!(FRUIT.contains("FIG"));
    assert!(!FRUIT.contains("bánana"));
}

#[test]
fn a_single_line_and_an_empty_list() {
    assert!(SortedLines::new("pass\n").contains("pass"));
    assert!(!SortedLines::new("pass\n").contains("past"));
    assert!(!SortedLines::new("").contains("pass"));
}

#[test]
fn the_shipped_list_is_sorted_lowercase_and_newline_terminated() {
    assert!(WORDS.text().ends_with('\n'));
    let words: Vec<&str> = WORDS.lines().collect();
    assert!(!words.is_empty());
    for word in &words {
        assert!(
            !word.is_empty() && word.bytes().all(|byte| byte.is_ascii_lowercase()),
            "{word:?} is not a lowercase ASCII word"
        );
    }
    for pair in words.windows(2) {
        assert!(
            pair[0] < pair[1],
            "{:?} is not before {:?}",
            pair[0],
            pair[1]
        );
    }
}

#[test]
fn the_shipped_list_holds_the_shortened_words_and_not_the_habits() {
    for word in [
        "password", "correct", "pass", "office", "coffee", "issue", "session",
    ] {
        assert!(WORDS.contains(word), "{word} should be listed");
    }
    // `of` outranks `off`, and the rest are how UniKey users get a plain letter back.
    for word in ["off", "tesst", "mixx", "iss", "mix", "test"] {
        assert!(!WORDS.contains(word), "{word} should not be listed");
    }
}

#[test]
fn a_word_without_a_revert_keeps_literal_empty() {
    for keys in ["mas", "chaof", "card", "text", "ddaayf"] {
        assert!(typed(InputMethod::Telex, keys).literal.is_empty(), "{keys}");
    }
}

#[test]
fn the_first_revert_keeps_the_doubled_key_and_later_keys_follow() {
    let session = typed(InputMethod::Telex, "pass");
    assert_eq!(session.keys, "pas");
    assert_eq!(session.literal, "pass");
    let session = typed(InputMethod::Telex, "passwor");
    assert_eq!(session.literal, "passwor");
}

#[test]
fn restorable_only_for_a_listed_word_not_yet_shown() {
    assert!(restorable(&typed(InputMethod::Telex, "password")));
    assert!(!restorable(&typed(InputMethod::Telex, "tesst")));
    assert!(!restorable(&typed(InputMethod::Telex, "mixx")));
}

#[test]
fn switched_off_it_never_starts() {
    let mut session = Session::new();
    session.config.restore_doubled_keys = false;
    for key in "pass".chars() {
        session.keys.push(key);
        note_key(&mut session, key);
        pipeline::process(&mut session, key, key, false);
    }
    assert!(session.literal.is_empty());
    assert!(!restorable(&session));
}

#[test]
fn vni_never_starts() {
    assert!(typed(InputMethod::Vni, "a11").literal.is_empty());
}

#[test]
fn clear_and_backspace_drop_literal() {
    let mut session = typed(InputMethod::Telex, "pass");
    session.clear();
    assert!(session.literal.is_empty());
    let mut session = typed(InputMethod::Telex, "pass");
    session.buffer.pop();
    session.resync_after_backspace();
    assert!(session.literal.is_empty());
}
