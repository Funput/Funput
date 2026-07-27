use funput_core::InputMethod;
use funput_engine::{Action, Engine};

fn engine() -> Engine {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::TelexAdvanced);
    engine
}

#[test]
fn full_telex_runs_end_to_end() {
    for (keys, output) in [
        ("w ", "ư "),
        ("wf ", "ừ "),
        ("t[ ", "tư "),
        ("m] ", "mơ "),
        ("tr[]ngf ", "trường "),
        ("ng[]if ", "người "),
        ("WWindowws ", "Windows "),
    ] {
        assert_eq!(
            crate::support::app_text(InputMethod::TelexAdvanced, keys),
            output,
            "{keys}"
        );
    }
}

#[test]
fn canonical_and_full_corpora_commit_the_same_text() {
    let canonical = "uw uwf tuw mow truwowngf nguwowif uwngf.";
    let full = "w wf t[ m] tr[]ngf ng[]if wngf.";
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, canonical),
        crate::support::app_text(InputMethod::TelexAdvanced, full)
    );
}

#[test]
fn direct_shortcuts_emit_minimal_diffs() {
    let mut engine = engine();
    let leading = engine.process_char('w');
    assert_eq!((leading.backspace, leading.output.as_str()), (0, "ư"));
    let reverted = engine.process_char('w');
    assert_eq!((reverted.backspace, reverted.output.as_str()), (1, "w"));

    engine.clear();
    engine.process_char('t');
    let horn_u = engine.process_char('[');
    assert_eq!((horn_u.backspace, horn_u.output.as_str()), (0, "ư"));
    engine.clear();
    for key in "tr[".chars() {
        engine.process_char(key);
    }
    let horn_o = engine.process_char(']');
    assert_eq!((horn_o.backspace, horn_o.output.as_str()), (0, "ơ"));
}

#[test]
fn raw_keys_flip_and_sticky_latin_survive_boundary() {
    let mut engine = engine();
    for key in "tr[]ngf".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "trường");
    assert_eq!(engine.keys(), "tr[]ngf");

    engine.flip_composing();
    assert_eq!(engine.buffer(), "tr[]ngf");
    engine.flip_composing();
    assert_eq!(engine.buffer(), "trường");
    engine.flip_composing();
    assert_eq!(engine.buffer(), "tr[]ngf");
    engine.process_char(' ');
    assert!(engine.buffer().is_empty());
}

#[test]
fn brackets_are_boundaries_only_outside_advanced_telex() {
    let mut standard = Engine::new();
    standard.process_char('t');
    assert_eq!(standard.process_char('[').action, Action::None);
    assert!(standard.buffer().is_empty());

    let mut advanced = engine();
    advanced.process_char('t');
    assert_eq!(advanced.process_char('[').action, Action::Send);
    assert_eq!(advanced.buffer(), "tư");
}

#[test]
fn unresolvable_composition_restores_at_boundary() {
    // `h` + `w` takes the `ư`, then `d` leaves it unspellable — the raw run comes
    // back at the word boundary, as in plain Telex.
    assert_eq!(
        crate::support::app_text(InputMethod::TelexAdvanced, "hwd "),
        "hwd "
    );
    // `dwd` does resolve here: `w` is the `ư` key and `d` is the stroke, so Full
    // Telex commits `đư` where plain Telex restores the pending `w` as `dwd`.
    assert_eq!(
        crate::support::app_text(InputMethod::TelexAdvanced, "dwd "),
        "đư "
    );
    assert_eq!(crate::support::app_text(InputMethod::Telex, "dwd "), "dwd ");
}

#[test]
fn capitalization_backspace_and_method_switch_stay_synchronized() {
    let mut engine = engine();
    engine.update_config(|c| c.auto_capitalize = true);
    engine.arm_capitalization();
    engine.process_char('[');
    assert_eq!(engine.buffer(), "Ư");
    assert_eq!(engine.keys(), "[");
    engine.on_backspace();
    assert!(engine.buffer().is_empty());
    assert!(engine.keys().is_empty());

    engine.process_char('w');
    engine.set_method(InputMethod::TelexAdvanced);
    assert_eq!(engine.buffer(), "ư");
    engine.set_method(InputMethod::Telex);
    assert!(engine.buffer().is_empty());
    assert!(engine.keys().is_empty());
}

#[test]
fn layout_stays_compact() {
    assert_eq!(std::mem::size_of::<InputMethod>(), 1);
    assert_eq!(std::mem::size_of::<Engine>(), 136);
}
