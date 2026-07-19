//! JNI mutation and persistence operations for personal suggestions.

use funput_suggestions::LearnOutcome;
use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jboolean, jlong};

use crate::suggestion_registry;
use crate::support::{JavaObject, neutral, safe};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeLearn(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    token: JString<'_>,
) -> jboolean {
    safe(false, || {
        let text = neutral(env.with_env(|env| token.try_to_string(env)).into_outcome());
        if text.is_empty() {
            return false;
        }
        suggestion_registry::with_mut(handle, |engine| {
            !matches!(engine.learn(&text), LearnOutcome::Ignored)
        })
        .unwrap_or(false)
    })
}

macro_rules! store_operation {
    ($name:ident, $operation:ident) => {
        #[unsafe(no_mangle)]
        pub extern "system" fn $name(
            _env: EnvUnowned<'_>,
            _this: JavaObject<'_>,
            handle: jlong,
        ) -> jboolean {
            safe(false, || {
                suggestion_registry::with_mut(handle, |engine| engine.$operation().is_ok())
                    .unwrap_or(false)
            })
        }
    };
}

store_operation!(
    Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeFlush,
    flush
);
store_operation!(
    Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeCompact,
    compact
);
store_operation!(
    Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeReset,
    reset
);
