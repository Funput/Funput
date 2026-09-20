//! Decoding what the host hands over, where a wrong value must never become a wrong
//! correction.

use funput_engine::KeyTouch;

use super::types::{CORRECTION_CHARS_CAP, FunputCorrectionCandidate};
use super::*;

fn touch(typed: u32) -> FunputKeyTouch {
    FunputKeyTouch {
        typed,
        typed_distance: 0.3,
        alternates: ['g' as u32, 'j' as u32, 'b' as u32],
        distances: [0.1, 0.2, 0.3],
        alternate_count: 3,
    }
}

#[test]
fn a_typed_key_that_is_not_a_scalar_refuses_the_whole_touch() {
    assert!(touch(0xD800).decode().is_none(), "a surrogate is not a key");
    assert!(touch(0x11_0000).decode().is_none(), "past the last plane");
    assert!(touch('h' as u32).decode().is_some());
}

#[test]
fn an_alternate_that_is_not_a_scalar_is_dropped_on_its_own() {
    // One bad neighbour costs that neighbour, not the key it belongs to: the word
    // stays correctable through the alternates that did decode.
    let mut reported = touch('h' as u32);
    reported.alternates[1] = 0xD800;
    let decoded = reported.decode().expect("the typed key is fine");
    assert_eq!(
        decoded,
        KeyTouch::new('h', 0.3)
            .with_alternate('g', 0.1)
            .with_alternate('b', 0.3)
    );
}

#[test]
fn a_count_past_the_cap_reads_only_what_fits() {
    let mut reported = touch('h' as u32);
    reported.alternate_count = 99;
    let decoded = reported.decode().expect("the typed key is fine");
    assert_eq!(
        decoded,
        KeyTouch::new('h', 0.3)
            .with_alternate('g', 0.1)
            .with_alternate('j', 0.2)
            .with_alternate('b', 0.3)
    );
}

#[test]
fn a_count_of_zero_leaves_the_key_with_no_neighbours() {
    let mut reported = touch('h' as u32);
    reported.alternate_count = 0;
    assert_eq!(reported.decode(), Some(KeyTouch::new('h', 0.3)));
}

#[test]
fn an_empty_candidate_encodes_as_nothing_rather_than_as_noise() {
    let encoded = FunputCorrectionCandidate::default();
    assert_eq!(encoded.count, 0);
    assert_eq!(encoded.edits, 0);
    assert_eq!(encoded.chars.len(), CORRECTION_CHARS_CAP);
}
