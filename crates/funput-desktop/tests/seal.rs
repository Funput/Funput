//! A lone Ctrl tap seals the word being composed (#736): it stays exactly as shown,
//! the next key starts a new word behind it, and Backspace can still re-open it.
//! Driven through the real `ShellState`, the way the Windows hook drives it.

use funput_core::InputMethod;
use funput_desktop::{Mods, ShellState, TriggerKey};
use funput_engine::{Action, KeySource};

const CTRL: Mods = Mods {
    ctrl: true,
    alt: false,
    win: false,
    shift: false,
};

/// A Telex shell and the text the app shows.
struct Typist {
    state: ShellState,
    app: String,
}

impl Typist {
    fn new() -> Self {
        let mut state = ShellState::new(None);
        state.set_method(InputMethod::Telex);
        Self {
            state,
            app: String::new(),
        }
    }

    /// One row, `ct` → `cơm tấm`.
    fn with_shortcut() -> Self {
        let mut typist = Self::new();
        typist.state.add_shortcut();
        typist.state.set_shortcut_trigger(0, "ct".into());
        typist.state.set_shortcut_expansion(0, "cơm tấm".into());
        typist
    }

    fn type_str(&mut self, keys: &str) -> &mut Self {
        for key in keys.chars() {
            let result = self.state.process_key(key, KeySource::Standard);
            if result.action == Action::None {
                self.app.push(key);
            } else {
                self.replace(result.backspace, &result.output);
            }
        }
        self
    }

    /// Ctrl pressed and released on its own. The app sees the key and does nothing.
    fn ctrl(&mut self) -> &mut Self {
        self.state.seal_word(CTRL);
        self
    }

    /// Backspace through the hook: it passes to the app unless the shell hands back
    /// an edit to make in its place.
    fn backspace(&mut self) -> &mut Self {
        match self.state.on_backspace() {
            Some(undo) => self.replace(undo.backspace, &undo.output),
            None => {
                self.app.pop();
            }
        }
        self
    }

    fn replace(&mut self, backspace: usize, output: &str) {
        for _ in 0..backspace {
            self.app.pop();
        }
        self.app.push_str(output);
    }
}

#[test]
fn the_key_after_ctrl_starts_a_new_word() {
    let mut typist = Typist::new();
    assert_eq!(typist.type_str("dung").ctrl().type_str("x").app, "dungx");
}

#[test]
fn the_sealed_part_keeps_its_marks() {
    let mut typist = Typist::new();
    assert_eq!(typist.type_str("haf").ctrl().type_str("s").app, "hàs");
}

#[test]
fn a_sealed_trigger_does_not_expand_at_space() {
    let mut typist = Typist::with_shortcut();
    assert_eq!(typist.type_str("CT").ctrl().type_str(" ").app, "CT ");

    let mut unsealed = Typist::with_shortcut();
    assert_eq!(unsealed.type_str("CT ").app, "CƠM TẤM ");
}

/// Enter and Tab ask the engine to expand the live word; a sealed word is not live.
#[test]
fn a_sealed_trigger_does_not_expand_at_enter() {
    let mut typist = Typist::with_shortcut();
    typist.state.set_shortcut_on_enter(true);
    typist.type_str("ct").ctrl();
    assert_eq!(typist.state.trigger_key(TriggerKey::Enter), None);
    assert_eq!(typist.app, "ct");
}

/// The caret never moved, so the sealed word is still the one Backspace returns to.
#[test]
fn backspace_reopens_a_sealed_word() {
    let mut typist = Typist::new();
    typist.type_str("dung").ctrl().backspace().type_str("s");
    assert_eq!(typist.app, "dún");
}

#[test]
fn ctrl_with_nothing_composing_changes_nothing() {
    let mut typist = Typist::new();
    assert!(!typist.state.seal_word(CTRL));
    typist.type_str("chaof ");
    assert!(!typist.state.seal_word(CTRL));
    assert_eq!(typist.backspace().type_str("s").app, "cháo");
}

/// Ctrl+Shift and Ctrl+Alt are other gestures, and Alt alone opens an app's menus.
#[test]
fn only_ctrl_on_its_own_seals() {
    let others = [
        Mods {
            shift: true,
            ..CTRL
        },
        Mods { alt: true, ..CTRL },
        Mods { win: true, ..CTRL },
        Mods {
            alt: true,
            ..Mods::default()
        },
    ];
    for tapped in others {
        let mut typist = Typist::new();
        typist.type_str("dung");
        assert!(!typist.state.seal_word(tapped), "{tapped:?}");
        assert_eq!(typist.type_str("x").app, "dũng");
    }
}

#[test]
fn the_switch_turns_sealing_off() {
    let mut typist = Typist::new();
    assert!(typist.state.settings().ctrl_seals_word, "on by default");

    typist.state.set_ctrl_seals_word(false);
    assert!(!typist.state.settings().ctrl_seals_word);
    assert_eq!(typist.type_str("dung").ctrl().type_str("x").app, "dũng");
}
