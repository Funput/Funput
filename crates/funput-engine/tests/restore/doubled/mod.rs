//! Doubled-key restore: an English word gets back the key a revert swallowed
//! (`password`, not `pasword`), while the doubled-key habit and Vietnamese stay put.

mod cases;
mod invariant;
mod toggles;

use funput_core::InputMethod;
use funput_engine::Engine;

/// An engine under `method` with doubled-key restore on or off, the rest default.
fn engine(method: InputMethod, restore_doubled_keys: bool) -> Engine {
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = method;
        config.restore_doubled_keys = restore_doubled_keys;
    });
    engine
}

/// What the app shows after `keys` under default Telex, feature on.
fn telex(keys: &str) -> String {
    crate::support::app_text_in(&mut engine(InputMethod::Telex, true), keys)
}
