//! A hook + inject shell with the OS taken out, shared by the integration tests.
//!
//! [`Shell`] is the Windows hook's `handle_keydown` in miniature: same order of
//! operations, same inject plan, plus a stand-in "app" that applies the plan to a
//! string. What a test asserts is the text the user ends up looking at.

#![allow(dead_code)]

use funput_core::InputMethod;
use funput_desktop::{CommittedTail, KeySource, plan_inject};
use funput_engine::Engine;

pub struct Shell {
    pub engine: Engine,
    tail: CommittedTail,
    /// The focused app's document. The caret is always at the end.
    pub app: String,
}

impl Shell {
    pub fn new(method: InputMethod) -> Self {
        let mut engine = Engine::new();
        engine.set_method(method);
        Self {
            engine,
            tail: CommittedTail::new(),
            app: String::new(),
        }
    }

    /// `KeyKind::Compose`: feed the engine, then either inject its plan — swallowing
    /// the key, even when the plan is empty — or let the literal key through to the app.
    pub fn key(&mut self, c: char) {
        self.tail.before_key(self.engine.buffer());
        let result = self.engine.process_key(c, KeySource::Standard);
        self.tail.after_key(c, &result, self.engine.buffer());

        let Some(plan) = plan_inject(&result) else {
            self.app.push(c);
            return;
        };
        self.delete(plan.backspaces);
        self.app
            .push_str(&String::from_utf16(&plan.units).expect("valid UTF-16"));
    }

    /// `KeyKind::Backspace`: sync Funput's state, then the physical key reaches the
    /// app and deletes one character.
    pub fn backspace(&mut self) {
        self.sync_backspace();
        self.delete(1);
    }

    /// The body of `shell::on_backspace` on Windows.
    fn sync_backspace(&mut self) {
        if !self.engine.buffer().is_empty() {
            self.engine.on_backspace();
            return;
        }
        let Some(removed) = self.tail.backspace() else {
            return;
        };
        self.engine.rewind_sentence(removed.deleted);
        if let Some(word) = removed.word {
            let adopted = self.engine.adopt(word);
            self.tail.resolve(adopted);
        }
    }

    /// `KeyKind::Flush`: a caret key, Enter, a shortcut, or a mouse click.
    pub fn flush(&mut self) {
        self.engine.clear();
        self.tail.clear();
    }

    pub fn type_str(&mut self, keys: &str) {
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
