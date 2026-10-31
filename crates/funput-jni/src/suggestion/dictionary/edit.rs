//! Changing the dictionary: through a handle, or, for the app, by posting to the
//! inbox of a store the keyboard owns.

use std::path::Path;

use funput_suggestions::{Edit, post_edits};
use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jboolean, jint, jlong};

use super::codes;
use crate::abi::{JavaObject, read_string, safe};
use crate::suggestion::registry;

/// Applies one edit and answers with its outcome code. On a snapshot handle this
/// changes only that view, which is how the app previews an edit under the
/// engine's own rules before posting it.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeApplyEdit(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    kind: jint,
    word: JString<'_>,
) -> jint {
    safe(codes::INVALID, || {
        let Some(kind) = codes::edit_kind(kind) else {
            return codes::INVALID;
        };
        let text = read_string(&mut env, &word);
        registry::with_mut(handle, |engine| {
            codes::outcome(engine.apply_edit(Edit::new(kind, &text)))
        })
        .unwrap_or(codes::INVALID)
    })
}

/// Folds in what the app posted, journals it, then clears the inbox. How many
/// edits were read, or -1 when the inbox or the journal could not be used; the
/// edits then wait for the next call.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeApplyPendingEdits(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jint {
    safe(-1, || {
        registry::with_mut(handle, |engine| match engine.apply_pending_edits() {
            Ok(count) => jint::try_from(count).unwrap_or(jint::MAX),
            Err(_) => -1,
        })
        .unwrap_or(-1)
    })
}

/// Posts one edit for the owner of the store at `path` to apply. Needs no handle
/// and writes one new file, so it is safe while the keyboard has the store open.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativePostEdit(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    path: JString<'_>,
    kind: jint,
    word: JString<'_>,
) -> jboolean {
    safe(false, || {
        let Some(kind) = codes::edit_kind(kind) else {
            return false;
        };
        let path = read_string(&mut env, &path);
        let text = read_string(&mut env, &word);
        !path.is_empty() && post_edits(Path::new(&path), &[Edit::new(kind, &text)]).is_ok()
    })
}
