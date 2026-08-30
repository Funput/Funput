use crate::tests::texts;
use crate::{SuggestionConfig, SuggestionEngine};

fn engine(config: SuggestionConfig) -> SuggestionEngine {
    SuggestionEngine::in_memory(config)
}

fn with(engine: &SuggestionEngine, previous: &str, prefix: &str) -> Vec<String> {
    engine
        .suggest_with(Some(previous), prefix)
        .iter()
        .map(str::to_owned)
        .collect()
}

#[test]
fn a_follower_of_the_context_takes_the_first_slot() {
    let mut engine = engine(SuggestionConfig::default());
    // "chúc" is used more often, so frequency alone puts it first.
    for _ in 0..5 {
        engine.learn_after(None, "chúc");
    }
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    engine.learn_after(Some("xin"), "chào");

    assert_eq!(texts(&engine, "ch"), ["chúc", "chào"]);
    assert_eq!(with(&engine, "xin", "ch"), ["chào", "chúc"]);
}

#[test]
fn a_follower_outside_the_prefix_top_three_is_still_offered() {
    let mut engine = engine(SuggestionConfig::default());
    for word in ["chăm", "chân", "chậm", "chợ"] {
        for _ in 0..5 {
            engine.learn_after(None, word);
        }
    }
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");

    let plain = texts(&engine, "ch");
    assert_eq!(plain.len(), 3);
    assert!(
        !plain.contains(&"chào".to_owned()),
        "the trie keeps only three per node, and \"chào\" is not one of them: {plain:?}"
    );
    assert_eq!(
        with(&engine, "xin", "ch")[0],
        "chào",
        "a sharp pair whose target is globally rare is exactly what the context is for"
    );
}

#[test]
fn a_follower_that_does_not_match_the_prefix_is_not_offered() {
    let mut engine = engine(SuggestionConfig::default());
    for _ in 0..2 {
        engine.learn_after(None, "cảm");
    }
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    engine.learn_after(Some("xin"), "chào");

    assert_eq!(
        with(&engine, "xin", "cả"),
        texts(&engine, "cả"),
        "the context may reorder completions, never replace them"
    );
}

#[test]
fn an_unknown_or_absent_context_changes_nothing() {
    let mut engine = engine(SuggestionConfig::default());
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    engine.learn_after(Some("xin"), "chào");

    let plain = texts(&engine, "ch");
    assert_eq!(with(&engine, "chưa", "ch"), plain);
    assert_eq!(
        engine
            .suggest_with(None, "ch")
            .iter()
            .map(str::to_owned)
            .collect::<Vec<_>>(),
        plain
    );
}

#[test]
fn an_edge_to_an_evicted_word_does_not_promote_its_replacement() {
    let mut engine = engine(SuggestionConfig {
        max_words: 4,
        ..SuggestionConfig::default()
    });
    for _ in 0..5 {
        engine.learn_after(None, "xin");
    }
    engine.learn_after(Some("xin"), "chào");
    engine.learn_after(Some("xin"), "chào");
    for _ in 0..4 {
        engine.learn_after(None, "chúc");
    }
    for _ in 0..4 {
        engine.learn_after(None, "aaa");
    }
    assert_eq!(with(&engine, "xin", "ch")[0], "chào");

    // "chào" is now the weakest, so "chăm" takes the very slot the edge points
    // at — and "chăm" is itself a completion of "ch".
    engine.learn_after(None, "chăm");
    engine.learn_after(None, "chăm");

    assert_eq!(
        with(&engine, "xin", "ch"),
        texts(&engine, "ch"),
        "the edge died with the word it pointed at; it must not promote whichever \
         word inherited the slot"
    );
    assert_eq!(with(&engine, "xin", "ch")[0], "chúc");
}

#[test]
fn a_follower_below_the_threshold_stays_where_it_was() {
    let mut engine = engine(SuggestionConfig {
        context_rerank_uses: 2,
        ..SuggestionConfig::default()
    });
    for _ in 0..5 {
        engine.learn_after(None, "chúc");
    }
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    engine.learn_after(None, "chào");

    assert_eq!(with(&engine, "xin", "ch"), ["chúc", "chào"]);
    engine.learn_after(Some("xin"), "chào");
    assert_eq!(with(&engine, "xin", "ch"), ["chào", "chúc"]);
}

#[test]
fn a_folded_prefix_still_reranks() {
    let mut engine = engine(SuggestionConfig::default());
    for _ in 0..5 {
        engine.learn_after(None, "hôm");
    }
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "hòa");
    engine.learn_after(Some("xin"), "hòa");

    assert_eq!(texts(&engine, "ho"), ["hôm", "hòa"]);
    assert_eq!(with(&engine, "xin", "ho"), ["hòa", "hôm"]);
}

fn predicted(engine: &SuggestionEngine, previous: &str) -> Vec<String> {
    with(engine, previous, "")
}

#[test]
fn a_dominant_pair_speaks_after_a_space() {
    let mut engine = engine(SuggestionConfig::default());
    engine.learn_after(None, "cảm");
    engine.learn_after(Some("cảm"), "ơn");
    engine.learn_after(Some("cảm"), "ơn");
    assert_eq!(predicted(&engine, "cảm"), ["ơn"]);
}

#[test]
fn a_flat_context_stays_silent() {
    let mut engine = engine(SuggestionConfig::default());
    engine.learn_after(None, "của");
    // Four followers sharing the sightings evenly. Every one of them clears the
    // use threshold; none of them accounts for enough of "của" to be worth
    // saying, which is the whole distinction this mode rests on.
    for word in ["tôi", "bạn", "anh", "chị"] {
        for _ in 0..3 {
            engine.learn_after(Some("của"), word);
        }
    }
    assert!(
        predicted(&engine, "của").is_empty(),
        "a context that can be followed by anything must not offer one of them"
    );
}

#[test]
fn only_the_leader_is_offered() {
    let mut engine = engine(SuggestionConfig::default());
    engine.learn_after(None, "xin");
    for _ in 0..8 {
        engine.learn_after(Some("xin"), "chào");
    }
    for _ in 0..2 {
        engine.learn_after(Some("xin"), "lỗi");
    }
    assert_eq!(predicted(&engine, "xin"), ["chào"]);
}

#[test]
fn a_pair_below_the_use_threshold_stays_silent() {
    let mut engine = engine(SuggestionConfig::default());
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    assert!(
        predicted(&engine, "xin").is_empty(),
        "seen once is not a habit"
    );

    engine.learn_after(Some("xin"), "chào");
    assert_eq!(predicted(&engine, "xin"), ["chào"]);
}

#[test]
fn without_a_context_nothing_is_predicted() {
    let mut engine = engine(SuggestionConfig::default());
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    engine.learn_after(Some("xin"), "chào");

    assert!(engine.suggest_with(None, "").is_empty());
    assert!(predicted(&engine, "chưa").is_empty());
}

#[test]
fn a_dead_leader_does_not_speak() {
    let mut engine = engine(SuggestionConfig {
        max_words: 3,
        ..SuggestionConfig::default()
    });
    engine.learn_after(None, "xin");
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    engine.learn_after(Some("xin"), "chào");
    assert_eq!(predicted(&engine, "xin"), ["chào"]);

    // "chào" is the weakest, so filling the lexicon pushes it out.
    engine.learn_after(None, "aaa");
    engine.learn_after(None, "aaa");
    engine.learn_after(None, "bbb");
    engine.learn_after(None, "bbb");
    assert!(predicted(&engine, "xin").is_empty());
}
