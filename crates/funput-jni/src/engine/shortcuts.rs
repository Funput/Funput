//! JNI exports for the shared engine's text-expansion table and switches.

use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jboolean, jlong};

use super::registry;
use crate::abi::{JavaObject, neutral, safe};

macro_rules! shortcut_switch {
    ($name:ident, $operation:expr) => {
        #[unsafe(no_mangle)]
        pub extern "system" fn $name(_env: EnvUnowned<'_>, _this: JavaObject<'_>, handle: jlong) {
            safe((), || {
                registry::with_mut(handle, $operation);
            });
        }
    };
    ($name:ident, $operation:expr, jboolean) => {
        #[unsafe(no_mangle)]
        pub extern "system" fn $name(
            _env: EnvUnowned<'_>,
            _this: JavaObject<'_>,
            handle: jlong,
            value: jboolean,
        ) {
            safe((), || {
                registry::with_mut(handle, |engine| $operation(engine, value));
            });
        }
    };
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeAddShortcut(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    trigger: JString<'_>,
    expansion: JString<'_>,
) {
    safe((), || {
        let trigger = neutral(
            env.with_env(|env| trigger.try_to_string(env))
                .into_outcome(),
        );
        let expansion = neutral(
            env.with_env(|env| expansion.try_to_string(env))
                .into_outcome(),
        );
        registry::with_mut(handle, |engine| engine.add_shortcut(trigger, expansion));
    });
}

shortcut_switch!(
    Java_app_funput_funput_ime_nativebridge_FunputNative_nativeClearShortcuts,
    |engine: &mut funput_engine::Engine| engine.clear_shortcuts()
);
shortcut_switch!(
    Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetShortcutsEnabled,
    |engine: &mut funput_engine::Engine, value| engine
        .update_config(|config| config.shortcuts_enabled = value),
    jboolean
);
shortcut_switch!(
    Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetShortcutSmartCase,
    |engine: &mut funput_engine::Engine, value| engine
        .update_config(|config| config.shortcut_smart_case = value),
    jboolean
);
shortcut_switch!(
    Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetShortcutsInEnglish,
    |engine: &mut funput_engine::Engine, value| engine
        .update_config(|config| config.shortcuts_in_english = value),
    jboolean
);
