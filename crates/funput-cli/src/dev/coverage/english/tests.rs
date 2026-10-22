use super::baseline::{compare, format, parse};
use super::counts::evaluate;
use super::outcome::{Outcome, classify, words};
use super::*;

#[test]
fn sorts_what_a_word_turned_into() {
    assert_eq!(classify("password", "password"), Outcome::Typed);
    assert_eq!(classify("password", "pasword"), Outcome::LostKey);
    assert_eq!(classify("been", "bên"), Outcome::Vietnamese);
    assert_eq!(classify("of", "off"), Outcome::Other);
}

#[test]
fn types_only_the_lowercase_ascii_words() {
    let tsv = "# header\npassword\t9\nUSA\t8\ncafé\t7\nof\t6\n\n";
    assert_eq!(words(tsv), ["password", "of"]);
}

#[test]
fn counts_each_kind_of_wrong() {
    let counts = evaluate(&["password", "is", "card"], SETUPS[0], 5);
    assert_eq!(
        (counts.lost_key, counts.vietnamese, counts.other),
        (0, 1, 0)
    );
    let lazy = evaluate(&["class"], SETUPS[1], 5);
    assert_eq!(lazy.wrong(), 0, "eager restore off still restores `class`");
}

fn named(pairs: &[(&str, usize)]) -> Vec<(String, usize)> {
    pairs
        .iter()
        .map(|&(name, count)| (name.to_owned(), count))
        .collect()
}

#[test]
fn the_baseline_reads_back_what_it_writes() {
    let counts = named(&[("telex.eager.total", 3), ("telex.eager.wrong", 1)]);
    assert_eq!(parse(&format(&counts)), counts);
}

#[test]
fn any_moved_count_fails_and_says_which() {
    let baseline = named(&[("a.total", 10), ("a.wrong", 4), ("a.lost_key", 2)]);
    assert!(compare(&baseline, &baseline).is_empty());
    let worse = compare(
        &baseline,
        &named(&[("a.total", 10), ("a.wrong", 5), ("a.lost_key", 2)]),
    );
    assert!(worse[0].contains("a.wrong regressed: 4 -> 5"), "{worse:?}");
    let better = compare(
        &baseline,
        &named(&[("a.total", 10), ("a.wrong", 3), ("a.lost_key", 2)]),
    );
    assert!(better[0].contains("lower the baseline"), "{better:?}");
    let grown = compare(
        &baseline,
        &named(&[("a.total", 11), ("a.wrong", 4), ("a.lost_key", 2)]),
    );
    assert!(grown[0].contains("lexicon has 11 words"), "{grown:?}");
    let renamed = compare(&baseline, &named(&[("a.total", 10), ("a.wrong", 4)]));
    assert!(
        renamed[0].contains("a.lost_key is no longer measured"),
        "{renamed:?}"
    );
}

/// The gate itself: today's engine typing today's lexicon gives exactly the counts
/// committed in `benchmarks/results/english-coverage.txt`.
#[test]
fn the_engine_types_english_as_the_baseline_says() {
    let tsv = include_str!("../../../../../funput-suggestions/data/lexicon/en.tsv");
    let committed = include_str!("../../../../../../benchmarks/results/english-coverage.txt");
    let current: Vec<_> = evaluate_all(tsv, None, 0)
        .iter()
        .flat_map(Counts::entries)
        .collect();
    let problems = compare(&parse(committed), &current);
    assert!(problems.is_empty(), "{}", problems.join("\n"));
}
