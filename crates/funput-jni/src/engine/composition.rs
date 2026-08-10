//! JNI exports for composition input and rendered text.

use funput_engine::Action;
use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jboolean, jint, jlong};

use super::registry;
use crate::abi::{JavaObject, neutral, safe, string_result};

/// Re-open an already-committed word as the live composition, so the next keystroke
/// edits it (Backspace back onto `chào`, then `s` gives `cháo`).
///
/// Returns whether the word was taken — only a Vietnamese syllable is, including one
/// still missing the tone its stop coda needs (`chuc` → `chúc`), so the caller must
/// leave the document untouched when this is `false`.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeAdopt(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    word: JString<'_>,
) -> jboolean {
    safe(false, || {
        let text = neutral(env.with_env(|env| word.try_to_string(env)).into_outcome());
        registry::with_mut(handle, |engine| engine.adopt(&text)).unwrap_or(false)
    })
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
