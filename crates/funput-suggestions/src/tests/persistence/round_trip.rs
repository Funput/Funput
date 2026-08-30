use tempfile::tempdir;

use crate::tests::bigram::followers_of;
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

#[test]
fn follower_edges_survive_a_compact() {
    let directory = tempdir().unwrap();
    {
        let mut engine =
            SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
        engine.learn_after(None, "xin");
        engine.learn_after(Some("xin"), "chào");
        engine.learn_after(Some("xin"), "chào");
        engine.learn_after(Some("xin"), "lỗi");
        engine.compact().unwrap();
    }
    let reopened = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    assert_eq!(
        followers_of(&reopened, "xin"),
        [("chào".to_owned(), 2), ("lỗi".to_owned(), 1)]
    );
    assert_eq!(context_seen(&reopened, "xin"), 3);
}

#[test]
fn a_shrunk_capacity_does_not_leave_edges_pointing_at_the_wrong_word() {
    let directory = tempdir().unwrap();
    {
        let mut engine =
            SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
        // Laid out so the trim is forced to hand a live slot to another word:
        // "bbb" is the weakest and sits at index 1, "ddd" is last and survives,
        // so `swap_remove` moves "ddd" into the slot "aaa" has an edge to.
        learned(&mut engine, "aaa", 4);
        engine.learn_after(Some("aaa"), "bbb");
        learned(&mut engine, "ccc", 3);
        learned(&mut engine, "ddd", 4);
        engine.compact().unwrap();
    }
    let reopened = SuggestionEngine::open(
        directory.path(),
        SuggestionConfig {
            max_words: 2,
            ..SuggestionConfig::default()
        },
    )
    .unwrap();

    assert_eq!(
        texts(&reopened, "a"),
        ["aaa"],
        "the edge holder must survive"
    );
    assert!(
        followers_of(&reopened, "aaa").is_empty(),
        "the edge pointed at a word the trim deleted; it must not resolve to \
         whichever word took over that slot"
    );
}

fn context_seen(engine: &SuggestionEngine, word: &str) -> u16 {
    engine
        .words
        .iter()
        .find(|record| record.text == word)
        .map(|record| record.context_seen)
        .unwrap_or_default()
}
