//! JNI exports for composition input and rendered text.

use funput_engine::Action;
use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jint, jlong};

use super::registry;
use crate::abi::{JavaObject, string_result};

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
        to_char(code_point)
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
        to_char(code_point)
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

fn to_char(code_point: jint) -> Option<char> {
    char::from_u32(code_point as u32)
}
