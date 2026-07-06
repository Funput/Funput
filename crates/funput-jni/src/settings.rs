//! JNI exports for engine configuration.

use funput_core::{InputMethod, ToneStyle};
use jni::EnvUnowned;
use jni::sys::{jboolean, jint, jlong};

use crate::registry;
use crate::support::{JavaObject, safe};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetMethod(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    method: jint,
) {
    safe((), || {
        let method = if method == 1 {
            InputMethod::Vni
        } else {
            InputMethod::Telex
        };
        registry::with_mut(handle, |engine| engine.set_method(method));
    });
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
