use super::*;

/// Convert a Rust string to the UTF-32 the ABI speaks.
fn utf32(text: &str) -> Vec<u32> {
    text.chars().map(|c| c as u32).collect()
}

fn starts_sentence(before: &str) -> bool {
    let text = utf32(before);
    unsafe { funput_starts_sentence(text.as_ptr(), text.len()) }
}

fn starts_word(before: &str) -> bool {
    let text = utf32(before);
    unsafe { funput_starts_word(text.as_ptr(), text.len()) }
}

/// The rules themselves are core's and tested there; this pins that the door
/// forwards to them rather than to some second copy.
#[test]
fn the_door_answers_with_cores_rules() {
    assert!(starts_sentence(""));
    assert!(starts_sentence("Xin chào. "));
    assert!(starts_sentence("nói \"Xin chào.\" "));
    assert!(!starts_sentence("Xin chào."));
    assert!(!starts_sentence("giá 1.5 "));
}

/// A keyboard commits its capital before the user can see it, so this door always
/// takes the guard. The bulk transform's reading is not reachable from here.
#[test]
fn a_repeated_full_stop_reads_as_an_abbreviation() {
    assert!(!starts_sentence("giấy tờ v.v. "));
    assert!(starts_sentence("TS. "));
}

#[test]
fn words_begin_after_anything_that_is_not_a_letter_or_digit() {
    assert!(starts_word(""));
    assert!(starts_word("Xin "));
    assert!(!starts_word("Xin chà"));
}

/// Null is how a host says it has no text, which is the start of the document.
#[test]
fn a_null_pointer_is_the_start_of_the_document() {
    assert!(unsafe { funput_starts_sentence(std::ptr::null(), 0) });
    assert!(unsafe { funput_starts_word(std::ptr::null(), 0) });
}
