//! Unit tests for typo correction, driven through the public [`Engine`] verbs so a
//! test reads the way a platform will call it.

mod gate;
mod search;
mod undo;

use funput_core::InputMethod;

use crate::{Engine, KeyTouch};

/// An engine with correction switched on.
fn engine(method: InputMethod) -> Engine {
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = method;
        config.typo_correction = true;
    });
    engine
}

/// Type `keys`, reporting a touch for every one of them.
///
/// A key listed in `slips` also reports the neighbour it names, closer than the key
/// that was actually registered — a finger that drifted. That is the only thing that
/// gives correction anything to find.
fn type_word(engine: &mut Engine, keys: &str, slips: &[(usize, char)]) {
    for (i, key) in keys.chars().enumerate() {
        let mut touch = KeyTouch::new(key, 0.35);
        for &(index, alternate) in slips {
            if index == i {
                touch = touch.with_alternate(alternate, 0.15);
            }
        }
        engine.set_next_key_touch(touch);
        engine.process_char(key);
    }
}

/// Type the word, then the space that ends it — the point correction acts on.
fn type_and_end(engine: &mut Engine, keys: &str, slips: &[(usize, char)]) {
    type_word(engine, keys, slips);
    engine.process_char(' ');
}

/// The corrected words on offer, best first.
fn candidate_texts(engine: &Engine) -> Vec<&str> {
    engine
        .correction_candidates()
        .iter()
        .map(|candidate| candidate.text())
        .collect()
}
