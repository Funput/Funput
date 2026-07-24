//! JNI exports for engine configuration.

use funput_core::{InputMethod, ToneStyle};
use jni::EnvUnowned;
use jni::sys::{jboolean, jint, jlong};

use super::registry;
use crate::abi::{JavaObject, safe};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetMethod(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    method: jint,
) {
    safe((), || {
        let method = decode_method(method);
        registry::with_mut(handle, |engine| engine.set_method(method));
    });
}

pub(crate) fn decode_method(method: jint) -> InputMethod {
    match method {
        1 => InputMethod::Vni,
        2 => InputMethod::TelexAdvanced,
        _ => InputMethod::Telex,
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetToneStyle(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    style: jint,
) {
    safe((), || {
        let style = if style == 1 {
            ToneStyle::Modern
        } else {
            ToneStyle::Traditional
        };
        registry::with_mut(handle, |engine| engine.set_tone_style(style));
    });
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetEnabled(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    enabled: jboolean,
) {
    update(handle, |engine| engine.set_enabled(enabled));
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetSpellCheck(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    enabled: jboolean,
) {
    update(handle, |engine| engine.set_spell_check(enabled));
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetSmartRestore(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    enabled: jboolean,
) {
    update(handle, |engine| engine.set_smart_restore(enabled));
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetEagerRestore(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    enabled: jboolean,
) {
    update(handle, |engine| engine.set_eager_restore(enabled));
}

fn update(handle: jlong, operation: impl FnOnce(&mut funput_engine::Engine)) {
    safe((), || {
        registry::with_mut(handle, operation);
    });
}

#[cfg(test)]
mod tests {
    use super::*;

    fn type_on(handle: i64, wire_method: jint, keys: &str) -> String {
        registry::with_mut(handle, |engine| {
            engine.set_method(decode_method(wire_method));
            for key in keys.chars() {
                engine.process_char(key);
            }
            engine.buffer().to_owned()
        })
        .expect("registered engine")
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
    fn persisted_advanced_id_survives_registry_relaunch() {
        let persisted_method = 2;
        let first = registry::create();
        assert_eq!(type_on(first, persisted_method, "t["), "tư");
        registry::destroy(first);

        let relaunched = registry::create();
        assert_eq!(type_on(relaunched, persisted_method, "m]"), "mơ");
        registry::destroy(relaunched);
    }
}
