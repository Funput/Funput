//! A hook + inject shell with the OS taken out, shared by the integration tests.
//!
//! [`Shell`] is the Windows hook's `handle_keydown` in miniature: same order of
//! operations, same inject plan, plus a stand-in "app" that applies the plan to a
//! string. What a test asserts is the text the user ends up looking at.

#![allow(dead_code)]

use funput_core::InputMethod;
use funput_desktop::{CommittedTail, ImeResult, KeySource, plan_inject, undo_shortcut};
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

        if !self.inject(&result) {
            self.app.push(c);
        }
    }

    /// `KeyKind::Backspace`: right after a gõ tắt expansion the key is swallowed and
    /// the undo injected. Otherwise Funput syncs its state, then the physical key
    /// reaches the app and deletes one character.
    pub fn backspace(&mut self) {
        if let Some(undo) = undo_shortcut(&mut self.engine, &mut self.tail) {
            self.inject(&undo);
            return;
        }
        self.sync_backspace();
        self.delete(1);
    }

    /// Apply the inject plan for `result` to the app. `false` when there is none and
    /// the key goes through instead.
    fn inject(&mut self, result: &ImeResult) -> bool {
        let Some(plan) = plan_inject(result) else {
            return false;
        };
        self.delete(plan.backspaces);
        self.app
            .push_str(&String::from_utf16(&plan.units).expect("valid UTF-16"));
        true
    }

    /// The rest of `ShellState::on_backspace`, once no undo applies.
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
            let adopted = self.engine.adopt_after(word, removed.before);
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
