//! JNI exports for the shared engine's text-expansion table and switches.

use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jboolean, jintArray, jlong};

use super::correction::edit_array;
use super::registry;
use crate::abi::{JavaObject, neutral, safe, string_result};

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

fn shortcut_string(handle: jlong, read: impl Fn(&funput_engine::Engine) -> Option<&str>) -> String {
    registry::with_mut(handle, |engine| read(engine).unwrap_or("").to_owned()).unwrap_or_default()
}

/// Whether Backspace can still put the last expansion back. Empty text means no.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeHasShortcutUndo(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jboolean {
    safe(false, || {
        registry::with_mut(handle, |engine| engine.has_shortcut_undo()).unwrap_or(false)
    })
}

/// The word the `↩` chip names. Empty once the undo is gone.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeShortcutUndoText<
    'caller,
>(
    env: EnvUnowned<'caller>,
    _this: JavaObject<'caller>,
    handle: jlong,
) -> JString<'caller> {
    string_result(env, || {
        shortcut_string(handle, |engine| engine.shortcut_undo_text())
    })
}

/// Expansion plus the boundary character, the suffix Backspace has to see.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeShortcutUndoSuffix<
    'caller,
>(
    env: EnvUnowned<'caller>,
    _this: JavaObject<'caller>,
    handle: jlong,
) -> JString<'caller> {
    string_result(env, || {
        shortcut_string(handle, |engine| engine.shortcut_undo_written())
    })
}

/// `[backspace, codepoint…]` putting the shown word back, or empty when nothing is armed.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeUndoShortcut(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jintArray {
    let edit = registry::with_mut(handle, |engine| engine.undo_shortcut()).flatten();
    edit_array(&mut env, edit)
}

/// Drop the undo and any one-time refusal. The caret has left the field.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeDismissShortcutUndo(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) {
    safe((), || {
        registry::with_mut(handle, |engine| engine.dismiss_shortcut_undo());
    });
}
