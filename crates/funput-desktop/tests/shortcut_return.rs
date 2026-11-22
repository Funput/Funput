//! Backspace takes a gõ tắt expansion back after the user typed on and deleted back
//! to it (#782). Driven through the real `ShellState`, the way the Windows hook drives
//! it, plus the shadow check on its own.

use funput_core::InputMethod;
use funput_desktop::{CommittedTail, ShellState, track_expansion, undo_shortcut};
use funput_engine::{Action, Engine, ImeResult};

/// A Telex shell with gõ tắt rows, and the text the app shows.
struct Typist {
    state: ShellState,
    app: String,
}

impl Typist {
    /// `ct` → `cơm tấm` and `cf` → `cà phê`.
    fn new() -> Self {
        Self::with_rows(&[("ct", "cơm tấm"), ("cf", "cà phê")])
    }

    fn with_rows(rows: &[(&str, &str)]) -> Self {
        let mut state = ShellState::new(None);
        state.set_method(InputMethod::Telex);
        // A new row is inserted at the top. Add from the bottom of the slice
        // upward, filling index 0, so the finished table matches `rows`.
        for (trigger, expansion) in rows.iter().rev() {
            state.add_shortcut();
            state.set_shortcut_trigger(0, (*trigger).into());
            state.set_shortcut_expansion(0, (*expansion).into());
        }
        Self {
            state,
            app: String::new(),
        }
    }

    fn type_str(&mut self, keys: &str) -> &mut Self {
        for key in keys.chars() {
            let result = self
                .state
                .process_key(key, funput_engine::KeySource::Standard);
            if result.action == Action::None {
                self.app.push(key);
            } else {
                self.replace(&result);
            }
        }
        self
    }

    /// Backspace through the hook: it passes to the app unless the shell hands back
    /// an edit to make in its place.
    fn backspace(&mut self, times: usize) -> &mut Self {
        for _ in 0..times {
            match self.state.on_backspace() {
                Some(undo) => self.replace(&undo),
                None => {
                    self.app.pop();
                }
            }
        }
        self
    }

    fn replace(&mut self, result: &ImeResult) {
        for _ in 0..result.backspace {
            self.app.pop();
        }
        self.app.push_str(&result.output);
    }
}

#[test]
fn deleting_back_to_an_expansion_takes_it_back_then_fixes_the_trigger() {
    let mut typist = Typist::new();
    typist.type_str("ct nayf ngon");
    assert_eq!(typist.app, "cơm tấm này ngon");

    typist.backspace("này ngon".chars().count());
    assert_eq!(typist.app, "cơm tấm ");
    typist.backspace(1);
    assert_eq!(typist.app, "ct ");

    typist.backspace(2).type_str("f ");
    assert_eq!(typist.app, "cà phê ");
}

/// A trigger ending in its own boundary (`ct;`) writes no boundary character, so the
/// expansion ends on a letter. Deleting back onto it must not re-open `tấm` for editing.
#[test]
fn an_expansion_ending_on_a_letter_is_taken_back_too() {
    let mut typist = Typist::with_rows(&[("ct;", "cơm tấm")]);
    typist.type_str("ct; ngon");
    assert_eq!(typist.app, "cơm tấm ngon");

    typist.backspace(" ngon".chars().count());
    assert_eq!(typist.app, "cơm tấm");
    typist.backspace(1);
    assert_eq!(typist.app, "ct;");
}

/// The undo after coming back refuses the trigger once, like the immediate one.
#[test]
fn the_trigger_taken_back_after_more_words_is_kept_once() {
    let mut typist = Typist::new();
    typist.type_str("ct x").backspace(2);
    assert_eq!(typist.app, "ct ");

    typist.type_str("ct ");
    assert_eq!(typist.app, "ct ct ");
}

/// A Ctrl seal clears the engine, so the expansion is gone and Backspace is plain.
#[test]
fn sealing_a_word_forgets_the_expansion() {
    let mut typist = Typist::new();
    typist.state.set_ctrl_seals_word(true);
    typist.type_str("ct x");
    typist.state.seal_word(funput_desktop::Mods {
        ctrl: true,
        alt: false,
        win: false,
        shift: false,
    });

    typist.backspace(2);
    assert_eq!(typist.app, "cơm tấm");
}

/// The shadow is back at the mark but the text there changed behind the shell's back
/// (an app's own autocorrect): nothing is taken back, and the expansion is dropped.
#[test]
fn a_changed_expansion_at_the_mark_is_dropped() {
    let mut engine = Engine::new();
    engine.add_shortcut("ct", "cơm tấm");
    let mut tail = CommittedTail::new();
    for key in "ct x".chars() {
        tail.before_key(engine.buffer());
        let result = engine.process_char(key);
        tail.after_key(key, &result, engine.buffer());
        track_expansion(&engine, &mut tail);
    }
    engine.on_backspace();
    tail.backspace();
    tail.backspace();
    let echoed = ImeResult {
        action: Action::None,
        backspace: 0,
        output: String::new(),
    };
    tail.before_key("n");
    tail.after_key(' ', &echoed, "");
    assert!(tail.at_expansion_end() && !tail.ends_with("cơm tấm "));

    assert_eq!(undo_shortcut(&mut engine, &mut tail), None);
    assert!(!engine.rearm_shortcut_undo());
    assert!(!tail.at_expansion_end());
}
