//! Rebuilding pairs from the journal: what survives a crash before a compact,
//! and what must never be invented on the way back.

use std::fs;

use tempfile::tempdir;

use crate::tests::bigram::followers_of;
use crate::tests::texts;
use crate::{SuggestionConfig, SuggestionEngine};

fn opened(directory: &std::path::Path) -> SuggestionEngine {
    SuggestionEngine::open(directory, SuggestionConfig::default()).unwrap()
}

#[test]
fn adjacent_tokens_come_back_as_a_pair() {
    let directory = tempdir().unwrap();
    {
        let mut engine = opened(directory.path());
        engine.learn_after(None, "xin");
        engine.learn_after(Some("xin"), "chào");
        engine.flush().unwrap();
    }
    let reopened = opened(directory.path());
    assert_eq!(followers_of(&reopened, "xin"), [("chào".to_owned(), 1)]);
}

#[test]
fn a_context_break_stops_two_tokens_being_read_as_a_pair() {
    let directory = tempdir().unwrap();
    {
        let mut engine = opened(directory.path());
        engine.learn_after(None, "xin");
        // A sentence ended here: the platform has no context to vouch for.
        engine.learn_after(None, "chào");
        engine.flush().unwrap();
    }
    let reopened = opened(directory.path());
    assert!(
        followers_of(&reopened, "xin").is_empty(),
        "the two tokens are adjacent in the file but were never adjacent in the text"
    );
}

#[test]
fn a_context_that_is_not_what_the_journal_wrote_last_invents_nothing() {
    let directory = tempdir().unwrap();
    {
        let mut engine = opened(directory.path());
        engine.learn_after(None, "aaa");
        engine.learn_after(None, "bbb");
        // "aaa" is a real context and the edge is real, but the journal's chain
        // runs through "bbb", so adjacency would reconstruct the wrong pair.
        engine.learn_after(Some("aaa"), "ccc");
        assert_eq!(followers_of(&engine, "aaa"), [("ccc".to_owned(), 1)]);
        engine.flush().unwrap();
    }
    let reopened = opened(directory.path());
    assert!(
        followers_of(&reopened, "bbb").is_empty(),
        "replay must never report a pair that was not typed"
    );
    assert!(followers_of(&reopened, "aaa").is_empty());
}

#[test]
fn a_version_one_journal_replays_its_words_and_no_pairs() {
    let directory = tempdir().unwrap();
    let journal = directory.path().join("personal-lexicon.journal");
    {
        // A valid current snapshot, so the journal is read at all.
        let mut engine = opened(directory.path());
        engine.compact().unwrap();
    }
    fs::write(&journal, version_one_frame(&["xin", "chào"])).unwrap();

    let reopened = opened(directory.path());
    assert_eq!(texts(&reopened, "ch"), Vec::<String>::new());
    assert_eq!(
        reopened.stats().words,
        2,
        "both words must still be learned"
    );
    assert!(
        followers_of(&reopened, "xin").is_empty(),
        "a v1 journal records no boundaries, so none of it can be trusted as adjacent"
    );
}

/// A valid v1 journal frame: a flat token list with no context breaks.
fn version_one_frame(tokens: &[&str]) -> Vec<u8> {
    let mut payload = Vec::new();
    payload.extend_from_slice(&(tokens.len() as u32).to_le_bytes());
    for token in tokens {
        payload.extend_from_slice(&(token.len() as u16).to_le_bytes());
        payload.extend_from_slice(token.as_bytes());
    }
    let mut frame = Vec::new();
    frame.extend_from_slice(b"FPJR");
    frame.extend_from_slice(&1u16.to_le_bytes());
    frame.extend_from_slice(&(payload.len() as u32).to_le_bytes());
    frame.extend_from_slice(&crate::persistence::checksum(&payload).to_le_bytes());
    frame.extend_from_slice(&payload);
    frame
}
