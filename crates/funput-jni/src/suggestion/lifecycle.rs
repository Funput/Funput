//! JNI ownership for the independent personal suggestion engine.

use std::path::Path;

use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jboolean, jlong};

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
            .then(|| registry::open(Path::new(&value)))
            .flatten()
            .unwrap_or(0)
    })
}

/// Attaches the English `en.lex` at `path` so queries fill the slots personal
/// words leave empty. `false` for an empty path, an unknown handle, or a file
/// that will not attach, and the engine keeps the lexicon it had. The decision is
/// `registry::attach_lexicon`, tested there; this only unwraps the Java string.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeAttachLexicon(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    path: JString<'_>,
) -> jboolean {
    safe(false, || {
        let value = neutral(env.with_env(|env| path.try_to_string(env)).into_outcome());
        !value.is_empty() && registry::attach_lexicon(handle, Path::new(&value))
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
