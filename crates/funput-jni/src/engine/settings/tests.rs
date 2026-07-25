use funput_core::{InputMethod, ToneStyle};
use funput_engine::EngineConfig;
use jni::sys::jint;

use super::{decode_method, decode_tone_style};
use crate::engine::registry;

/// Drive the engine the way `nativeConfigure` does — one batch config, then keys.
fn type_with(config: EngineConfig, keys: &str) -> String {
    let handle = registry::create();
    let buffer = registry::with_mut(handle, |engine| {
        engine.configure(config);
        for key in keys.chars() {
            engine.process_char(key);
        }
        engine.buffer().to_owned()
    })
    .expect("registered engine");
    registry::destroy(handle);
    buffer
}

fn config(method: jint, tone_style: jint) -> EngineConfig {
    EngineConfig {
        method: decode_method(method),
        tone_style: decode_tone_style(tone_style),
        ..EngineConfig::default()
    }
}

#[test]
fn method_wire_values_are_stable_and_unknown_is_safe() {
    assert_eq!(decode_method(0), InputMethod::Telex);
    assert_eq!(decode_method(1), InputMethod::Vni);
    assert_eq!(decode_method(2), InputMethod::TelexAdvanced);
    assert_eq!(decode_method(-1), InputMethod::Telex);
    assert_eq!(decode_method(99), InputMethod::Telex);
}

#[test]
fn tone_style_wire_values_are_stable_and_unknown_is_safe() {
    assert_eq!(decode_tone_style(1), ToneStyle::Modern);
    assert_eq!(decode_tone_style(0), ToneStyle::Traditional);
    assert_eq!(decode_tone_style(42), ToneStyle::Traditional);
}

#[test]
fn persisted_advanced_id_survives_registry_relaunch() {
    let persisted_method = 2; // Telex Advanced
    assert_eq!(type_with(config(persisted_method, 0), "t["), "tư");
    assert_eq!(type_with(config(persisted_method, 0), "m]"), "mơ");
}

/// The wire config must land field for field: a swapped bool or a dropped tone style
/// would still compile, so each option is asserted through composed output.
#[test]
fn configure_applies_each_wire_option() {
    // VNI (1) + Modern tone (1): `hoa2` puts the tone on `a`, not `o`.
    assert_eq!(type_with(config(1, 1), "hoa2"), "hoà");
    // Same keys, Traditional (0).
    assert_eq!(type_with(config(1, 0), "hoa2"), "hòa");

    // Spell-check on keeps an impossible diacritic literal (Telex `tetf`).
    let checked = EngineConfig {
        spell_check: true,
        smart_restore: false,
        ..config(0, 0)
    };
    assert_eq!(type_with(checked, "tetf"), "tetf");
    let unchecked = EngineConfig {
        spell_check: false,
        smart_restore: false,
        ..config(0, 0)
    };
    assert_eq!(type_with(unchecked, "tetf"), "tèt");

    // Eager restore flips a dead-end word back to raw keys before the boundary.
    let eager = EngineConfig {
        eager_restore: true,
        ..config(0, 0)
    };
    assert_eq!(type_with(eager, "text"), "text");
    let lazy = EngineConfig {
        eager_restore: false,
        ..config(0, 0)
    };
    assert_eq!(type_with(lazy, "text"), "tẽt");
}
