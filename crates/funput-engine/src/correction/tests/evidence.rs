//! What the generators are told about each key, and by whom.

use funput_core::InputMethod;

use crate::correction::evidence::Evidence;
use crate::correction::score::{LAMBDA, SUBSTITUTE_STATIC, substitution_cost};
use crate::correction::touch::{KeyTouch, TouchLog};

/// A log for `keys` where every key listed in `reported` carries a touch with one
/// neighbour (`x`, nearer than the key itself), and the rest arrived without one.
fn log(keys: &str, reported: &[usize]) -> TouchLog {
    let mut log = TouchLog::default();
    for (i, key) in keys.chars().enumerate() {
        let touch = reported
            .contains(&i)
            .then(|| KeyTouch::new(key, 0.4).with_alternate('x', 0.2).store());
        log.push(key, touch);
    }
    log
}

fn keys_of(evidence: &Evidence<'_>, origin: Option<u8>, key: char) -> Vec<(char, f32)> {
    evidence.neighbours(origin, key).iter().collect()
}

#[test]
fn a_reported_touch_answers_with_the_host_s_alternates_priced_by_the_model() {
    let log = log("nha", &[0, 1, 2]);
    let evidence = Evidence::new(&log, "nha", InputMethod::Telex);
    assert!(evidence.has_full_touch());
    let found = keys_of(&evidence, Some(1), 'h');
    assert_eq!(found.len(), 1);
    assert_eq!(found[0].0, 'x');
    // Distances are stored in Q8.8, so compare against the quantised model.
    assert!((found[0].1 - substitution_cost(0.4, 0.2)).abs() < 0.01);
    assert!(
        found[0].1 < LAMBDA,
        "a nearer neighbour costs less than the flat edit"
    );
}

#[test]
fn a_key_without_a_report_is_read_off_the_layout() {
    let log = log("nha", &[0, 2]);
    let evidence = Evidence::new(&log, "nha", InputMethod::Telex);
    assert!(!evidence.has_full_touch());
    let found = keys_of(&evidence, Some(1), 'h');
    let keys: String = found.iter().map(|(key, _)| key).collect();
    assert_eq!(keys, "gjybn");
    assert!(found.iter().all(|(_, cost)| *cost == SUBSTITUTE_STATIC));
}

#[test]
fn a_position_with_no_typed_key_behind_it_is_read_off_the_layout_too() {
    let log = log("nha", &[0, 1, 2]);
    let evidence = Evidence::new(&log, "nha", InputMethod::Telex);
    let keys: String = keys_of(&evidence, None, 'h')
        .iter()
        .map(|(key, _)| key)
        .collect();
    assert_eq!(keys, "gjybn");
}

#[test]
fn a_log_that_no_longer_matches_the_keys_is_ignored_whole() {
    let log = log("nha", &[0, 1, 2]);
    let evidence = Evidence::new(&log, "nh", InputMethod::Telex);
    assert!(!evidence.has_full_touch());
    assert_eq!(keys_of(&evidence, Some(1), 'h')[0].1, SUBSTITUTE_STATIC);
}

#[test]
fn with_no_evidence_at_all_the_layout_speaks_and_nothing_competes_as_typed() {
    let evidence = Evidence::none(InputMethod::Vni);
    assert!(!evidence.has_full_touch());
    let keys: String = keys_of(&evidence, Some(0), 'q')
        .iter()
        .map(|(key, _)| key)
        .collect();
    assert_eq!(keys, "1wa", "VNI reads the digit row");
    let evidence = Evidence::none(InputMethod::Telex);
    let keys: String = keys_of(&evidence, Some(0), 'q')
        .iter()
        .map(|(key, _)| key)
        .collect();
    assert_eq!(keys, "wa");
}
