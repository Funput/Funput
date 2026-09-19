//! The cases measured on iOS (`docs/features/typo-correction.md` §3.4), as a table.
//!
//! Each row is a word that today lands as raw keystrokes or as an unfinished
//! composition, the neighbouring key the finger drifted to, and the word the system
//! keyboards would have produced.

use funput_core::InputMethod;

use crate::support::{Document, candidate_texts, correcting_engine, type_leaning, type_touched};

struct Case {
    method: InputMethod,
    keys: &'static str,
    /// The neighbour offered, and which key of `keys` it belongs to.
    slip: (usize, char),
    /// What the app shows before the correction — the whole point of the backspace
    /// count, which differs between a restored word and a composed one.
    shown: &'static str,
    corrected: &'static str,
}

const CASES: &[Case] = &[
    Case {
        method: InputMethod::Telex,
        keys: "dduwowfnh",
        slip: (8, 'g'),
        shown: "dduwowfnh",
        corrected: "đường",
    },
    Case {
        method: InputMethod::Telex,
        keys: "tpoi",
        slip: (1, 'o'),
        shown: "tpoi",
        corrected: "tôi",
    },
    Case {
        method: InputMethod::Telex,
        keys: "khpong",
        slip: (2, 'o'),
        shown: "khpong",
        corrected: "không",
    },
    Case {
        method: InputMethod::Telex,
        keys: "vieeyj",
        slip: (4, 't'),
        shown: "vieeyj",
        corrected: "việt",
    },
    // A composed word rather than restored keys: what gets deleted is `quâ`, not the
    // four keystrokes behind it.
    Case {
        method: InputMethod::Telex,
        keys: "quaa",
        slip: (3, 's'),
        shown: "quâ",
        corrected: "quá",
    },
    Case {
        method: InputMethod::Telex,
        keys: "minhg",
        slip: (4, 'f'),
        shown: "minhg",
        corrected: "mình",
    },
    Case {
        method: InputMethod::Vni,
        keys: "d9u7o7nh2",
        slip: (7, 'g'),
        shown: "d9u7o7nh2",
        corrected: "đường",
    },
];

/// Words that are already right, and must come out of a boundary untouched even with
/// a neighbour on offer.
const UNTOUCHED: &[(InputMethod, &str, (usize, char))] = &[
    (InputMethod::Telex, "bans", (3, 'd')),
    (InputMethod::Telex, "banj", (3, 'k')),
    (InputMethod::Vni, "d9u7o7ng3", (8, '2')),
    (InputMethod::Telex, "nhaf", (3, 's')),
    // `dduowxj ` is in the design doc's table, but it composes to `đuợ`, which
    // `funput_core::is_complete_syllable` accepts. Correcting it would mean
    // correcting words that are structurally valid — the context-sensitive extension
    // the design defers, and the one place this feature could rewrite a word the user
    // meant. It stays untouched until that arrives.
    (InputMethod::Telex, "dduowxj", (5, 'c')),
];

#[test]
fn the_measured_cases_correct_the_way_the_system_keyboards_do() {
    for case in CASES {
        let mut engine = correcting_engine(case.method);
        let mut doc = Document::new();
        type_touched(&mut engine, &mut doc, case.keys, &[case.slip]);
        assert_eq!(doc.text(), case.shown, "{}: before", case.keys);
        type_touched(&mut engine, &mut doc, " ", &[]);
        assert!(
            candidate_texts(&engine).contains(&case.corrected),
            "{}: offered {:?}, wanted {}",
            case.keys,
            candidate_texts(&engine),
            case.corrected
        );
        let index = candidate_texts(&engine)
            .iter()
            .position(|text| *text == case.corrected)
            .expect("checked above");
        doc.edited(&engine.apply_correction(Some(index)));
        assert_eq!(
            doc.text(),
            format!("{} ", case.corrected),
            "{}: after",
            case.keys
        );
    }
}

#[test]
fn a_word_that_is_already_right_is_never_touched() {
    for &(method, keys, slip) in UNTOUCHED {
        let mut engine = correcting_engine(method);
        let mut doc = Document::new();
        type_touched(&mut engine, &mut doc, keys, &[slip]);
        let before = doc.text().to_owned();
        type_touched(&mut engine, &mut doc, " ", &[]);
        assert!(
            !engine.has_pending_correction(),
            "{keys}: offered {:?}",
            candidate_texts(&engine)
        );
        assert_eq!(doc.text(), format!("{before} "));
    }
}

#[test]
fn a_correction_keeps_the_case_of_the_word_it_replaces() {
    // `Nhaf` is `Nhà`; mistype the `f` and the capital has to survive the repair.
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "Nhad ", &[(3, 'f')]);
    assert_eq!(candidate_texts(&engine), ["Nhà"]);
}

/// The `nhad` case from the design, in all three directions. Two tones are one key
/// apart, so which one the user meant is only in where the finger landed — this is
/// the case the touch model exists for, and the case where getting it wrong is a
/// wrong word rather than a missed one.
///
/// The numbers here are not arbitrary. At Δ 1.5 the margin only opens once the
/// nearer key is about 0.8 pitches clearer than the other: closer than that and the
/// engine declines and offers both, which is what the design asks for. So a tone
/// pair resolves only for a finger that really did lean.
#[test]
fn nhad_follows_the_finger_to_the_tone_it_leaned_towards() {
    for (near, far, expected) in [
        (('f', 0.1), ('s', 1.0), "nhà"),
        (('s', 0.1), ('f', 1.0), "nhá"),
    ] {
        let mut engine = correcting_engine(InputMethod::Telex);
        let mut doc = Document::new();
        type_leaning(&mut engine, &mut doc, "nhad", 3, near, far);
        type_touched(&mut engine, &mut doc, " ", &[]);

        let chosen = engine
            .choose_correction(&[], &[])
            .expect("one tone is clearly nearer than the other");
        assert_eq!(
            candidate_texts(&engine)[chosen],
            expected,
            "leaning towards {} should choose {expected}",
            near.0
        );
    }
}

/// A finger that leaned only a little is still a finger that might have meant
/// either, so the engine declines rather than guesses.
#[test]
fn nhad_leaning_slightly_is_still_too_close_to_call() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_leaning(&mut engine, &mut doc, "nhad", 3, ('f', 0.15), ('s', 0.75));
    type_touched(&mut engine, &mut doc, " ", &[]);

    assert_eq!(candidate_texts(&engine).len(), 2);
    assert_eq!(engine.choose_correction(&[], &[]), None);
}

#[test]
fn nhad_typed_dead_centre_is_offered_rather_than_applied() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_leaning(&mut engine, &mut doc, "nhad", 3, ('f', 0.3), ('s', 0.3));
    type_touched(&mut engine, &mut doc, " ", &[]);

    assert_eq!(candidate_texts(&engine).len(), 2);
    assert_eq!(engine.choose_correction(&[], &[]), None);
}
