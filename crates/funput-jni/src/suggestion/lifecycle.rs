//! JNI ownership for the independent personal suggestion engine.

use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::jlong;

use super::registry;
use crate::abi::{JavaObject, neutral, safe};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeCreate(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
) -> jlong {
    safe(0, registry::create)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeOpen(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    path: JString<'_>,
) -> jlong {
    safe(0, || {
        let value = neutral(env.with_env(|env| path.try_to_string(env)).into_outcome());
        (!value.is_empty())
            .then(|| registry::open(std::path::Path::new(&value)))
            .flatten()
            .unwrap_or(0)
    })
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeDestroy(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) {
    safe((), || registry::destroy(handle));
}
