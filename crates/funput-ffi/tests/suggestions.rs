use std::io::Write;

use funput_ffi::{
    FunputSuggestionResult, funput_suggestion_attach_lexicon, funput_suggestion_engine_free,
    funput_suggestion_engine_new_in_memory, funput_suggestion_engine_open, funput_suggestion_flush,
    funput_suggestion_learn, funput_suggestion_learn_after, funput_suggestion_query,
    funput_suggestion_query_with, funput_suggestion_reset, funput_suggestion_stats,
};
use funput_suggestions::lexicon_build::encode;

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

fn learn_after(
    engine: *mut funput_ffi::FunputSuggestionEngine,
    previous: &str,
    text: &str,
) -> bool {
    let previous = codepoints(previous);
    let text = codepoints(text);
    unsafe {
        funput_suggestion_learn_after(
            engine,
            previous.as_ptr(),
            previous.len(),
            text.as_ptr(),
            text.len(),
        )
    }
}

fn query_with(
    engine: *const funput_ffi::FunputSuggestionEngine,
    previous: &str,
    prefix: &str,
) -> FunputSuggestionResult {
    let previous = codepoints(previous);
    let prefix = codepoints(prefix);
    unsafe {
        funput_suggestion_query_with(
            engine,
            previous.as_ptr(),
            previous.len(),
            prefix.as_ptr(),
            prefix.len(),
        )
    }
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

#[test]
fn a_context_reorders_the_candidates_through_c_abi() {
    let engine = funput_suggestion_engine_new_in_memory();
    assert!(!engine.is_null());
    // "chúc" is used more, so frequency alone puts it first.
    for _ in 0..5 {
        assert!(learn(engine, "chúc"));
    }
    assert!(learn(engine, "xin"));
    assert!(learn_after(engine, "xin", "chào"));
    assert!(learn_after(engine, "xin", "chào"));

    let plain = query(engine, "ch");
    assert_eq!(candidate(&plain, 0), "chúc");

    let contextual = query_with(engine, "xin", "ch");
    assert_eq!(candidate(&contextual, 0), "chào");

    // An empty context is a normal thing to say, not a malformed call.
    let none = query_with(engine, "", "ch");
    assert_eq!(candidate(&none, 0), "chúc");
    assert_eq!(none.count, plain.count);

    unsafe { funput_suggestion_engine_free(engine) };
}

#[test]
fn a_null_context_is_the_same_as_no_context() {
    let engine = funput_suggestion_engine_new_in_memory();
    // `ch` is an onset with nothing behind it, so it is promoted on the slow tier like
    // any other token Vietnamese cannot spell. The subject here is the null context.
    for _ in 0..4 {
        assert!(unsafe {
            funput_suggestion_learn_after(engine, std::ptr::null(), 0, [99u32, 104].as_ptr(), 2)
        });
    }
    let result =
        unsafe { funput_suggestion_query_with(engine, std::ptr::null(), 0, [99u32].as_ptr(), 1) };
    assert_eq!(candidate(&result, 0), "ch");
    unsafe { funput_suggestion_engine_free(engine) };
}

/// Four English words, ranked in the order given.
const LEXICON: &str = "what\t0\nwhen\t1\nwhich\t2\nwhale\t3\n";

/// An `en.lex` on disk, built by the same encoder as the shipped one.
fn lexicon_file(tsv: &str) -> tempfile::NamedTempFile {
    let mut file = tempfile::NamedTempFile::new().unwrap();
    file.write_all(&encode(tsv).unwrap()).unwrap();
    file
}

fn attach(engine: *mut funput_ffi::FunputSuggestionEngine, path: &[u8]) -> bool {
    unsafe { funput_suggestion_attach_lexicon(engine, path.as_ptr(), path.len()) }
}

fn path_bytes(file: &tempfile::NamedTempFile) -> Vec<u8> {
    file.path().to_str().unwrap().as_bytes().to_vec()
}

fn texts(result: &FunputSuggestionResult) -> Vec<String> {
    (0..result.count as usize)
        .map(|index| candidate(result, index))
        .collect()
}

#[test]
fn an_attached_lexicon_fills_the_empty_slots_through_c_abi() {
    let lexicon = lexicon_file(LEXICON);
    let engine = funput_suggestion_engine_new_in_memory();
    assert!(attach(engine, &path_bytes(&lexicon)));
    assert_eq!(texts(&query(engine, "wh")), ["what", "when", "which"]);

    // Personal words lead; the lexicon only fills what they leave.
    // Four for an English word, which the syllable rule does not recognise.
    for _ in 0..4 {
        assert!(learn(engine, "whale"));
    }
    assert_eq!(texts(&query(engine, "wh")), ["whale", "what", "when"]);
    assert_eq!(
        texts(&query_with(engine, "", "wh")),
        ["whale", "what", "when"]
    );
    unsafe { funput_suggestion_engine_free(engine) };
}

#[test]
fn a_failed_attach_keeps_the_lexicon_already_attached() {
    let lexicon = lexicon_file(LEXICON);
    let mut damaged = tempfile::NamedTempFile::new().unwrap();
    damaged.write_all(b"FPLX, but not a lexicon").unwrap();
    let mut missing = path_bytes(&lexicon);
    missing.extend_from_slice(b".missing");

    let engine = funput_suggestion_engine_new_in_memory();
    assert!(attach(engine, &path_bytes(&lexicon)));
    for path in [missing, vec![0xff], path_bytes(&damaged)] {
        assert!(!attach(engine, &path), "attached {path:?}");
    }
    assert_eq!(texts(&query(engine, "wh")), ["what", "when", "which"]);
    unsafe { funput_suggestion_engine_free(engine) };
}

#[test]
fn attach_is_null_safe() {
    let lexicon = lexicon_file(LEXICON);
    let path = path_bytes(&lexicon);
    assert!(!unsafe {
        funput_suggestion_attach_lexicon(std::ptr::null_mut(), path.as_ptr(), path.len())
    });

    let engine = funput_suggestion_engine_new_in_memory();
    assert!(!unsafe { funput_suggestion_attach_lexicon(engine, std::ptr::null(), 0) });
    // A null path with a length is malformed and must not be read.
    assert!(!unsafe { funput_suggestion_attach_lexicon(engine, std::ptr::null(), 8) });
    assert_eq!(query(engine, "wh").count, 0);
    unsafe { funput_suggestion_engine_free(engine) };
}

#[test]
fn reset_forgets_the_user_but_keeps_the_lexicon() {
    let directory = tempfile::tempdir().unwrap();
    let store = directory.path().to_string_lossy();
    let lexicon = lexicon_file(LEXICON);
    let engine = unsafe { funput_suggestion_engine_open(store.as_ptr(), store.len()) };
    assert!(attach(engine, &path_bytes(&lexicon)));
    for _ in 0..4 {
        assert!(learn(engine, "whale"));
    }
    assert_eq!(candidate(&query(engine, "wh"), 0), "whale");

    assert!(unsafe { funput_suggestion_reset(engine) });
    assert_eq!(texts(&query(engine, "wh")), ["what", "when", "which"]);
    unsafe { funput_suggestion_engine_free(engine) };
}
