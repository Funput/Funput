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
    // Four: `antoàn` is two syllables run together, so it waits like any other token
    // Vietnamese cannot spell. The subject here is the abandoned snapshot, not that.
    learned(&mut engine, "antoàn", 4);
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

#[test]
fn an_edge_to_an_evicted_word_does_not_come_back_after_a_compact() {
    let directory = tempdir().unwrap();
    let config = SuggestionConfig {
        max_words: 3,
        ..SuggestionConfig::default()
    };
    {
        let mut engine = SuggestionEngine::open(directory.path(), config).unwrap();
        learned(&mut engine, "xin", 4);
        engine.learn_after(Some("xin"), "chào");
        learned(&mut engine, "cảm", 3);
        // The store is full, so `ơn` takes the weakest slot, which is `chào`'s.
        // The `xin` edge still names that slot at the old generation: dead.
        engine.learn("ơn");
        assert!(followers_of(&engine, "xin").is_empty());
        engine.compact().unwrap();
    }
    let reopened = SuggestionEngine::open(directory.path(), config).unwrap();
    assert!(
        reopened.words.iter().any(|record| record.text == "ơn"),
        "the evicting word must survive"
    );
    assert!(
        followers_of(&reopened, "xin").is_empty(),
        "the edge pointed at the evicted `chào`; reloading must not hand it to `ơn`"
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
