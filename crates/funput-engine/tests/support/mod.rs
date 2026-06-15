//! Shared helpers for engine integration tests.

#![allow(dead_code)]

use funput_core::InputMethod;
use funput_engine::{Engine, ImeResult};

pub fn type_keys(method: InputMethod, keys: &str) -> (String, Vec<ImeResult>) {
    let mut engine = Engine::new();
    engine.set_method(method);
    let mut results = Vec::new();
    for key in keys.chars() {
        results.push(engine.process_char(key));
    }
    (engine.buffer().to_owned(), results)
}

pub fn type_keys_buffer(method: InputMethod, keys: &str) -> String {
    type_keys(method, keys).0
}
