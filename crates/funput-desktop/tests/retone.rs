//! Retone after Backspace, driven the way a hook + inject shell drives it.
//!
//! The shell below is the Windows hook's `handle_keydown` with the OS taken out:
//! same order of operations, same inject plan, plus a stand-in "app" that applies
//! the plan to a string. What it asserts is the text the user ends up looking at.

use funput_core::InputMethod;
use funput_desktop::{CommittedTail, KeySource, plan_inject};
use funput_engine::Engine;

struct Shell {
    engine: Engine,
    tail: CommittedTail,
    /// The focused app's document. The caret is always at the end.
    app: String,
}

impl Shell {
    fn new(method: InputMethod) -> Self {
        let mut engine = Engine::new();
        engine.set_method(method);
        Self {
            engine,
            tail: CommittedTail::new(),
            app: String::new(),
        }
    }

    /// `KeyKind::Compose`: feed the engine, then either inject its plan or let the
    /// literal key through to the app.
    fn key(&mut self, c: char) {
        self.tail.before_key(self.engine.buffer());
        let result = self.engine.process_key(c, KeySource::Standard);
        self.tail.after_key(c, &result, self.engine.buffer());

        let plan = plan_inject(&result);
        if plan.is_noop() {
            self.app.push(c);
            return;
        }
        self.delete(plan.backspaces);
        self.app
            .push_str(&String::from_utf16(&plan.units).expect("valid UTF-16"));
    }

    /// `KeyKind::Backspace`: sync Funput's state, then the physical key reaches the
    /// app and deletes one character.
    fn backspace(&mut self) {
        self.sync_backspace();
        self.delete(1);
    }

    /// The body of `shell::on_backspace` on Windows.
    fn sync_backspace(&mut self) {
        if !self.engine.buffer().is_empty() {
            self.engine.on_backspace();
            return;
        }
        let Some(word) = self.tail.backspace() else {
            return;
        };
        let adopted = self.engine.adopt(word);
        self.tail.resolve(adopted);
    }

    /// `KeyKind::Flush`: a caret key, Enter, a shortcut, or a mouse click.
    fn flush(&mut self) {
        self.engine.clear();
        self.tail.clear();
    }

    fn type_str(&mut self, keys: &str) {
        for key in keys.chars() {
            self.key(key);
        }
    }

    fn delete(&mut self, count: usize) {
        for _ in 0..count {
            self.app.pop();
        }
    }
}

/// The feature as the user asked for it: `phủ`, Space, Backspace, then a tone key
/// edits the word instead of typing a letter after it.
#[test]
fn backspace_after_a_space_retones_the_word() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("phur ");
    assert_eq!(shell.app, "phủ ");

    shell.backspace();
    assert_eq!(shell.app, "phủ", "the space is gone, the word is untouched");

    shell.key('s');
    assert_eq!(shell.app, "phú");
}

#[test]
fn retoning_works_the_same_in_vni() {
    let mut shell = Shell::new(InputMethod::Vni);
    shell.type_str("phu3 ");
    assert_eq!(shell.app, "phủ ");

    shell.backspace();
    shell.key('1'); // VNI sắc
    assert_eq!(shell.app, "phú");
}

/// The retoned word is a normal composition again: the next boundary commits the
/// edited form, and English restore does not fire on it.
#[test]
fn a_boundary_after_retoning_commits_the_edited_word() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("chaof ");
    shell.backspace();
    shell.key('s');
    shell.key(' ');
    assert_eq!(shell.app, "cháo ");
}

/// Two words: only the one the caret lands on is re-opened, and the text in front
/// of it is left alone.
#[test]
fn only_the_word_at_the_caret_is_reopened() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("chaof phur ");
    assert_eq!(shell.app, "chào phủ ");

    shell.backspace();
    shell.key('s');
    assert_eq!(shell.app, "chào phú");
}

/// The reported bug: a word whose stop coda (`c`, `ch`, `p`, `t`) never got its tone
/// is committed exactly as typed, so re-opening it has to work too.
#[test]
fn a_word_committed_before_its_tone_is_reopened() {
    for (keys, tone, expected) in [
        ("chuc ", 's', "chúc"),
        ("tich ", 's', "tích"),
        ("hoc ", 'j', "học"),
        ("dat ", 'j', "dạt"),
    ] {
        let mut shell = Shell::new(InputMethod::Telex);
        shell.type_str(keys);
        shell.backspace();
        shell.key(tone);
        assert_eq!(shell.app, expected, "typing {keys:?} then ⌫ then {tone:?}");
    }
}

/// The same bug one step earlier: a word committed before its *vowel shape*. `dien`
/// is `diên` without the circumflex, so the shape, stroke and tone keys all have to
/// reach it — `dien` + Space + ⌫ + `d` gives `đien`, not `diend`.
#[test]
fn a_word_committed_before_its_vowel_shape_is_reopened() {
    for (keys, modifier, expected) in [
        ("dien ", 'd', "đien"),
        ("vien ", 'e', "viên"),
        ("thuo ", 'w', "thuơ"),
        ("nuoc ", 'w', "nươc"),
        ("cuu ", 'w', "cưu"),
    ] {
        let mut shell = Shell::new(InputMethod::Telex);
        shell.type_str(keys);
        shell.backspace();
        shell.key(modifier);
        assert_eq!(
            shell.app, expected,
            "typing {keys:?} then ⌫ then {modifier:?}"
        );
    }
}

/// VNI reaches a re-opened word the same way — the gate is the engine's, not Telex's.
#[test]
fn a_word_awaiting_its_shape_is_reopened_in_vni_too() {
    let mut shell = Shell::new(InputMethod::Vni);
    shell.type_str("vien ");
    shell.backspace();
    shell.key('6'); // VNI circumflex
    assert_eq!(shell.app, "viên");
}

/// The looser gate stops at the word boundary: an English word that happens to look
/// like an unshaped rhyme is still restored when the next boundary arrives.
#[test]
fn an_english_word_that_looks_unshaped_is_restored_at_the_boundary() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("true ");
    assert_eq!(shell.app, "true ");

    shell.backspace();
    shell.key('s');
    shell.key(' ');
    assert_eq!(shell.app, "trues ");
}

/// The engine only adopts real Vietnamese syllables, so an English word keeps
/// taking literal letters after it.
#[test]
fn an_english_word_is_not_reopened() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("hello ");
    assert_eq!(shell.app, "hello ");

    shell.backspace();
    shell.key('s');
    assert_eq!(shell.app, "hellos");
}

/// Every character between the word and the caret has to be deleted first — the
/// word is only re-opened once the caret actually reaches it.
#[test]
fn punctuation_between_the_word_and_the_caret_is_deleted_first() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("phur, ");
    assert_eq!(shell.app, "phủ, ");

    shell.backspace();
    shell.key('s');
    assert_eq!(
        shell.app, "phủ,s",
        "the caret was on the comma, not on the word"
    );

    shell.flush();
    shell.type_str("phur, ");
    shell.backspace();
    shell.backspace();
    shell.key('s');
    assert_eq!(shell.app, "phủ,sphú");
}

/// Holding Backspace eats the live word and carries on into the one before it,
/// which is re-opened in turn.
#[test]
fn backspace_carries_on_into_the_previous_word() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("chaof banj");
    assert_eq!(shell.app, "chào bạn");

    for _ in 0..4 {
        shell.backspace(); // ba̱n, ba, b, then the space
    }
    assert_eq!(shell.app, "chào");

    shell.key('s');
    assert_eq!(shell.app, "cháo");
}

/// Anything that may have moved the caret without Funput seeing it — a click, an
/// arrow key, a focus change — drops the shadow, and Backspace goes back to being
/// a plain delete.
#[test]
fn a_flush_disables_retoning() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.type_str("phur ");
    shell.flush(); // the user clicked somewhere; Funput no longer knows the caret

    shell.backspace();
    shell.key('s');
    assert_eq!(shell.app, "phủs");
}

/// A gõ tắt expansion lands in the app as one injected string; the caret sits at
/// the end of its last word, which is the one that re-opens.
#[test]
fn an_expansion_reopens_its_last_word() {
    let mut shell = Shell::new(InputMethod::Telex);
    shell.engine.add_shortcut("vn", "Việt Nam");
    shell.type_str("vn ");
    assert_eq!(shell.app, "Việt Nam ");

    shell.backspace();
    shell.key('f');
    assert_eq!(shell.app, "Việt Nàm");
}
