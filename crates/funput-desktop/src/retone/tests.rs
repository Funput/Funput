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

    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("phủ"));
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

    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("phủ"));
}

#[test]
fn every_separator_has_to_be_deleted_before_the_word_is_offered() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "phủ", ',');
    tail.before_key("");
    tail.after_key(' ', &none(), ""); // a space typed on an empty composition

    let space = tail.backspace().expect("the space was deleted");
    assert_eq!(
        (space.deleted, space.word),
        (' ', None),
        "caret on the comma"
    );
    assert_eq!(
        tail.backspace().and_then(|hit| hit.word),
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

    let digit = tail.backspace().expect("the digit was deleted");
    assert_eq!((digit.deleted, digit.word), ('1', None));
    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("phủ"));
}

/// English restore and gõ tắt expansion inject the finished text themselves; the
/// boundary character is already part of `output`.
#[test]
fn injected_output_is_folded_in_whole() {
    let mut tail = CommittedTail::new();
    tail.before_key("tẽt");
    tail.after_key(' ', &send(3, "text "), "");

    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("text"));
}

#[test]
fn an_expansion_offers_only_its_last_word() {
    let mut tail = CommittedTail::new();
    tail.before_key("vn");
    tail.after_key(' ', &send(2, "Việt Nam "), "");

    // `Nam` is what the caret is at the end of — `Việt` is two words back.
    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("Nam"));
}

/// A refused word (the engine only adopts real syllables) stays on screen, and so
/// does the shadow describing it: `backspace` already folded the deletion in, so
/// the next one keeps walking back through the word — and hands over whatever
/// shorter form the engine will take.
#[test]
fn a_refused_word_keeps_the_shadow() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "chào", ' ');
    commit(&mut tail, "dungh", ' ');

    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("dungh"));
    tail.resolve(false); // not a syllable
    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("dung"));
    tail.resolve(true); // `dung` is the live composition now
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
    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("phủ"));
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

/// Undoing an expansion swaps it for the trigger in the shadow, so a later
/// Backspace re-opens the trigger rather than the expansion's last word.
#[test]
fn rewrite_end_swaps_the_expansion_for_what_replaced_it() {
    let mut tail = CommittedTail::new();
    tail.before_key("vn");
    tail.after_key(' ', &send(2, "Việt Nam "), "");

    assert!(tail.rewrite_end("Việt Nam ", "vn "));
    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("vn"));
}

/// When the shadow does not end with what is being replaced it has lost track of
/// the screen, so it forgets everything rather than describe text that is not there.
#[test]
fn rewrite_end_clears_a_shadow_that_does_not_match() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "phủ", ' ');

    assert!(!tail.rewrite_end("Việt Nam ", "vn "));
    assert_eq!(tail.backspace(), None);
}

/// A gõ tắt prefix (`;ct`) is deleted with the word, so the shadow drops it too.
#[test]
fn an_expansion_takes_its_prefix_out_of_the_shadow() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "an", ' ');
    tail.before_key("");
    tail.after_key(';', &none(), "");
    tail.before_key("ct");
    tail.after_key(' ', &send(3, "cơm tấm "), "");
    assert!(tail.ends_with("an cơm tấm "));
}

/// The caret is at the expansion end only at its own length, before and after the
/// shadow grows and shrinks past it.
#[test]
fn the_expansion_end_is_found_again_by_deleting_back_to_it() {
    let mut tail = CommittedTail::new();
    tail.before_key("ct");
    tail.after_key(' ', &send(2, "cơm tấm "), "");
    tail.mark_expansion_end();
    assert!(tail.at_expansion_end());

    commit(&mut tail, "ngon", ' ');
    assert!(!tail.at_expansion_end());
    for _ in "ngon ".chars() {
        tail.backspace();
    }
    assert!(tail.at_expansion_end());

    tail.clear();
    assert!(!tail.at_expansion_end());
}

/// Trimming the front moves the mark with it, and forgets it once the cut reaches it.
#[test]
fn trimming_moves_the_expansion_end_or_forgets_it() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "front", ' ');
    while tail.tail.len() < CAPACITY - 20 {
        commit(&mut tail, "abc", ' ');
    }
    tail.mark_expansion_end();
    commit(&mut tail, "abcdefghijklmnopqrstu", ' ');
    for _ in "abcdefghijklmnopqrstu ".chars() {
        tail.backspace();
    }
    assert!(tail.at_expansion_end(), "the cut stopped before the mark");

    for _ in 0..40 {
        commit(&mut tail, "abcdefghij", ' ');
    }
    for _ in 0..40 * "abcdefghij ".len() {
        tail.backspace();
    }
    assert!(!tail.at_expansion_end(), "the cut reached the mark");
}

/// A cut landing exactly on the mark drops the expansion from the shadow, so the mark
/// goes with it rather than survive as a mark at the very start.
#[test]
fn a_cut_exactly_at_the_expansion_end_forgets_it() {
    let mut tail = CommittedTail::new();
    commit(&mut tail, "front", ' ');
    tail.mark_expansion_end();
    commit(&mut tail, &"a".repeat(CAPACITY - 6), ' ');
    assert!(!tail.tail.starts_with("front"), "the cut took `front `");
    while tail.backspace().is_some() {}
    assert!(!tail.at_expansion_end());
}

/// Backspace landing right after an expansion keeps its last word closed: the next
/// Backspace is for the whole expansion, not for editing its last word.
#[test]
fn backspace_onto_an_expansion_end_offers_no_word() {
    let mut tail = CommittedTail::new();
    tail.before_key("ct");
    tail.after_key(';', &send(2, "cơm tấm"), "");
    tail.mark_expansion_end();
    commit(&mut tail, "", ' ');
    assert_eq!(tail.backspace().map(|hit| hit.word), Some(None));

    tail.forget_expansion_end();
    commit(&mut tail, "", ' ');
    assert_eq!(tail.backspace().and_then(|hit| hit.word), Some("tấm"));
}

/// More prefix than the shadow holds empties it rather than keep a stale front.
#[test]
fn a_prefix_longer_than_the_shadow_empties_it() {
    let mut tail = CommittedTail::new();
    tail.before_key("ct");
    tail.after_key(' ', &send(4, "cơm tấm "), "");
    assert!(tail.ends_with("cơm tấm "));
    assert_eq!(tail.backspace().map(|hit| hit.deleted), Some(' '));
}
