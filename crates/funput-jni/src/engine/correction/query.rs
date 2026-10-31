//! The read-only half of the handshake: what the IME reads between typing the
//! boundary key and answering, and the counters it reports at the end of a session.

use funput_engine::CorrectionMetrics;
use jni::EnvUnowned;
use jni::objects::{JBooleanArray, JIntArray};
use jni::sys::{jboolean, jint, jintArray, jlong, jobjectArray};

use super::arrays::{read_bools, read_ints};
use super::registry;
use crate::abi::{JavaObject, int_array, neutral, safe, string_array};

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
        string_array(&mut env, &words)
    })
}

/// Rank the parked candidates with the personal store's use counts folded in and
/// return the winner, or `-1` when the top two are too close to call — a pair to
/// offer on the suggestion bar rather than an edit to make.
///
/// `uses` and `allowed` are both parallel to `nativeCorrectionCandidates`. A shorter
/// or null `uses` reads its missing entries as zero; a shorter or null `allowed` reads
/// its missing entries as permitted.
///
/// `allowed` is how the IME refuses to correct *into* a word its dictionary does not
/// know. The formula lives in Rust so the confidence margin has one definition across
/// the platforms.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeChooseCorrection(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    uses: JIntArray<'_>,
    allowed: JBooleanArray<'_>,
) -> jint {
    safe(-1, || {
        let counts = neutral(env.with_env(|env| read_ints(env, &uses)).into_outcome());
        let allowed = neutral(env.with_env(|env| read_bools(env, &allowed)).into_outcome());
        registry::with_mut(handle, |engine| {
            engine
                .choose_correction(&counts, &allowed)
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

/// The engine's correction counters since it was created, for the IME's end-of-session
/// log. Counts only, never words: see `encode_metrics` for the order.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeCorrectionMetrics(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jintArray {
    let metrics = registry::with_mut(handle, |engine| engine.correction_metrics());
    int_array(&mut env, &encode_metrics(&metrics.unwrap_or_default()))
}

/// The counters in the order `FunputCorrectionMetrics` declares them in the C header,
/// so both platforms log the same line. A count past `i32::MAX` clamps, never wraps.
pub(super) fn encode_metrics(metrics: &CorrectionMetrics) -> [jint; 10] {
    [
        metrics.applied,
        metrics.reverted,
        metrics.skipped_ambiguous,
        metrics.candidates_max,
        metrics.microseconds_max,
        metrics.kept_as_typed,
        metrics.offered,
        metrics.no_candidate,
        metrics.suppressed,
        metrics.valid_near_edge,
    ]
    .map(|count| jint::try_from(count).unwrap_or(jint::MAX))
}
