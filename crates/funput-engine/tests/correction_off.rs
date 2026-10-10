//! Switching typo correction on must not change what typing does.
//!
//! The whole fixture corpus is replayed with the setting on, in the three shapes a
//! platform can take: reporting no touches at all, reporting them and declining every
//! correction, and reporting them and never answering. The first must reproduce the
//! recorded instruction stream exactly; the other two must land on the same text,
//! because a declined correction is the word boundary finishing the job it deferred.

mod fixtures {
    pub mod step_cases;
}
mod support;

use funput_core::{InputMethod, ToneStyle};
use funput_engine::{Action, Engine, EngineConfig, KeyTouch};

use fixtures::step_cases::{APP_TEXT_CASES, STEP_CASES, TELEX_BUFFER_CASES, VNI_BUFFER_CASES};
use support::{Document, correcting_engine};

/// What a platform does with a correction it is offered.
#[derive(Clone, Copy, PartialEq)]
enum Answer {
    /// Its word store vetoed the change — the common case for English words.
    Decline,
    /// It does not implement the second step at all.
    Ignore,
    /// It never even reports where the fingers landed.
    NoTouches,
}

const ROWS: [&str; 3] = ["qwertyuiop", "asdfghjkl", "zxcvbnm"];

/// The key to the right on a QWERTY keyboard — a plausible neighbour for any letter,
/// so most words in the corpus really do get candidates parked for them.
fn neighbour(key: char) -> Option<char> {
    ROWS.iter().find_map(|row| {
        let index = row.find(key)?;
        row[index + 1..].chars().next()
    })
}

fn engine(method: InputMethod) -> Engine {
    let mut engine = correcting_engine(method);
    engine.update_config(|config| config.tone_style = ToneStyle::Traditional);
    engine
}

struct Run {
    doc: Document,
    steps: Vec<(Action, usize, String)>,
    buffer: String,
}

/// Type `keys` with correction on, answering the way `answer` says.
fn run(method: InputMethod, keys: &str, answer: Answer) -> Run {
    let mut engine = engine(method);
    let mut run = Run {
        doc: Document::new(),
        steps: Vec::new(),
        buffer: String::new(),
    };
    for key in keys.chars() {
        if answer != Answer::NoTouches {
            let mut touch = KeyTouch::new(key, 0.2);
            if let Some(alternate) = neighbour(key) {
                touch = touch.with_alternate(alternate, 0.9);
            }
            engine.set_next_key_touch(touch);
        }
        let result = engine.process_char(key);
        run.steps
            .push((result.action, result.backspace, result.output.clone()));
        run.doc.typed(key, &result);
        if answer == Answer::Decline && engine.has_pending_correction() {
            run.doc.edited(&engine.apply_correction(None));
        }
    }
    run.buffer = engine.buffer().to_owned();
    run
}

#[test]
fn the_default_configuration_leaves_typo_correction_off() {
    assert!(!EngineConfig::default().typo_correction);
    assert!(!Engine::new().config().typo_correction);
}

#[test]
fn every_fixture_case_keeps_its_instruction_stream_when_no_touches_are_reported() {
    for case in STEP_CASES {
        let run = run(case.method, case.keys, Answer::NoTouches);
        assert_eq!(
            run.buffer, case.final_buffer,
            "{}: final buffer",
            case.label
        );
        assert_eq!(
            run.steps.len(),
            case.steps.len(),
            "{}: step count",
            case.label
        );
        for (i, (got, expected)) in run.steps.iter().zip(case.steps.iter()).enumerate() {
            assert_eq!(got.0, expected.action, "{}: step {i} action", case.label);
            assert_eq!(
                got.1, expected.backspace,
                "{}: step {i} backspace",
                case.label
            );
            assert_eq!(got.2, expected.output, "{}: step {i} output", case.label);
        }
    }
}

#[test]
fn every_fixture_case_keeps_its_text_when_every_correction_is_declined() {
    for case in APP_TEXT_CASES {
        let run = run(case.method, case.keys, Answer::Decline);
        assert_eq!(run.doc.text(), case.output, "{}", case.label);
    }
}

#[test]
fn every_fixture_case_keeps_its_text_when_the_platform_never_answers() {
    for case in APP_TEXT_CASES {
        let run = run(case.method, case.keys, Answer::Ignore);
        assert_eq!(run.doc.text(), case.output, "{}", case.label);
    }
}

#[test]
fn every_buffer_case_composes_the_same_word_with_correction_on() {
    for case in TELEX_BUFFER_CASES.iter().chain(VNI_BUFFER_CASES) {
        let mut engine = engine(case.method);
        for key in case.keys.chars() {
            let mut touch = KeyTouch::new(key, 0.2);
            if let Some(alternate) = neighbour(key) {
                touch = touch.with_alternate(alternate, 0.9);
            }
            engine.set_next_key_touch(touch);
            engine.process_char(key);
        }
        assert_eq!(engine.buffer(), case.output, "{}", case.label);
    }
}
