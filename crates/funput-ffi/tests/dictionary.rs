//! The personal dictionary through the C ABI: the keyboard's handle edits and
//! lists, the app reads a snapshot and posts edits, and bad input is refused.

use std::ptr;

use funput_ffi::{
    FUNPUT_DICTIONARY_APPLIED, FUNPUT_DICTIONARY_EDIT_ADD, FUNPUT_DICTIONARY_EDIT_BLOCK,
    FUNPUT_DICTIONARY_FILTER_ALL, FUNPUT_DICTIONARY_INVALID, FUNPUT_DICTIONARY_ORIGIN_ADDED,
    FUNPUT_DICTIONARY_ORIGIN_BLOCKED, FUNPUT_DICTIONARY_SORT_MOST_USED,
    FUNPUT_DICTIONARY_UNCHANGED, FunputDictionaryEntry, FunputDictionaryList,
    FunputSuggestionEngine, funput_suggestion_apply_edit, funput_suggestion_apply_pending_edits,
    funput_suggestion_blocked_list, funput_suggestion_dictionary_list,
    funput_suggestion_dictionary_list_entry, funput_suggestion_dictionary_list_free,
    funput_suggestion_dictionary_list_len, funput_suggestion_engine_free,
    funput_suggestion_engine_new_in_memory, funput_suggestion_engine_open,
    funput_suggestion_engine_open_snapshot, funput_suggestion_post_edit,
};

fn codepoints(text: &str) -> Vec<u32> {
    text.chars().map(u32::from).collect()
}

fn apply(engine: *mut FunputSuggestionEngine, kind: u32, word: &str) -> u32 {
    let word = codepoints(word);
    unsafe { funput_suggestion_apply_edit(engine, kind, word.as_ptr(), word.len()) }
}

fn post(path: &str, kind: u32, word: &str) -> bool {
    let word = codepoints(word);
    unsafe {
        funput_suggestion_post_edit(path.as_ptr(), path.len(), kind, word.as_ptr(), word.len())
    }
}

/// Reads a list handle out entry by entry, and frees it.
fn drain(list: *mut FunputDictionaryList) -> Vec<(String, u32)> {
    assert!(!list.is_null());
    let len = unsafe { funput_suggestion_dictionary_list_len(list) };
    let entries = (0..len)
        .map(|index| {
            let mut entry = FunputDictionaryEntry {
                count: 0,
                chars: [0; 32],
                uses: 0,
                origin: 0,
                last_used: 0,
            };
            assert!(unsafe { funput_suggestion_dictionary_list_entry(list, index, &mut entry) });
            let text = entry.chars[..entry.count as usize]
                .iter()
                .filter_map(|&code| char::from_u32(code))
                .collect();
            (text, entry.origin)
        })
        .collect();
    unsafe { funput_suggestion_dictionary_list_free(list) };
    entries
}

fn listed(engine: *const FunputSuggestionEngine) -> Vec<(String, u32)> {
    drain(unsafe {
        funput_suggestion_dictionary_list(
            engine,
            FUNPUT_DICTIONARY_FILTER_ALL,
            FUNPUT_DICTIONARY_SORT_MOST_USED,
            ptr::null(),
            0,
        )
    })
}

#[test]
fn edits_and_listings_round_trip_through_the_handle() {
    let engine = funput_suggestion_engine_new_in_memory();
    assert_eq!(
        apply(engine, FUNPUT_DICTIONARY_EDIT_ADD, "Funput"),
        FUNPUT_DICTIONARY_APPLIED
    );
    assert_eq!(
        apply(engine, FUNPUT_DICTIONARY_EDIT_ADD, "funput"),
        FUNPUT_DICTIONARY_UNCHANGED
    );
    assert_eq!(
        apply(engine, FUNPUT_DICTIONARY_EDIT_BLOCK, "đm"),
        FUNPUT_DICTIONARY_APPLIED
    );

    assert_eq!(
        listed(engine),
        [("funput".to_owned(), FUNPUT_DICTIONARY_ORIGIN_ADDED)]
    );
    let blocked = drain(unsafe { funput_suggestion_blocked_list(engine) });
    assert_eq!(
        blocked,
        [("đm".to_owned(), FUNPUT_DICTIONARY_ORIGIN_BLOCKED)]
    );
    unsafe { funput_suggestion_engine_free(engine) };
}

#[test]
fn bad_input_is_refused_and_null_is_harmless() {
    let engine = funput_suggestion_engine_new_in_memory();
    assert_eq!(apply(engine, 99, "a"), FUNPUT_DICTIONARY_INVALID);
    let surrogate = [0xD800u32];
    let outcome = unsafe {
        funput_suggestion_apply_edit(engine, FUNPUT_DICTIONARY_EDIT_ADD, surrogate.as_ptr(), 1)
    };
    assert_eq!(outcome, FUNPUT_DICTIONARY_INVALID);
    let list = unsafe { funput_suggestion_dictionary_list(engine, 9, 0, ptr::null(), 0) };
    assert!(list.is_null());
    let mut entry = std::mem::MaybeUninit::<FunputDictionaryEntry>::uninit();
    assert!(!unsafe {
        funput_suggestion_dictionary_list_entry(ptr::null(), 0, entry.as_mut_ptr())
    });
    assert_eq!(
        apply(ptr::null_mut(), FUNPUT_DICTIONARY_EDIT_ADD, "a"),
        FUNPUT_DICTIONARY_INVALID
    );
    assert_eq!(
        unsafe { funput_suggestion_apply_pending_edits(ptr::null_mut()) },
        -1
    );
    unsafe { funput_suggestion_dictionary_list_free(ptr::null_mut()) };
    unsafe { funput_suggestion_engine_free(engine) };
}

#[test]
fn the_app_posts_and_the_keyboard_applies() {
    let directory = tempfile::tempdir().unwrap();
    let path = directory.path().to_str().unwrap();
    let keyboard = unsafe { funput_suggestion_engine_open(path.as_ptr(), path.len()) };
    assert!(!keyboard.is_null());

    assert!(post(path, FUNPUT_DICTIONARY_EDIT_ADD, "funput"));
    assert!(!post(path, 99, "funput"));
    let view = unsafe { funput_suggestion_engine_open_snapshot(path.as_ptr(), path.len()) };
    assert_eq!(
        listed(view),
        [("funput".to_owned(), FUNPUT_DICTIONARY_ORIGIN_ADDED)]
    );
    unsafe { funput_suggestion_engine_free(view) };

    assert_eq!(
        unsafe { funput_suggestion_apply_pending_edits(keyboard) },
        1
    );
    assert_eq!(
        listed(keyboard),
        [("funput".to_owned(), FUNPUT_DICTIONARY_ORIGIN_ADDED)]
    );
    unsafe { funput_suggestion_engine_free(keyboard) };
}
