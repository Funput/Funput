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
    bytes[8..10].copy_from_slice(&2u16.to_le_bytes());
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
