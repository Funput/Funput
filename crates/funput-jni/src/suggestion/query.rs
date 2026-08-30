//! Allocation-bounded JNI query and text-free stats exports.

use jni::EnvUnowned;
use jni::objects::{JLongArray, JObjectArray, JString};
use jni::sys::{jlong, jlongArray, jobjectArray};

use super::registry;
use crate::abi::{JavaObject, neutral, safe};

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeQuery(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    prefix: JString<'_>,
) -> jobjectArray {
    safe(std::ptr::null_mut(), || {
        let result = env
            .with_env(|env| -> jni::errors::Result<_> {
                let text = prefix.try_to_string(env)?;
                let words = registry::with(handle, |engine| {
                    engine
                        .suggest(&text)
                        .iter()
                        .take(3)
                        .map(str::to_owned)
                        .collect::<Vec<_>>()
                })
                .unwrap_or_default();
                candidates(env, &words)
            })
            .into_outcome();
        neutral(result)
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
        let result = env
            .with_env(|env| -> jni::errors::Result<_> {
                let context = previous.try_to_string(env)?;
                let text = prefix.try_to_string(env)?;
                let words = registry::with(handle, |engine| {
                    engine
                        .suggest_with(Some(context.as_str()).filter(|it| !it.is_empty()), &text)
                        .iter()
                        .take(3)
                        .map(str::to_owned)
                        .collect::<Vec<_>>()
                })
                .unwrap_or_default();
                candidates(env, &words)
            })
            .into_outcome();
        neutral(result)
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
        let result = env
            .with_env(|env| -> jni::errors::Result<_> {
                let array = JLongArray::new(env, values.len())?;
                array.set_region(env, 0, &values)?;
                Ok(array.into_raw())
            })
            .into_outcome();
        neutral(result)
    })
}

fn candidates(env: &mut jni::Env<'_>, words: &[String]) -> jni::errors::Result<jobjectArray> {
    let initial = JString::from_str(env, words.first().map(String::as_str).unwrap_or(""))?;
    let array = JObjectArray::<JString>::new(env, words.len(), &initial)?;
    for (index, word) in words.iter().enumerate().skip(1) {
        let value = JString::from_str(env, word)?;
        array.set_element(env, index, &value)?;
    }
    Ok(array.into_raw())
}
