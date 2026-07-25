//! JNI exports for engine configuration.
//!
//! Android applies its durable options as one batch (`nativeConfigure`), so the Kotlin
//! side keeps a single writer for engine configuration. `nativeSetEnabled` stays
//! separate: VI/EN is runtime state, flipped per field and by the language key.

use funput_core::{InputMethod, ToneStyle};
use funput_engine::EngineConfig;
use jni::EnvUnowned;
use jni::sys::{jboolean, jint, jlong};

use super::registry;
use crate::abi::{JavaObject, safe};

/// Apply every durable engine option in one call. Wire values mirror the Kotlin
/// `EngineConfiguration`: method `0 = Telex`, `1 = VNI`, `2 = Telex Advanced`; tone
/// style `1 = Modern`, anything else Traditional.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeConfigure(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    method: jint,
    tone_style: jint,
    smart_restore: jboolean,
    eager_restore: jboolean,
    spell_check: jboolean,
    auto_capitalize: jboolean,
) {
    let config = EngineConfig {
        method: decode_method(method),
        tone_style: decode_tone_style(tone_style),
        smart_restore,
        eager_restore,
        spell_check,
        auto_capitalize,
    };
    update(handle, |engine| engine.configure(config));
}

pub(crate) fn decode_method(method: jint) -> InputMethod {
    match method {
        1 => InputMethod::Vni,
        2 => InputMethod::TelexAdvanced,
        _ => InputMethod::Telex,
    }
}

pub(crate) fn decode_tone_style(style: jint) -> ToneStyle {
    if style == 1 {
        ToneStyle::Modern
    } else {
        ToneStyle::Traditional
    }
}

/// Vietnamese composition on/off — runtime state, not part of the batch config: the
/// IME flips it per field and when the user taps the language key.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetEnabled(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    enabled: jboolean,
) {
    update(handle, |engine| engine.set_enabled(enabled));
}

fn update(handle: jlong, operation: impl FnOnce(&mut funput_engine::Engine)) {
    safe((), || {
        registry::with_mut(handle, operation);
    });
}

#[cfg(test)]
mod tests;
