//! JNI exports for typo correction: the handshake the IME runs at a word boundary.
//!
//! Two steps, because `funput-engine` carries no dictionary. The IME reports where
//! each touch landed, types the key as usual, and then — only when
//! `nativeHasPendingCorrection` says so — reads the candidates, weighs them against
//! the personal store, and answers with `nativeApplyCorrection`. Not answering costs
//! nothing: the boundary already did what it always did, and the next keystroke lets
//! the parked correction lapse. A bar that wants to show the repair before the space
//! asks ahead with the calls in `preview`.
//!
//! An edit comes back as an `IntArray` of `[backspace, codepoint…]`, the shape
//! `nativeStats` already uses for numbers, so one call carries both halves and there
//! is no order to get wrong. Kotlin turns the tail into text with
//! `String(edit, 1, edit.size - 1)`.
//!
//! # Layout
//!
//! - this file — the calls that change engine state.
//! - `query` — the read-only calls between the two steps, and the counters.
//! - `preview` — the calls a bar makes before the word ends.
//! - `arrays` — the Java arrays every call above reads or returns.

mod arrays;
// Exports link by `#[unsafe(no_mangle)]` symbol name, so declaring the module is
// all it takes for the JVM to find what is in it.
mod preview;
mod query;

use funput_engine::{Action, ImeResult, KeyTouch};
use jni::EnvUnowned;
use jni::sys::{jfloat, jint, jintArray, jlong};

use super::composition::to_char;
use super::registry;
use crate::abi::{JavaObject, int_array, safe};

/// Report where the finger landed for the key about to be sent.
///
/// Consumed by the next `nativeProcess`/`nativeBoundary`, whatever key that turns out
/// to be. A negative alternate codepoint means "no neighbour here", so the IME passes
/// the same arguments for every key and leaves the unused ones at -1. A word with a
/// key that arrived without a touch is left alone entirely.
///
/// Flat primitives rather than arrays: this runs on every keystroke, and a JNI array
/// per key would allocate on both sides of the boundary.
#[unsafe(no_mangle)]
#[allow(clippy::too_many_arguments)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeSetNextKeyTouch(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    typed: jint,
    typed_distance: jfloat,
    first: jint,
    first_distance: jfloat,
    second: jint,
    second_distance: jfloat,
    third: jint,
    third_distance: jfloat,
) {
    safe((), || {
        let Some(key) = to_char(typed) else {
            return;
        };
        let mut touch = KeyTouch::new(key, typed_distance);
        for (alternate, distance) in [
            (first, first_distance),
            (second, second_distance),
            (third, third_distance),
        ] {
            if let Some(alternate) = to_char(alternate) {
                touch = touch.with_alternate(alternate, distance);
            }
        }
        registry::with_mut(handle, |engine| engine.set_next_key_touch(touch));
    })
}

/// Apply the candidate at `index`, or decline it with a negative index.
///
/// Returns the edit to make: `[backspace, codepoint…]`, or an empty array when there
/// is nothing to do. Declining is a no-op: the boundary keystroke already did
/// everything it would have done without correction, English restore included.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeApplyCorrection(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    index: jint,
) -> jintArray {
    let index = usize::try_from(index).ok();
    let edit = registry::with_mut(handle, |engine| engine.apply_correction(index));
    edit_array(&mut env, edit)
}

/// Undo the correction applied on the previous keystroke, when Backspace is still
/// the very next thing the user does.
///
/// A no-op unless `nativeHasCorrectionUndo` is true, so an ordinary Backspace can
/// never be swallowed here: the IME asks first and calls one or the other.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeUndoCorrection(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jintArray {
    let edit = registry::with_mut(handle, |engine| {
        engine.has_correction_undo().then(|| engine.on_backspace())
    })
    .flatten();
    edit_array(&mut env, edit)
}

/// `[backspace, codepoint…]`, or nothing at all for a no-op. Split from the JNI
/// marshalling below so the shape the IME decodes can be tested without a JVM.
pub(super) fn encode_edit(edit: Option<ImeResult>) -> Vec<jint> {
    match edit {
        Some(result) if result.action != Action::None => {
            std::iter::once(jint::try_from(result.backspace).unwrap_or(jint::MAX))
                .chain(result.output.chars().map(|c| c as jint))
                .collect()
        }
        _ => Vec::new(),
    }
}

/// Marshal an edit into the Java array the IME reads it from.
pub(crate) fn edit_array(env: &mut EnvUnowned<'_>, edit: Option<ImeResult>) -> jintArray {
    int_array(env, &encode_edit(edit))
}

#[cfg(test)]
mod tests;
