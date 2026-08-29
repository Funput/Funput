use tempfile::tempdir;

use crate::tests::{learned, texts};
use crate::{SuggestionConfig, SuggestionEngine};

#[test]
fn snapshot_and_journal_round_trip() {
    let directory = tempdir().unwrap();
    {
        let mut engine =
            SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
        learned(&mut engine, "chào", 2);
        engine.compact().unwrap();
        learned(&mut engine, "chào", 1);
        learned(&mut engine, "chúc", 2);
        engine.flush().unwrap();
    }
    let reopened = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(texts(&reopened, "ch"), ["chào", "chúc"]);
    assert_eq!(reopened.stats().words, 2);
}

#[test]
fn abandoned_temporary_snapshot_keeps_last_good_state() {
    let directory = tempdir().unwrap();
    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    learned(&mut engine, "antoàn", 2);
    engine.compact().unwrap();
    std::fs::write(
        directory.path().join("personal-lexicon.snapshot.tmp"),
        b"partial",
    )
    .unwrap();
    drop(engine);
    let reopened = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(texts(&reopened, "an"), ["antoàn"]);
}
