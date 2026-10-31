//! Listing the dictionary as parallel arrays: the words, then their details.
//!
//! The two calls take the same query and read the same handle. The app owns its
//! snapshot handle alone and asks both in turn, so they always describe the same
//! listing.

use funput_suggestions::{DictionaryQuery, SuggestionEngine};
use jni::EnvUnowned;
use jni::objects::JString;
use jni::sys::{jint, jlong, jlongArray, jobjectArray};

use super::codes;
use crate::abi::{JavaObject, long_array, read_string, safe, string_array};
use crate::suggestion::registry;

#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeDictionaryWords(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    filter: jint,
    sort: jint,
    search: JString<'_>,
) -> jobjectArray {
    safe(std::ptr::null_mut(), || {
        let search = read_string(&mut env, &search);
        match listing(handle, filter, sort, &search) {
            Some((words, _)) => string_array(&mut env, &words),
            None => std::ptr::null_mut(),
        }
    })
}

/// `DETAILS_PER_ENTRY` numbers per word of `nativeDictionaryWords`, in its order.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeDictionaryDetails(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
    filter: jint,
    sort: jint,
    search: JString<'_>,
) -> jlongArray {
    safe(std::ptr::null_mut(), || {
        let search = read_string(&mut env, &search);
        match listing(handle, filter, sort, &search) {
            Some((_, details)) => long_array(&mut env, &details),
            None => std::ptr::null_mut(),
        }
    })
}

/// The blocked words, in code point order.
#[unsafe(no_mangle)]
pub extern "system" fn Java_app_funput_funput_ime_nativebridge_PersonalSuggestionNative_nativeBlockedWords(
    mut env: EnvUnowned<'_>,
    _this: JavaObject<'_>,
    handle: jlong,
) -> jobjectArray {
    safe(std::ptr::null_mut(), || {
        let words = registry::with(handle, blocked).unwrap_or_default();
        string_array(&mut env, &words)
    })
}

/// `None` for an unknown handle, filter or sort.
pub(super) fn listing(
    handle: i64,
    filter: i32,
    sort: i32,
    search: &str,
) -> Option<(Vec<String>, Vec<i64>)> {
    let query = DictionaryQuery {
        filter: codes::filter(filter)?,
        sort: codes::sort(sort)?,
        search,
    };
    registry::with(handle, |engine| codes::flatten(&engine.dictionary(&query)))
}

fn blocked(engine: &SuggestionEngine) -> Vec<String> {
    engine.blocked_words().map(str::to_owned).collect()
}
