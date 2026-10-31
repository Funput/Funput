//! A dictionary listing as an opaque handle the host reads entry by entry.
//!
//! The handle owns a copy of every entry, so it stays valid however the engine
//! changes after it was taken, and a settings list can page through it lazily.

use std::ptr;

use funput_suggestions::DictionaryQuery;

use super::types::{FunputDictionaryEntry, filter, sort};
use crate::abi::safe;
use crate::suggestion::engine::{FunputSuggestionEngine, codepoints_from_raw};
use crate::suggestion::query::decode_valid_codepoints;

/// Opaque, owned list of dictionary entries. Free with
/// `funput_suggestion_dictionary_list_free`.
pub struct FunputDictionaryList {
    entries: Vec<FunputDictionaryEntry>,
}

/// List the dictionary: `filter` is a `FUNPUT_DICTIONARY_FILTER_*`, `sort` a
/// `FUNPUT_DICTIONARY_SORT_*`, and `search` is matched anywhere in a word with
/// marks and case ignored (empty or null matches everything). Null on invalid
/// input.
///
/// # Safety
/// `engine` must be a live suggestion handle or null and may not be used
/// concurrently. `search` must point to `search_len` readable codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_dictionary_list(
    engine: *const FunputSuggestionEngine,
    filter_code: u32,
    sort_code: u32,
    search: *const u32,
    search_len: usize,
) -> *mut FunputDictionaryList {
    safe(ptr::null_mut(), || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return ptr::null_mut();
        };
        let (Some(filter), Some(sort)) = (filter(filter_code), sort(sort_code)) else {
            return ptr::null_mut();
        };
        let Some(search) =
            codepoints_from_raw(search, search_len).and_then(decode_valid_codepoints)
        else {
            return ptr::null_mut();
        };
        let query = DictionaryQuery {
            filter,
            sort,
            search: &search,
        };
        let entries = engine.inner.dictionary(&query);
        boxed(
            entries
                .iter()
                .map(FunputDictionaryEntry::from_entry)
                .collect(),
        )
    })
}

/// List the blocked words, each with origin `FUNPUT_DICTIONARY_ORIGIN_BLOCKED`.
///
/// # Safety
/// `engine` must be a live suggestion handle or null and may not be used concurrently.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_blocked_list(
    engine: *const FunputSuggestionEngine,
) -> *mut FunputDictionaryList {
    safe(ptr::null_mut(), || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return ptr::null_mut();
        };
        boxed(
            engine
                .inner
                .blocked_words()
                .map(FunputDictionaryEntry::blocked)
                .collect(),
        )
    })
}

/// How many entries the list holds; 0 for null.
///
/// # Safety
/// `list` must be a live list handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_dictionary_list_len(
    list: *const FunputDictionaryList,
) -> usize {
    safe(0, || {
        unsafe { list.as_ref() }.map_or(0, |list| list.entries.len())
    })
}

/// Copy entry `index` into `out`. False, with `out` untouched, for a null
/// argument or an index past the end.
///
/// # Safety
/// `list` must be a live list handle or null; `out` must be writable or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_dictionary_list_entry(
    list: *const FunputDictionaryList,
    index: usize,
    out: *mut FunputDictionaryEntry,
) -> bool {
    safe(false, || {
        let (Some(list), Some(out)) = (unsafe { list.as_ref() }, unsafe { out.as_mut() }) else {
            return false;
        };
        list.entries.get(index).map(|entry| *out = *entry).is_some()
    })
}

/// # Safety
/// `list` must be a live list handle or null, and is invalid afterwards.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_dictionary_list_free(list: *mut FunputDictionaryList) {
    safe((), || {
        if !list.is_null() {
            drop(unsafe { Box::from_raw(list) });
        }
    });
}

fn boxed(entries: Vec<FunputDictionaryEntry>) -> *mut FunputDictionaryList {
    Box::into_raw(Box::new(FunputDictionaryList { entries }))
}
