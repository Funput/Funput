//! Allocation-bounded JNI query and text-free stats exports.

use funput_suggestions::SuggestionSet;
use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jboolean, jint, jlong, jlongArray, jobjectArray};

use super::registry;
use crate::abi::{JavaObject, long_array, neutral, read_string, safe, string_array};

/// The bar's three slots, as owned text that outlives the engine lock.
fn top(set: SuggestionSet<'_>) -> Vec<String> {
    set.iter().take(3).map(str::to_owned).collect()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeQuery(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    prefix: JString<'_>,
) -> jobjectArray {
    safe(std::ptr::null_mut(), || {
        let text = read_string(&mut env, &prefix);
        let words = registry::with(handle, |engine| top(engine.suggest(&text))).unwrap_or_default();
        string_array(&mut env, &words)
    })
}

/// As `nativeQuery`, with the words that have followed `previous` before moved to
/// the front. A null or empty `previous` means there was no context to vouch for.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeQueryWith(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    previous: JString<'_>,
    prefix: JString<'_>,
) -> jobjectArray {
    safe(std::ptr::null_mut(), || {
        let context = read_string(&mut env, &previous);
        let text = read_string(&mut env, &prefix);
        let context = Some(context.as_str()).filter(|it| !it.is_empty());
        let words = registry::with(handle, |engine| top(engine.suggest_with(context, &text)))
            .unwrap_or_default();
        string_array(&mut env, &words)
    })
}

/// As `nativeQueryWith`, with the words before as one string separated by spaces,
/// oldest first: `"cảm ơn"` reads the two words before. Empty means no context.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeQueryIn(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    context: JString<'_>,
    prefix: JString<'_>,
) -> jobjectArray {
    safe(std::ptr::null_mut(), || {
        let context = read_string(&mut env, &context);
        let text = read_string(&mut env, &prefix);
        let words = registry::with(handle, |engine| {
            top(engine.suggest_after_words(&context, &text))
        })
        .unwrap_or_default();
        string_array(&mut env, &words)
    })
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeStats(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jlongArray {
    safe(std::ptr::null_mut(), || {
        let values = registry::with(handle, |engine| {
            let stats = engine.stats();
            [
                stats.words as i64,
                stats.promoted_words as i64,
                stats.exact_nodes as i64,
                stats.folded_nodes as i64,
                stats.pending_mutations as i64,
                stats.journal_bytes as i64,
                stats.estimated_heap_bytes as i64,
                stats.last_snapshot_bytes as i64,
            ]
        })
        .unwrap_or([0; 8]);
        long_array(&mut env, &values)
    })
}

/// How many times the user has typed `word`, and 0 for one the store has never
/// seen — the `uses` entry that goes into `nativeChooseCorrection`.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeFrequency(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    word: JString<'_>,
) -> jint {
    safe(0, || {
        let text = neutral(env.with_env(|env| word.try_to_string(env)).into_outcome());
        registry::with(handle, |engine| {
            jint::try_from(engine.frequency(&text)).unwrap_or(jint::MAX)
        })
        .unwrap_or(0)
    })
}

/// Whether `word` is a word at all: one the user has typed, or one in the attached
/// English list.
///
/// The veto behind typo correction. Without it the IME would let a correction rewrite
/// `text ` as `tẻ`, since `r` sits beside `t` and the engine has no dictionary to know
/// better.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeIsKnownWord(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    word: JString<'_>,
) -> jboolean {
    safe(false, || {
        let text = neutral(env.with_env(|env| word.try_to_string(env)).into_outcome());
        registry::with(handle, |engine| engine.is_known_word(&text)).unwrap_or(false)
    })
}
