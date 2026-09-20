//! The read-only half of the handshake: what the IME reads between typing the
//! boundary key and answering.

use jni::EnvUnowned;
use jni::objects::{JIntArray, JObjectArray, JString};
use jni::sys::{jboolean, jint, jlong, jobjectArray};

use super::registry;
use crate::abi::{JavaObject, neutral, safe, string_result};

/// Whether the last keystroke ended a word on a correction still waiting for an
/// answer.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeHasPendingCorrection(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jboolean {
    safe(false, || {
        registry::with_mut(handle, |engine| engine.has_pending_correction()).unwrap_or(false)
    })
}

/// The words the parked correction can reach, best touch score first. Empty when
/// nothing is pending.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeCorrectionCandidates(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jobjectArray {
    safe(std::ptr::null_mut(), || {
        let words = registry::with_mut(handle, |engine| {
            engine
                .correction_candidates()
                .iter()
                .map(|candidate| candidate.text().to_owned())
                .collect::<Vec<_>>()
        })
        .unwrap_or_default();
        let result = env
            .with_env(|env| -> jni::errors::Result<_> {
                let initial = JString::from_str(env, words.first().map_or("", String::as_str))?;
                let array = JObjectArray::<JString>::new(env, words.len(), &initial)?;
                for (index, word) in words.iter().enumerate().skip(1) {
                    let value = JString::from_str(env, word)?;
                    array.set_element(env, index, &value)?;
                }
                Ok(array.into_raw())
            })
            .into_outcome();
        neutral(result)
    })
}

/// Rank the parked candidates with the personal store's use counts folded in and
/// return the winner, or `-1` when the top two are too close to call — a pair to
/// offer on the suggestion bar rather than an edit to make.
///
/// `uses` is parallel to `nativeCorrectionCandidates`; a shorter or null array reads
/// the missing entries as zero. The formula lives in Rust so the confidence margin
/// has one definition across the platforms.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeChooseCorrection(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    uses: JIntArray<'_>,
) -> jint {
    safe(-1, || {
        let counts = env
            .with_env(|env| -> jni::errors::Result<Vec<jint>> {
                let len = uses.len(env).unwrap_or(0);
                let mut values = vec![0; len];
                if len > 0 {
                    uses.get_region(env, 0, &mut values)?;
                }
                Ok(values)
            })
            .into_outcome();
        let counts: Vec<u32> = neutral(counts)
            .into_iter()
            .map(|count| u32::try_from(count).unwrap_or(0))
            .collect();
        registry::with_mut(handle, |engine| {
            engine
                .choose_correction(&counts)
                .and_then(|index| jint::try_from(index).ok())
                .unwrap_or(-1)
        })
        .unwrap_or(-1)
    })
}

/// How many characters applying a candidate will delete: the word as the app shows
/// it, plus the boundary character already in the document. 0 when nothing is
/// pending.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativePendingCorrectionBackspace(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jint {
    safe(0, || {
        registry::with_mut(handle, |engine| {
            jint::try_from(engine.pending_correction_backspace()).unwrap_or(0)
        })
        .unwrap_or(0)
    })
}

/// Whether Backspace would undo the last correction rather than delete a character.
/// The IME asks this before every Backspace and calls `nativeUndoCorrection` instead
/// when it is true.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeHasCorrectionUndo(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jboolean {
    safe(false, || {
        registry::with_mut(handle, |engine| engine.has_correction_undo()).unwrap_or(false)
    })
}

/// The word the user actually typed, while the undo is still one Backspace away —
/// what the `↩ dduwowfnh` chip shows. Empty once the undo is gone.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeCorrectionUndoText<
    'caller,
>(
    env: EnvUnowned<'caller>,
    _this: JavaObject<'caller>,
    handle: jlong,
) -> JString<'caller> {
    string_result(env, || {
        registry::with_mut(handle, |engine| {
            engine.correction_undo_text().unwrap_or_default().to_owned()
        })
        .unwrap_or_default()
    })
}
