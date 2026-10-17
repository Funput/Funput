//! What the IME decodes, tested without a JVM: only the JNI marshalling needs one,
//! and none of the decisions live there.

use funput_engine::{Action, ImeResult};

use super::*;

fn send(backspace: usize, output: &str) -> ImeResult {
    ImeResult {
        action: Action::Send,
        backspace,
        output: output.to_owned(),
    }
}

#[test]
fn an_edit_encodes_as_its_backspace_count_then_its_text() {
    let encoded = encode_edit(Some(send(10, "đường ")));
    assert_eq!(encoded[0], 10);
    let text: String = encoded[1..]
        .iter()
        .filter_map(|&c| char::from_u32(c as u32))
        .collect();
    assert_eq!(text, "đường ");
}

#[test]
fn a_no_op_encodes_as_nothing_at_all() {
    // An empty array is how the IME is told to leave the document alone; a `[0]`
    // would read as a real edit that deletes nothing and inserts nothing.
    assert!(encode_edit(None).is_empty());
    assert!(
        encode_edit(Some(ImeResult {
            action: Action::None,
            backspace: 0,
            output: String::new(),
        }))
        .is_empty()
    );
}

#[test]
fn a_code_point_java_cannot_have_typed_is_not_a_key() {
    // Java hands codepoints over as a signed int, so -1 is how the IME says "no
    // neighbour here" and must never decode to a character.
    assert_eq!(to_char('h' as jint), Some('h'));
    assert_eq!(to_char(-1), None);
    assert_eq!(to_char(0x11_0000), None, "past the last plane");
    assert_eq!(to_char(0xD800), None, "a surrogate is not a scalar");
}

#[test]
fn a_ranking_encodes_as_best_second_and_verdict() {
    use funput_engine::{CorrectionRanking, CorrectionVerdict};

    use super::preview::encode_ranking;

    let ranking = CorrectionRanking {
        best: Some(1),
        second: Some(0),
        verdict: CorrectionVerdict::Ambiguous,
    };
    assert_eq!(encode_ranking(ranking), [1, 0, 1]);
    // An empty slot is -1, never 0: 0 is a real candidate index.
    assert_eq!(encode_ranking(CorrectionRanking::REFUSED), [-1, -1, 3]);
}

#[test]
fn metrics_encode_in_the_order_the_c_header_declares_them() {
    use funput_engine::CorrectionMetrics;

    use super::query::encode_metrics;

    // Ten distinct values, so a swapped pair cannot pass by accident.
    let metrics = CorrectionMetrics {
        applied: 1,
        reverted: 2,
        skipped_ambiguous: 3,
        candidates_max: 4,
        microseconds_max: 5,
        kept_as_typed: 6,
        offered: 7,
        no_candidate: 8,
        suppressed: 9,
        valid_near_edge: 10,
    };
    assert_eq!(encode_metrics(&metrics), [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
}

#[test]
fn a_count_past_what_java_can_hold_clamps_rather_than_wraps() {
    use funput_engine::CorrectionMetrics;

    use super::query::encode_metrics;

    let metrics = CorrectionMetrics {
        microseconds_max: u32::MAX,
        ..CorrectionMetrics::default()
    };
    assert_eq!(encode_metrics(&metrics)[4], jint::MAX);
}
