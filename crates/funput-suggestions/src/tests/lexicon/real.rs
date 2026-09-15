//! The word list the keyboards will ship, checked whole: its size against the
//! ceiling, and every prefix it contains against an independently built answer.

use std::collections::HashMap;

use super::*;
use crate::lexicon::{Lexicon, verify};

const EN_TSV: &str = include_str!("../../../data/lexicon/en.tsv");

/// The design's ceiling for `en.lex`. Raising it is a decision for the design
/// document, not for this test.
const MAX_EN_LEX_BYTES: usize = 512 * 1024;

fn shipped() -> Vec<(String, u16)> {
    EN_TSV
        .lines()
        .filter(|line| !line.starts_with('#'))
        .map(|line| {
            let (word, rank) = line.split_once('\t').unwrap();
            (word.to_owned(), rank.parse().unwrap())
        })
        .collect()
}

#[test]
fn en_lex_stays_under_its_size_ceiling() {
    let bytes = encode(EN_TSV).unwrap();
    assert!(
        bytes.len() <= MAX_EN_LEX_BYTES,
        "en.lex is {} bytes, over the {MAX_EN_LEX_BYTES}-byte ceiling",
        bytes.len()
    );
}

#[test]
fn every_prefix_of_every_shipped_word_gets_its_true_top_three() {
    let mut words = shipped();
    words.sort_by_key(|(word, _)| word.to_ascii_lowercase());
    // Each prefix → every (rank, id) that carries it, built in one pass.
    let mut runs: HashMap<String, Vec<(u16, usize)>> = HashMap::new();
    for (id, (word, rank)) in words.iter().enumerate() {
        let key = word.to_ascii_lowercase();
        for len in 2..=key.len() {
            runs.entry(key[..len].to_owned())
                .or_default()
                .push((*rank, id));
        }
    }

    let bytes = encode(EN_TSV).unwrap();
    let heavy = runs.values().filter(|run| run.len() > 3).count();
    assert_eq!(verify(&bytes).unwrap().heavy_prefixes, heavy);

    let lexicon = Lexicon::from_bytes(bytes).unwrap();
    for (prefix, run) in &mut runs {
        run.sort_unstable();
        let expected: Vec<&str> = run
            .iter()
            .take(3)
            .map(|(_, id)| words[*id].0.as_str())
            .collect();
        for asked in [prefix.clone(), prefix.to_ascii_uppercase()] {
            let found: Vec<&str> = lexicon.top3(&asked).iter().collect();
            assert_eq!(found, expected, "prefix {asked:?}");
        }
    }
    for absent in ["qx", "zzzz", "thx", "xylophonez"] {
        assert!(
            !runs.contains_key(absent) && lexicon.top3(absent).is_empty(),
            "{absent}"
        );
    }
}
