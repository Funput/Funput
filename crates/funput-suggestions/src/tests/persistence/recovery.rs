use std::fs::{self, OpenOptions};
use std::io::Write;

use tempfile::tempdir;

use crate::tests::{learned, texts};
use crate::{SuggestionConfig, SuggestionEngine};

#[test]
fn truncated_journal_keeps_complete_frames() {
    let directory = tempdir().unwrap();
    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    learned(&mut engine, "một", 2);
    engine.compact().unwrap();
    learned(&mut engine, "mới", 2);
    engine.flush().unwrap();
    let journal = directory.path().join("personal-lexicon.journal");
    OpenOptions::new()
        .append(true)
        .open(journal)
        .unwrap()
        .write_all(b"FPJ")
        .unwrap();
    drop(engine);
    let reopened = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(texts(&reopened, "m"), ["mới", "một"]);
}

#[test]
fn corrupt_snapshot_fails_closed_to_an_empty_store() {
    let directory = tempdir().unwrap();
    fs::write(
        directory.path().join("personal-lexicon.snapshot"),
        b"corrupt",
    )
    .unwrap();
    fs::write(
        directory.path().join("personal-lexicon.journal"),
        b"ignored",
    )
    .unwrap();
    let engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert!(engine.suggest("anything").is_empty());
}

#[test]
fn newer_schema_is_rejected_without_overwriting_it() {
    let directory = tempdir().unwrap();
    let snapshot = directory.path().join("personal-lexicon.snapshot");
    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    learned(&mut engine, "schema", 2);
    engine.compact().unwrap();
    drop(engine);
    let mut bytes = fs::read(&snapshot).unwrap();
    // One past whatever this build writes, read out of the file rather than
    // written in here, so the test keeps meaning the same thing after a bump.
    let version = u16::from_le_bytes(bytes[8..10].try_into().unwrap());
    bytes[8..10].copy_from_slice(&(version + 1).to_le_bytes());
    let content_len = bytes.len() - 4;
    let sum = crate::persistence::checksum(&bytes[..content_len]);
    bytes[content_len..].copy_from_slice(&sum.to_le_bytes());
    fs::write(&snapshot, &bytes).unwrap();
    let error = SuggestionEngine::open(directory.path(), SuggestionConfig::default())
        .err()
        .expect("newer schema must be unavailable");
    assert_eq!(error.kind(), std::io::ErrorKind::Unsupported);
    assert_eq!(fs::read(snapshot).unwrap(), bytes);
}

#[test]
fn a_discarded_snapshot_takes_its_journal_with_it() {
    let directory = tempdir().unwrap();
    let journal = directory.path().join("personal-lexicon.journal");
    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    learned(&mut engine, "cũ", 2);
    engine.flush().unwrap();
    drop(engine);
    assert!(fs::metadata(&journal).unwrap().len() > 0);

    // The frames in that journal are perfectly good. It is the snapshot they were
    // written against that is gone.
    fs::write(
        directory.path().join("personal-lexicon.snapshot"),
        b"corrupt",
    )
    .unwrap();

    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(
        fs::metadata(&journal).unwrap().len(),
        0,
        "a journal we refuse to read must not be left for the next open to append to"
    );

    learned(&mut engine, "mới", 2);
    engine.flush().unwrap();
    drop(engine);
    let reopened = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(texts(&reopened, "m"), ["mới"]);
}

#[test]
fn an_oversized_journal_is_discarded_without_losing_persistence() {
    let directory = tempdir().unwrap();
    let journal = directory.path().join("personal-lexicon.journal");
    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    learned(&mut engine, "cũ", 2);
    engine.compact().unwrap();
    drop(engine);
    fs::write(&journal, vec![0u8; 2 * 1024 * 1024]).unwrap();

    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(fs::metadata(&journal).unwrap().len(), 0);
    assert_eq!(
        texts(&engine, "c"),
        ["cũ"],
        "the snapshot was never in doubt"
    );

    learned(&mut engine, "mới", 2);
    engine.flush().unwrap();
    drop(engine);
    let reopened = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(texts(&reopened, "m"), ["mới"]);
}

/// A valid v1 snapshot: the word list alone, with no follower section.
fn version_one_snapshot(words: &[(&str, u32, u64)], sequence: u64) -> Vec<u8> {
    let mut bytes = Vec::new();
    bytes.extend_from_slice(b"FPSNAP01");
    bytes.extend_from_slice(&1u16.to_le_bytes());
    bytes.extend_from_slice(&sequence.to_le_bytes());
    bytes.extend_from_slice(&(words.len() as u32).to_le_bytes());
    for (text, uses, last_used) in words {
        bytes.extend_from_slice(&(text.len() as u16).to_le_bytes());
        bytes.extend_from_slice(text.as_bytes());
        bytes.extend_from_slice(&uses.to_le_bytes());
        bytes.extend_from_slice(&last_used.to_le_bytes());
    }
    let sum = crate::persistence::checksum(&bytes);
    bytes.extend_from_slice(&sum.to_le_bytes());
    bytes
}

#[test]
fn a_version_one_snapshot_upgrades_without_losing_a_word() {
    let directory = tempdir().unwrap();
    let snapshot = directory.path().join("personal-lexicon.snapshot");
    let original = version_one_snapshot(&[("chào", 4, 1), ("chúc", 2, 2)], 2);
    fs::write(&snapshot, &original).unwrap();

    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(texts(&engine, "ch"), ["chào", "chúc"]);
    assert_eq!(engine.stats().words, 2);

    engine.compact().unwrap();
    drop(engine);
    assert_ne!(
        fs::read(&snapshot).unwrap(),
        original,
        "the first compact should have rewritten the file at the current schema"
    );

    let reopened = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(texts(&reopened, "ch"), ["chào", "chúc"]);
}
