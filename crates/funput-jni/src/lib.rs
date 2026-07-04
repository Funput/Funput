//! Minimal JNI boundary between Android and the shared Funput engine.

mod registry;

use std::panic::{AssertUnwindSafe, catch_unwind};

use funput_core::InputMethod;
use funput_engine::Action;
use jni::EnvUnowned;
use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JObject, JString};
use jni::sys::{jboolean, jint, jlong};

type JavaObject<'caller> = JObject<'caller>;

fn safe<T>(default: T, operation: impl FnOnce() -> T) -> T {
    catch_unwind(AssertUnwindSafe(operation)).unwrap_or(default)
}

fn string_result<'caller>(
    mut env: EnvUnowned<'caller>,
    operation: impl FnOnce() -> String,
) -> JString<'caller> {
    env.with_env(|env| JString::from_str(env, operation()))
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeCreate(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
) -> jlong {
    safe(0, registry::create)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeDestroy(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) {
    safe((), || registry::destroy(handle));
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeClear(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) {
    safe((), || {
        registry::with_mut(handle, |engine| engine.clear());
    });
}

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
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetEnabled(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    enabled: jboolean,
) {
    safe((), || {
        registry::with_mut(handle, |engine| engine.set_enabled(enabled));
    });
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeProcess<
    'caller,
>(
    env: EnvUnowned<'caller>,
    _this: JavaObject<'caller>,
    handle: jlong,
    code_point: jint,
) -> JString<'caller> {
    string_result(env, || {
        char::from_u32(code_point as u32)
            .and_then(|key| {
                registry::with_mut(handle, |engine| {
                    engine.process_char(key);
                    engine.buffer().to_owned()
                })
            })
            .unwrap_or_default()
    })
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeBoundary<
    'caller,
>(
    env: EnvUnowned<'caller>,
    _this: JavaObject<'caller>,
    handle: jlong,
    code_point: jint,
) -> JString<'caller> {
    string_result(env, || {
        char::from_u32(code_point as u32)
            .and_then(|key| registry::with_mut(handle, |engine| engine.process_char(key)))
            .filter(|result| result.action != Action::None)
            .map(|result| result.output)
            .unwrap_or_default()
    })
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeBackspace<
    'caller,
>(
    env: EnvUnowned<'caller>,
    _this: JavaObject<'caller>,
    handle: jlong,
) -> JString<'caller> {
    string_result(env, || {
        registry::with_mut(handle, |engine| {
            engine.on_backspace();
            engine.buffer().to_owned()
        })
        .unwrap_or_default()
    })
}
