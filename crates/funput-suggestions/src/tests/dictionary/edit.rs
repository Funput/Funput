//! What each edit does to the store, the bar and the veto.

use std::io::Write;

use super::{apply, blocked, engine, listed};
use crate::lexicon::encode::encode;
use crate::tests::{learned, texts};
use crate::{
    DictionaryFilter, DictionaryQuery, DictionarySort, EditKind, EditOutcome, LearnOutcome, Origin,
    SuggestionConfig, SuggestionEngine,
};

#[test]
fn an_added_word_is_offered_at_once_and_listed_as_added() {
    let mut engine = engine();
    assert_eq!(
        apply(&mut engine, EditKind::Add, "Funput"),
        EditOutcome::Applied
    );
    assert_eq!(texts(&engine, "fun"), ["funput"]);
    let entries = engine.dictionary(&DictionaryQuery {
        filter: DictionaryFilter::Added,
        ..DictionaryQuery::default()
    });
    assert_eq!(entries.len(), 1);
    assert_eq!(entries[0].origin, Origin::Added);
    assert_eq!(
        apply(&mut engine, EditKind::Add, "funput"),
        EditOutcome::Unchanged
    );
}

#[test]
fn a_word_still_below_the_threshold_is_not_listed() {
    let mut engine = engine();
    learned(&mut engine, "đánb", 1);
    learned(&mut engine, "được", 2);
    assert_eq!(listed(&engine), ["được"]);
}

#[test]
fn forgetting_drops_the_word_until_it_is_typed_again() {
    let mut engine = engine();
    learned(&mut engine, "deadline", 4);
    assert_eq!(
        apply(&mut engine, EditKind::Forget, "Deadline"),
        EditOutcome::Applied
    );
    assert!(texts(&engine, "dead").is_empty());
    assert_eq!(
        apply(&mut engine, EditKind::Forget, "deadline"),
        EditOutcome::Unchanged
    );
    learned(&mut engine, "deadline", 4);
    assert_eq!(texts(&engine, "dead"), ["deadline"]);
}

#[test]
fn a_blocked_word_is_never_learned_and_never_offered_from_the_lexicon() {
    let mut engine = engine();
    let mut file = tempfile::NamedTempFile::new().unwrap();
    file.write_all(&encode("deadline\t0\ndeal\t1\n").unwrap())
        .unwrap();
    engine.attach_lexicon(file.path()).unwrap();
    learned(&mut engine, "deadline", 4);

    assert_eq!(
        apply(&mut engine, EditKind::Block, "deadline"),
        EditOutcome::Applied
    );
    assert_eq!(engine.learn("deadline"), LearnOutcome::Ignored);
    assert_eq!(texts(&engine, "dea"), ["deal"]);
    assert_eq!(blocked(&engine), ["deadline"]);
    assert!(
        engine.is_known_word("deadline"),
        "a blocked word is still a word"
    );

    assert_eq!(
        apply(&mut engine, EditKind::Unblock, "deadline"),
        EditOutcome::Applied
    );
    assert_eq!(texts(&engine, "dea"), ["deadline", "deal"]);
}

#[test]
fn adding_a_blocked_word_lifts_the_block() {
    let mut engine = engine();
    apply(&mut engine, EditKind::Block, "zalo");
    assert_eq!(
        apply(&mut engine, EditKind::Add, "zalo"),
        EditOutcome::Applied
    );
    assert!(blocked(&engine).is_empty());
    assert_eq!(texts(&engine, "za"), ["zalo"]);
}

#[test]
fn malformed_words_are_refused_without_a_change() {
    let mut engine = engine();
    for word in ["", " ", "hai từ", &"a".repeat(33)] {
        assert_eq!(
            apply(&mut engine, EditKind::Add, word),
            EditOutcome::Invalid
        );
    }
    assert_eq!(engine.stats().words, 0);
}

#[test]
fn the_limits_refuse_before_anything_changes() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig {
        max_added_words: 1,
        max_blocked_words: 1,
        ..SuggestionConfig::default()
    });
    apply(&mut engine, EditKind::Add, "một");
    apply(&mut engine, EditKind::Block, "hai");
    learned(&mut engine, "ba", 2);
    assert_eq!(
        apply(&mut engine, EditKind::Add, "bốn"),
        EditOutcome::LimitReached
    );
    assert_eq!(
        apply(&mut engine, EditKind::Add, "hai"),
        EditOutcome::LimitReached
    );
    assert_eq!(blocked(&engine), ["hai"], "a refused add keeps the block");
    assert_eq!(
        apply(&mut engine, EditKind::Block, "ba"),
        EditOutcome::LimitReached
    );
    assert_eq!(
        texts(&engine, "b"),
        ["ba"],
        "a refused block forgets nothing"
    );
}

#[test]
fn an_added_word_is_never_evicted_to_make_room() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig {
        max_words: 2,
        ..SuggestionConfig::default()
    });
    apply(&mut engine, EditKind::Add, "funput");
    for word in ["anh", "em", "chị", "bạn"] {
        learned(&mut engine, word, 2);
    }
    assert_eq!(engine.stats().words, 2);
    assert_eq!(texts(&engine, "fu"), ["funput"]);
}

#[test]
fn a_full_store_of_added_words_refuses_rather_than_evicting_one() {
    let directory = tempfile::tempdir().unwrap();
    {
        let mut engine =
            SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
        for word in ["một", "hai", "ba"] {
            apply(&mut engine, EditKind::Add, word);
        }
        engine.flush().unwrap();
    }
    let shrunk = SuggestionConfig {
        max_words: 2,
        ..SuggestionConfig::default()
    };
    let mut engine = SuggestionEngine::open(directory.path(), shrunk).unwrap();
    assert_eq!(engine.learn("bốn"), LearnOutcome::Ignored);
    assert_eq!(
        apply(&mut engine, EditKind::Add, "năm"),
        EditOutcome::LimitReached
    );
    let mut words = listed(&engine);
    words.sort();
    assert_eq!(words, ["ba", "hai", "một"], "no added word may be evicted");
}

#[test]
fn a_blocked_word_is_never_predicted_from_its_context() {
    let mut engine = engine();
    for _ in 0..4 {
        engine.learn_after(None, "xin");
        engine.learn_after(Some("xin"), "chào");
    }
    assert_eq!(texts_after(&engine, "xin"), ["chào"]);
    apply(&mut engine, EditKind::Block, "chào");
    assert!(texts_after(&engine, "xin").is_empty());
}

#[test]
fn listings_search_without_marks_and_sort_as_asked() {
    let mut engine = engine();
    learned(&mut engine, "được", 5);
    learned(&mut engine, "đi", 2);
    learned(&mut engine, "ăn", 3);
    let query = |search, sort| DictionaryQuery {
        search,
        sort,
        ..DictionaryQuery::default()
    };
    let words = |query: DictionaryQuery<'_>| -> Vec<String> {
        engine
            .dictionary(&query)
            .iter()
            .map(|entry| entry.word.to_owned())
            .collect()
    };
    assert_eq!(words(query("duoc", DictionarySort::MostUsed)), ["được"]);
    assert_eq!(
        words(query("", DictionarySort::MostUsed)),
        ["được", "ăn", "đi"]
    );
    assert_eq!(
        words(query("", DictionarySort::Recent)),
        ["ăn", "đi", "được"]
    );
    assert_eq!(
        words(query("", DictionarySort::Alphabetical)),
        ["ăn", "đi", "được"]
    );
}

#[test]
fn reset_forgets_every_word_but_keeps_the_blocks() {
    let mut engine = engine();
    apply(&mut engine, EditKind::Add, "funput");
    apply(&mut engine, EditKind::Block, "đm");
    engine.reset().unwrap();
    assert!(listed(&engine).is_empty());
    assert_eq!(blocked(&engine), ["đm"]);
}

fn texts_after(engine: &SuggestionEngine, previous: &str) -> Vec<String> {
    engine
        .suggest_with(Some(previous), "")
        .iter()
        .map(str::to_owned)
        .collect()
}
