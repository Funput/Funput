//! Asking before the word ends: preview the repair, rank it, or turn it down.
//!
//! The three calls a suggestion bar runs while a word is still being typed, and the
//! text its undo chip shows once a repair has landed. The
//! ranking comes back as an `IntArray` `[best, second, verdict]` — `-1` for a slot
//! with nothing in it, and the verdict numbered as the C header does: 0 apply,
//! 1 ambiguous, 2 as typed, 3 refused.

use funput_engine::{CorrectionRanking, CorrectionVerdict};
use jni::EnvUnowned;
use jni::objects::{JBooleanArray, JIntArray, JString};
use jni::sys::{jint, jintArray, jlong};

use super::arrays::{read_bools, read_ints};
use super::registry;
use crate::abi::{JavaObject, int_array, neutral, safe, string_result};

/// Search the word being typed the way the boundary will, and return how many
/// candidates it reaches. 0 while the word could still become a syllable, for a word
/// correction leaves alone, and for one the user already refused. The candidates
/// stand until the next letter key, and the boundary reuses them.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativePreviewCorrection(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jint {
    safe(0, || {
        registry::with_mut(handle, |engine| {
            jint::try_from(engine.preview_correction()).unwrap_or(0)
        })
        .unwrap_or(0)
    })
}

/// The user tapped the word exactly as they typed it: leave it, and do not offer
/// this spelling again the next time it is typed. Drops any preview, parked
/// correction and armed undo.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeRefuseCorrection(
    _env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) {
    safe((), || {
        registry::with_mut(handle, |engine| engine.refuse_correction());
    })
}

/// Rank the candidates in hand — a preview's, or a parked correction's — with the
/// personal store's use counts folded in. `uses` and `allowed` read exactly as in
/// `nativeChooseCorrection`; unlike it, this counts nothing.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_FunputNative_nativeRankCorrection(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    uses: JIntArray<'_>,
    allowed: JBooleanArray<'_>,
) -> jintArray {
    safe(std::ptr::null_mut(), || {
        let counts = neutral(env.with_env(|env| read_ints(env, &uses)).into_outcome());
        let allowed = neutral(env.with_env(|env| read_bools(env, &allowed)).into_outcome());
        let ranking =
            registry::with_mut(handle, |engine| engine.rank_correction(&counts, &allowed))
                .unwrap_or(CorrectionRanking::REFUSED);
        int_array(&mut env, &encode_ranking(ranking))
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

/// `[best, second, verdict]`, split from the marshalling so the shape the IME
/// decodes can be tested without a JVM.
pub(super) fn encode_ranking(ranking: CorrectionRanking) -> [jint; 3] {
    let index = |slot: Option<usize>| slot.and_then(|i| jint::try_from(i).ok()).unwrap_or(-1);
    let verdict = match ranking.verdict {
        CorrectionVerdict::Apply => 0,
        CorrectionVerdict::Ambiguous => 1,
        CorrectionVerdict::AsTyped => 2,
        CorrectionVerdict::Refused => 3,
    };
    [index(ranking.best), index(ranking.second), verdict]
}
