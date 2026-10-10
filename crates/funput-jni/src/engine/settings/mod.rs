//! JNI exports for engine configuration.
//!
//! Android applies `nativeConfigure` followed by `nativeSetExtraOnsets`, so the Kotlin
//! side keeps a single writer for engine configuration. `nativeSetEnabled` stays
//! separate: VI/EN is runtime state, flipped per field and by the language key.

use funput_core::{InputMethod, ToneStyle};
use jni::EnvUnowned;
use jni::sys::{jboolean, jint, jlong};

use super::registry;
use crate::abi::{JavaObject, safe};

mod extra_onsets;

/// Apply the original durable options without resetting extra onsets or shortcuts.
/// Wire values mirror Kotlin's `EngineConfiguration`: method `0 = Telex`,
/// `1 = VNI`, `2 = Telex Advanced`; tone
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
    // Preserve options supplied by separate setters, including extra onsets and
    // shortcuts. The Kotlin declaration and JNI signature remain unchanged.
    update(handle, |engine| {
        engine.update_config(|config| {
            config.method = decode_method(method);
            config.tone_style = decode_tone_style(tone_style);
            config.smart_restore = smart_restore;
            config.eager_restore = eager_restore;
            config.spell_check = spell_check;
            config.auto_capitalize = auto_capitalize;
        });
    });
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

/// Switch typo correction on or off. Runtime state like `nativeSetEnabled`, and kept
/// out of `nativeConfigure` because that symbol's name encodes its Java signature.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetTypoCorrection(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    on: jboolean,
) {
    safe((), || {
        registry::with_mut(handle, |engine| {
            engine.update_config(|config| config.typo_correction = on);
        });
    })
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
