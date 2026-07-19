use funput_ffi::{
    FunputSuggestionResult, funput_suggestion_engine_free, funput_suggestion_engine_new_in_memory,
    funput_suggestion_engine_open, funput_suggestion_flush, funput_suggestion_learn,
    funput_suggestion_query, funput_suggestion_reset, funput_suggestion_stats,
};

fn codepoints(text: &str) -> Vec<u32> {
    text.chars().map(u32::from).collect()
}

fn learn(engine: *mut funput_ffi::FunputSuggestionEngine, text: &str) -> bool {
    let text = codepoints(text);
    unsafe { funput_suggestion_learn(engine, text.as_ptr(), text.len()) }
}

fn query(
    engine: *const funput_ffi::FunputSuggestionEngine,
    prefix: &str,
) -> FunputSuggestionResult {
    let prefix = codepoints(prefix);
    unsafe { funput_suggestion_query(engine, prefix.as_ptr(), prefix.len()) }
}

fn candidate(result: &FunputSuggestionResult, index: usize) -> String {
    let candidate = &result.candidates[index];
    candidate.chars[..candidate.count as usize]
        .iter()
        .filter_map(|value| char::from_u32(*value))
        .collect()
}

#[test]
fn learns_queries_and_resets_through_c_abi() {
    let engine = funput_suggestion_engine_new_in_memory();
    assert!(!engine.is_null());
    assert!(learn(engine, "chào"));
    assert_eq!(query(engine, "ch").count, 0);
    assert!(learn(engine, "chào"));
    let result = query(engine, "ch");
    assert_eq!(result.count, 1);
    assert_eq!(candidate(&result, 0), "chào");
    assert_eq!(unsafe { funput_suggestion_stats(engine) }.words, 1);
    assert!(unsafe { funput_suggestion_flush(engine) });
    assert!(unsafe { funput_suggestion_reset(engine) });
    assert_eq!(query(engine, "ch").count, 0);
    unsafe { funput_suggestion_engine_free(engine) };
}

#[test]
fn null_and_invalid_inputs_fail_silently() {
    let empty = unsafe { funput_suggestion_query(std::ptr::null(), std::ptr::null(), 0) };
    assert_eq!(empty.count, 0);
    assert!(!unsafe { funput_suggestion_learn(std::ptr::null_mut(), std::ptr::null(), 0) });
    let engine = funput_suggestion_engine_new_in_memory();
    let invalid = [0x11_0000u32];
    assert!(!unsafe { funput_suggestion_learn(engine, invalid.as_ptr(), 1) });
    assert_eq!(
        unsafe { funput_suggestion_query(engine, invalid.as_ptr(), 1) }.count,
        0
    );
    unsafe { funput_suggestion_engine_free(engine) };
}

#[test]
fn persistent_handle_round_trips_without_owned_results() {
    let directory = tempfile::tempdir().unwrap();
    let path = directory.path().to_string_lossy();
    let engine = unsafe { funput_suggestion_engine_open(path.as_ptr(), path.len()) };
    assert!(!engine.is_null());
    assert!(learn(engine, "đường"));
    assert!(learn(engine, "đường"));
    assert!(unsafe { funput_ffi::funput_suggestion_compact(engine) });
    unsafe { funput_suggestion_engine_free(engine) };

    let reopened = unsafe { funput_suggestion_engine_open(path.as_ptr(), path.len()) };
    let result = query(reopened, "du");
    assert_eq!(result.count, 1);
    assert_eq!(candidate(&result, 0), "đường");
    unsafe { funput_suggestion_engine_free(reopened) };
}
