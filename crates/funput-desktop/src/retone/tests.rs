use funput_engine::{Action, ImeResult};

use super::*;

fn none() -> ImeResult {
    ImeResult {
        action: Action::None,
        backspace: 0,
        output: String::new(),
    }
}

fn send(backspace: usize, output: &str) -> ImeResult {
    ImeResult {
        action: Action::Send,
        backspace,
        output: output.to_owned(),
    }
}

/// Commit `word` with `key`, the way the engine does when nothing is injected:
/// the word is already on screen and the app echoes the boundary key after it.
fn commit(tail: &mut CommittedTail, word: &str, key: char) {
    tail.before_key(word);
    tail.after_key(key, &none(), "");
}

/// The reason this exists: `phủ` + Space, then Backspace, hands `phủ` back.
#[test]
fn backspace_over_the_space_reopens_the_word() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "phủ", ' ');

    assert_eq!(tail.backspace(), Some("phủ"));
    tail.resolve(true);
    // The word belongs to the engine now, so the shadow no longer claims it.
    assert_eq!(tail.backspace(), None);
}

#[test]
fn a_key_that_is_still_composing_leaves_the_shadow_alone() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "phủ", ' ');

    // Typing the next word must not push anything: it lives in the engine buffer
    // until a boundary commits it.
    tail.before_key("");
    tail.after_key('c', &none(), "c");
    tail.before_key("c");
    tail.after_key('h', &none(), "ch");

    assert_eq!(tail.backspace(), Some("phủ"));
}

#[test]
fn every_separator_has_to_be_deleted_before_the_word_is_offered() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "phủ", ',');
    tail.before_key("");
    tail.after_key(' ', &none(), ""); // a space typed on an empty composition

    assert_eq!(
        tail.backspace(),
        None,
        "deleted the space, caret on the comma"
    );
    assert_eq!(
        tail.backspace(),
        Some("phủ"),
        "deleted the comma, caret at the end of the word"
    );
}

/// A digit at the start of a word passes through the engine untouched, so it stands
/// between the word and the caret like any other character.
#[test]
fn a_passed_through_digit_counts_as_a_character() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "phủ", ' ');
    tail.before_key("");
    tail.after_key('1', &none(), "");

    assert_eq!(tail.backspace(), None); // deleted the digit
    assert_eq!(tail.backspace(), Some("phủ"));
}

/// English restore and gõ tắt expansion inject the finished text themselves; the
/// boundary character is already part of `output`.
#[test]
fn injected_output_is_folded_in_whole() {
    let mut tail = CommittedTail::new();
    tail.before_key("tẽt");
    tail.after_key(' ', &send(3, "text "), "");

    assert_eq!(tail.backspace(), Some("text"));
}

#[test]
fn an_expansion_offers_only_its_last_word() {
    let mut tail = CommittedTail::new();
    tail.before_key("vn");
    tail.after_key(' ', &send(2, "Việt Nam "), "");

    // `Nam` is what the caret is at the end of — `Việt` is two words back.
    assert_eq!(tail.backspace(), Some("Nam"));
}

/// A refused word (the engine only adopts real syllables) is left on screen with
/// nothing tracking it, so the shadow can no longer place the caret.
#[test]
fn a_refused_word_resets_the_shadow() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "chào", ' ');
    commit(&mut tail, "hello", ' ');

    assert_eq!(tail.backspace(), Some("hello"));
    tail.resolve(false);
    assert_eq!(tail.backspace(), None);
}

#[test]
fn clear_forgets_everything() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "phủ", ' ');
    tail.clear();
    assert_eq!(tail.backspace(), None);
}

#[test]
fn backspacing_an_empty_shadow_is_a_no_op() {
    let mut tail = CommittedTail::new();
    assert_eq!(tail.backspace(), None);
    assert_eq!(tail.backspace(), None);
}

/// Over the cap, the shadow drops whole words off the front: what is left has to
/// stay a suffix of the real text, and must never begin mid-word.
#[test]
fn trimming_drops_whole_words_and_keeps_the_last_one() {
    let mut tail = CommittedTail::new();
    for _ in 0..12 {
        commit(&mut tail, "nghiêng", ' ');
    }
    commit(&mut tail, "phủ", ' ');

    assert!(tail.tail.len() <= CAPACITY + "nghiêng ".len());
    assert!(
        !tail.tail.starts_with("iêng"),
        "trimming cut a word in half: {:?}",
        tail.tail
    );
    assert_eq!(tail.backspace(), Some("phủ"));
}

/// A single run longer than the cap has no separator to cut at, so the shadow gives
/// up rather than offering a fragment from the middle of it.
#[test]
fn an_unbroken_run_longer_than_the_cap_is_dropped() {
    let mut tail = CommittedTail::new();
    let long = "a".repeat(CAPACITY * 2);
    commit(&mut tail, &long, ' ');

    assert!(tail.tail.len() <= CAPACITY);
    assert_eq!(tail.backspace(), None);
}

#[test]
fn word_start_finds_the_run_after_the_last_separator() {
    assert_eq!(word_start(""), 0);
    assert_eq!(word_start("phủ"), 0);
    assert_eq!(word_start("chào phủ"), "chào ".len());
    assert_eq!(word_start("phủ "), "phủ ".len()); // ends on a separator
}
