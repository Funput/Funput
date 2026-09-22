use super::*;

fn typing(before: &str) -> bool {
    starts_sentence(before, Rules::TYPING)
}

fn transform(before: &str) -> bool {
    starts_sentence(before, Rules::TRANSFORM)
}

#[test]
fn an_empty_document_starts_a_sentence() {
    assert!(typing(""));
}

#[test]
fn a_terminator_needs_the_whitespace_that_confirms_it() {
    assert!(!typing("Xin chào."));
    assert!(typing("Xin chào. "));
    assert!(!typing("Xin chào"));
}

#[test]
fn every_terminator_ends_a_sentence() {
    for text in ["Thật. ", "Thật! ", "Thật? ", "Thật… "] {
        assert!(typing(text), "{text:?} should end a sentence");
    }
}

#[test]
fn a_run_of_terminators_ends_one_sentence() {
    assert!(typing("Thật?! "));
    assert!(typing("Rồi... "));
}

#[test]
fn a_decimal_point_is_not_an_ending() {
    assert!(!typing("giá 1.5 "));
    assert!(!typing("giá 1.5 triệu "));
}

#[test]
fn a_newline_is_a_boundary_without_punctuation() {
    assert!(typing("một\n"));
    assert!(typing("một\n\n"));
}

#[test]
fn closers_between_the_terminator_and_the_space_are_transparent() {
    assert!(typing("nói \"Xin chào.\" "));
    assert!(typing("(Xin chào.) "));
    assert!(typing("nói 'Xin chào.' "));
    assert!(typing("nói «Xin chào.» "));
}

#[test]
fn a_closer_alone_does_not_end_a_sentence() {
    assert!(!typing("(xin chào) "));
    assert!(!typing("nói \"xin chào\" "));
}

#[test]
fn typing_reads_a_repeated_dot_as_an_abbreviation() {
    assert!(!typing("giấy tờ v.v. "));
    assert!(!typing("lúc 10 a.m. "));
    assert!(!typing("sang U.S. "));
}

#[test]
fn a_single_dotted_word_still_reads_as_an_ending() {
    // No earlier dot gives `TS.` away, so it is indistinguishable from an ending.
    assert!(typing("TS. "));
    assert!(typing("gửi về TP. "));
}

#[test]
fn the_transform_reading_takes_every_dot_as_an_ending() {
    assert!(transform("giấy tờ v.v. "));
    assert!(transform("sang U.S. "));
}

#[test]
fn an_opener_leaves_the_sentence_waiting() {
    assert!(typing("\""));
    assert!(typing("("));
    assert!(typing("— "));
}

#[test]
fn a_digit_has_already_begun_the_sentence() {
    assert!(!typing("3 "));
    assert!(!typing("Xin chào. 3 "));
}

#[test]
fn a_word_starts_after_anything_that_is_not_a_letter_or_digit() {
    assert!(starts_word(""));
    assert!(starts_word("Xin "));
    assert!(starts_word("một-"));
    assert!(starts_word("(",));
}

#[test]
fn a_word_does_not_start_mid_token() {
    assert!(!starts_word("Xin chà"));
    assert!(!starts_word("3"));
}
