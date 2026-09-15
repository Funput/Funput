//! The lookup against a brute-force oracle, on word lists built to be dense in
//! shared prefixes so that heavy and light answers both come up constantly.

use proptest::prelude::*;

use super::*;
use crate::lexicon::Lexicon;

fn texts(lexicon: &Lexicon, prefix: &str) -> Vec<String> {
    lexicon.top3(prefix).iter().map(str::to_owned).collect()
}

/// Three letters in both cases, so a list of a few hundred words is all runs.
fn word_list() -> impl Strategy<Value = Vec<(String, u16)>> {
    prop::collection::vec("[a-cA-C]{2,6}", 1..200)
        .prop_flat_map(|words| {
            let mut seen = std::collections::HashSet::new();
            let words: Vec<String> = words
                .into_iter()
                .filter(|word| seen.insert(word.to_ascii_lowercase()))
                .collect();
            let ranks: Vec<u16> = (0..words.len() as u16).collect();
            (Just(words), Just(ranks).prop_shuffle())
        })
        .prop_map(|(words, ranks)| words.into_iter().zip(ranks).collect())
}

fn encoded(words: &[(String, u16)]) -> Lexicon {
    let list: String = words
        .iter()
        .map(|(word, rank)| format!("{word}\t{rank}\n"))
        .collect();
    Lexicon::from_bytes(encode(&list).unwrap()).unwrap()
}

#[test]
fn an_empty_lexicon_and_an_unfit_prefix_answer_nothing() {
    let empty = Lexicon::from_bytes(encode("").unwrap()).unwrap();
    assert!(empty.top3("th").is_empty());
    let lexicon = Lexicon::from_bytes(fixture_bytes()).unwrap();
    for prefix in ["", "t", "th3", "thé", "t h", &"t".repeat(33)] {
        assert!(lexicon.top3(prefix).is_empty(), "{prefix:?}");
    }
}

#[test]
fn the_fixture_answers_light_heavy_and_cased_prefixes() {
    let lexicon = Lexicon::from_bytes(fixture_bytes()).unwrap();
    assert_eq!(texts(&lexicon, "th"), ["the", "that", "they"]);
    assert_eq!(texts(&lexicon, "THE"), ["the", "they", "there"]);
    assert_eq!(texts(&lexicon, "wh"), ["what", "when", "which"]);
    assert_eq!(texts(&lexicon, "ip"), ["iPhone"]);
    assert_eq!(texts(&lexicon, "covid"), ["COVID"]);
    assert!(texts(&lexicon, "zz").is_empty());
}

proptest! {
    #![proptest_config(ProptestConfig::with_cases(64))]

    #[test]
    fn every_prefix_matches_the_oracle(words in word_list(), extra in prop::collection::vec("[a-dA-D0-9]{0,7}", 0..32)) {
        let lexicon = encoded(&words);
        let prefixes = words
            .iter()
            .flat_map(|(word, _)| (0..=word.len()).map(move |len| word[..len].to_owned()))
            .chain(extra);
        for prefix in prefixes {
            prop_assert_eq!(texts(&lexicon, &prefix), oracle(&words, &prefix), "prefix {:?}", prefix);
        }
    }
}
