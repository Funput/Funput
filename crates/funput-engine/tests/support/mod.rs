//! Shared helpers for engine integration tests.

#![allow(dead_code)]

use funput_core::{InputMethod, ToneStyle};
use funput_engine::{Action, Engine, ImeResult, KeyTouch};

/// Type keys through a fresh [`Engine`]; returns final buffer + per-step [`ImeResult`].
pub fn type_keys_with_results(method: InputMethod, keys: &str) -> (String, Vec<ImeResult>) {
    let mut engine = Engine::new();
    engine.set_method(method);
    engine.update_config(|config| config.tone_style = ToneStyle::Traditional);
    let mut results = Vec::new();
    for key in keys.chars() {
        results.push(engine.process_char(key));
    }
    (engine.buffer().to_owned(), results)
}

/// Short alias for [`type_keys_with_results`].
pub fn type_keys(method: InputMethod, keys: &str) -> (String, Vec<ImeResult>) {
    type_keys_with_results(method, keys)
}

pub fn type_keys_buffer(method: InputMethod, keys: &str) -> String {
    type_keys_with_results(method, keys).0
}

/// Type space-separated words; simulates engine word-boundary clear between words.
pub fn type_words(method: InputMethod, text: &str) -> String {
    let mut engine = Engine::new();
    engine.set_method(method);
    engine.update_config(|config| config.tone_style = ToneStyle::Traditional);
    let mut words = Vec::new();
    for (i, word) in text.split(' ').enumerate() {
        if i > 0 {
            engine.process_char(' ');
        }
        for key in word.chars() {
            engine.process_char(key);
        }
        words.push(engine.buffer().to_owned());
    }
    words.join(" ")
}

/// Reconstruct the app text from the inject stream (None → append key,
/// Send → delete `backspace` chars then append `output`).
pub fn app_text(method: InputMethod, keys: &str) -> String {
    let mut engine = Engine::new();
    engine.set_method(method);
    engine.update_config(|config| config.tone_style = ToneStyle::Traditional);
    let mut app = String::new();
    for key in keys.chars() {
        let r = engine.process_char(key);
        match r.action {
            Action::None => app.push(key),
            Action::Send => {
                for _ in 0..r.backspace {
                    app.pop();
                }
                app.push_str(&r.output);
            }
            Action::Restore => unreachable!("Restore not implemented yet"),
        }
    }
    app
}

/// A stand-in for the app's text field, so a test can assert on what a user would
/// actually see rather than on a stream of instructions.
#[derive(Default)]
pub struct Document {
    text: String,
}

impl Document {
    pub fn new() -> Self {
        Self::default()
    }

    /// Apply a keystroke's result, echoing the key itself when the engine passed it
    /// through — which is what every platform shell does.
    pub fn typed(&mut self, key: char, result: &ImeResult) {
        match result.action {
            Action::None => self.text.push(key),
            _ => self.edited(result),
        }
    }

    /// Apply an edit the engine returned outside a keystroke: a correction, an undo.
    pub fn edited(&mut self, result: &ImeResult) {
        if result.action == Action::None {
            return;
        }
        for _ in 0..result.backspace {
            self.text.pop();
        }
        self.text.push_str(&result.output);
    }

    pub fn text(&self) -> &str {
        &self.text
    }
}

/// An engine with typo correction switched on.
pub fn correcting_engine(method: InputMethod) -> Engine {
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = method;
        config.typo_correction = true;
    });
    engine
}

/// Type `keys`, reporting a touch for every one of them and mirroring the result into
/// `doc`.
///
/// A `(index, key)` slip says the finger landed nearer that neighbour than the key
/// that actually registered — the only thing that gives correction something to find.
pub fn type_touched(engine: &mut Engine, doc: &mut Document, keys: &str, slips: &[(usize, char)]) {
    for (i, key) in keys.chars().enumerate() {
        let mut touch = KeyTouch::new(key, 0.35);
        for &(index, alternate) in slips {
            if index == i {
                touch = touch.with_alternate(alternate, 0.15);
            }
        }
        engine.set_next_key_touch(touch);
        let result = engine.process_char(key);
        doc.typed(key, &result);
    }
}

/// Type `keys` with one key reported as having drifted towards `towards` by
/// `distance` pitches, while its other neighbour sits further away. Lets a test say
/// which way the finger leaned, which a uniform slip cannot.
pub fn type_leaning(
    engine: &mut Engine,
    doc: &mut Document,
    keys: &str,
    at: usize,
    near: (char, f32),
    far: (char, f32),
) {
    for (i, key) in keys.chars().enumerate() {
        let mut touch = KeyTouch::new(key, 0.35);
        if i == at {
            touch = touch
                .with_alternate(near.0, near.1)
                .with_alternate(far.0, far.1);
        }
        engine.set_next_key_touch(touch);
        let result = engine.process_char(key);
        doc.typed(key, &result);
    }
}

/// The candidate words on offer, best first.
pub fn candidate_texts(engine: &Engine) -> Vec<&str> {
    engine
        .correction_candidates()
        .iter()
        .map(funput_engine::CorrectionCandidate::text)
        .collect()
}
