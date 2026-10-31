//! Edits survive a reopen whichever file they reached: the journal, the snapshot,
//! or both after a crash between the two halves of a compact.

use std::fs;
use std::path::Path;

use tempfile::tempdir;

use super::{apply, blocked, listed};
use crate::binary::checksum;
use crate::tests::{learned, texts};
use crate::{EditKind, Origin, SuggestionConfig, SuggestionEngine};

fn opened(directory: &Path) -> SuggestionEngine {
    SuggestionEngine::open(directory, SuggestionConfig::default()).unwrap()
}

fn edited(directory: &Path) {
    let mut engine = opened(directory);
    learned(&mut engine, "xin", 2);
    learned(&mut engine, "chào", 2);
    apply(&mut engine, EditKind::Add, "funput");
    apply(&mut engine, EditKind::Forget, "xin");
    apply(&mut engine, EditKind::Block, "đm");
    learned(&mut engine, "chào", 1);
    engine.flush().unwrap();
}

fn assert_edited(engine: &SuggestionEngine) {
    assert_eq!(listed(engine), ["chào", "funput"]);
    assert_eq!(engine.frequency("chào"), 3);
    assert_eq!(blocked(engine), ["đm"]);
    let added = engine.dictionary(&Default::default());
    assert_eq!(added[1].origin, Origin::Added);
}

#[test]
fn edits_replay_from_the_journal() {
    let directory = tempdir().unwrap();
    edited(directory.path());
    assert_edited(&opened(directory.path()));
}

#[test]
fn edits_survive_a_compact() {
    let directory = tempdir().unwrap();
    edited(directory.path());
    opened(directory.path()).compact().unwrap();
    assert_edited(&opened(directory.path()));
}

#[test]
fn a_journal_that_outlived_its_compact_applies_no_edit_twice() {
    let directory = tempdir().unwrap();
    edited(directory.path());
    let journal = directory.path().join("personal-lexicon.journal");
    let stale = fs::read(&journal).unwrap();
    opened(directory.path()).compact().unwrap();
    // The crash: the snapshot was renamed into place, the journal never truncated.
    fs::write(&journal, stale).unwrap();
    assert_edited(&opened(directory.path()));
}

#[test]
fn replay_keeps_edits_a_since_lowered_limit_would_refuse() {
    let directory = tempdir().unwrap();
    {
        let mut engine = opened(directory.path());
        for word in ["một", "hai", "ba"] {
            apply(&mut engine, EditKind::Block, word);
        }
        engine.flush().unwrap();
    }
    let lowered = SuggestionConfig {
        max_blocked_words: 1,
        ..SuggestionConfig::default()
    };
    let engine = SuggestionEngine::open(directory.path(), lowered).unwrap();
    assert_eq!(blocked(&engine), ["ba", "hai", "một"]);
}

#[test]
fn a_version_two_snapshot_and_a_version_three_journal_still_load() {
    let directory = tempdir().unwrap();
    fs::write(
        directory.path().join("personal-lexicon.snapshot"),
        version_two_snapshot(&[("chào", 2, 1)], 1),
    )
    .unwrap();
    fs::write(
        directory.path().join("personal-lexicon.journal"),
        version_three_frame(&[("chào", false), ("bạn", false), ("bạn", false)], 4),
    )
    .unwrap();
    let engine = opened(directory.path());
    assert_eq!(texts(&engine, "ch"), ["chào"]);
    assert_eq!(engine.frequency("chào"), 3);
    assert_eq!(texts(&engine, "b"), ["bạn"]);
}

/// A valid v2 snapshot: words with an empty follower section and no flags.
fn version_two_snapshot(words: &[(&str, u32, u64)], sequence: u64) -> Vec<u8> {
    let mut bytes = Vec::new();
    bytes.extend_from_slice(b"FPSNAP01");
    bytes.extend_from_slice(&2u16.to_le_bytes());
    bytes.extend_from_slice(&sequence.to_le_bytes());
    bytes.extend_from_slice(&(words.len() as u32).to_le_bytes());
    for (text, uses, last_used) in words {
        bytes.extend_from_slice(&(text.len() as u16).to_le_bytes());
        bytes.extend_from_slice(text.as_bytes());
        bytes.extend_from_slice(&uses.to_le_bytes());
        bytes.extend_from_slice(&last_used.to_le_bytes());
        bytes.extend_from_slice(&[0, 0, 0]);
    }
    let sum = checksum(&bytes);
    bytes.extend_from_slice(&sum.to_le_bytes());
    bytes
}

/// A valid v3 journal frame: a flag byte per token, and the closing sequence.
fn version_three_frame(tokens: &[(&str, bool)], sequence: u64) -> Vec<u8> {
    let mut payload = sequence.to_le_bytes().to_vec();
    payload.extend_from_slice(&(tokens.len() as u32).to_le_bytes());
    for (token, chained) in tokens {
        payload.extend_from_slice(&(token.len() as u16).to_le_bytes());
        payload.push(u8::from(*chained));
        payload.extend_from_slice(token.as_bytes());
    }
    let mut frame = b"FPJR".to_vec();
    frame.extend_from_slice(&3u16.to_le_bytes());
    frame.extend_from_slice(&(payload.len() as u32).to_le_bytes());
    frame.extend_from_slice(&checksum(&payload).to_le_bytes());
    frame.extend_from_slice(&payload);
    frame
}
