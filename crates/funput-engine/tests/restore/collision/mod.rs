//! Collision restore: an English word that a composition rule turned into a real
//! syllable (`two` → `tơ` in Telex, issue #661) gets its keys back at the word
//! boundary, while Vietnamese typed through the same rule stays put.

mod cases;
mod invariant;
mod toggles;

use funput_core::InputMethod;
use funput_engine::Engine;

/// The words the engine ships for the deferred-`w` rule.
const DEFERRED_W: &str = include_str!("../../../data/collision/deferred_w/words.txt");

/// An engine under `method` with collision restore on or off, the rest default.
fn engine(method: InputMethod, restore_collisions: bool) -> Engine {
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = method;
        config.restore_collisions = restore_collisions;
    });
    engine
}

/// What the app shows after `keys` under `method`, feature on or off.
fn shown(method: InputMethod, restore_collisions: bool, keys: &str) -> String {
    crate::support::app_text_in(&mut engine(method, restore_collisions), keys)
}
