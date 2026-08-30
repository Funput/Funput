use super::{edge, followers_of};
use crate::{LearnOutcome, SuggestionConfig, SuggestionEngine};

fn bounded(max_words: usize) -> SuggestionEngine {
    SuggestionEngine::in_memory(SuggestionConfig {
        max_words,
        ..SuggestionConfig::default()
    })
}

#[test]
fn a_pair_is_recorded_and_counted_up() {
    let mut engine = bounded(16);
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    assert_eq!(followers_of(&engine, "xin"), [edge("chào", 1)]);

    engine.learn_after(Some("xin"), "chào");
    assert_eq!(followers_of(&engine, "xin"), [edge("chào", 2)]);
}

#[test]
fn learning_without_a_context_records_no_edge() {
    let mut engine = bounded(16);
    engine.learn_after(None, "xin");
    engine.learn_after(None, "chào");
    assert!(followers_of(&engine, "xin").is_empty());
}

#[test]
fn an_ignored_token_records_no_edge() {
    let mut engine = bounded(16);
    engine.learn_after(None, "xin");
    assert_eq!(
        engine.learn_after(Some("xin"), "hai từ"),
        LearnOutcome::Ignored
    );
    assert_eq!(engine.learn_after(Some("xin"), ""), LearnOutcome::Ignored);
    assert!(followers_of(&engine, "xin").is_empty());
}

#[test]
fn an_unknown_context_records_no_edge() {
    let mut engine = bounded(16);
    engine.learn_after(Some("chưa gặp"), "chào");
    assert!(followers_of(&engine, "chào").is_empty());
}

#[test]
fn a_context_evicted_by_its_own_token_records_no_edge() {
    // One slot, so learning "chào" can only take the slot "xin" is sitting in.
    let mut engine = bounded(1);
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");

    assert_eq!(engine.stats().words, 1);
    assert!(
        followers_of(&engine, "chào").is_empty(),
        "the context was evicted by the very token that would follow it; the edge \
         would land on whatever moved into its slot"
    );
}

#[test]
fn an_edge_to_an_evicted_word_reads_as_dead() {
    let mut engine = bounded(3);
    engine.learn_after(None, "aaa");
    engine.learn_after(None, "aaa");
    engine.learn_after(Some("aaa"), "bbb");
    assert_eq!(followers_of(&engine, "aaa"), [edge("bbb", 1)]);

    // "bbb" is the lowest ranked, so filling the lexicon pushes it out.
    engine.learn_after(None, "ccc");
    engine.learn_after(None, "ddd");
    assert!(
        followers_of(&engine, "aaa").is_empty(),
        "an edge outlives its target, and must read as dead rather than point at \
         the word that took the slot"
    );
}

#[test]
fn a_word_may_follow_itself() {
    let mut engine = bounded(16);
    engine.learn_after(None, "ha");
    engine.learn_after(Some("ha"), "ha");
    assert_eq!(followers_of(&engine, "ha"), [edge("ha", 1)]);
}
