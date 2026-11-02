use std::path::PathBuf;

use clap::Parser;
use funput_core::{InputMethod, ToneStyle};
use funput_suggestions::SuggestionConfig;

use super::complete::keys;
use super::report::{Source, Tally};
use super::sweep::grid;
use super::{Options, SuggestArgs, measure, text};

fn options(seed: bool, learn: bool) -> Options {
    Options {
        config: SuggestionConfig::default(),
        method: InputMethod::Telex,
        tone: ToneStyle::Modern,
        seed,
        learn,
        lexicon: None,
        show: 10,
        json: false,
    }
}

fn replay(text: &str, seed: bool, learn: bool) -> Tally {
    measure(&text::runs(text), &options(seed, learn)).expect("no lexicon to open")
}

fn shipped_text() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../benchmarks/chat-vi.txt")
}

#[test]
fn runs_break_where_the_trackers_drop_context() {
    let runs = text::runs("Cảm ơn,  Bạn  nhé\n# a comment\nđi3về.\n\n");
    assert_eq!(
        runs,
        [
            vec!["cảm", "ơn"],
            vec!["bạn", "nhé"],
            vec!["đi"],
            vec!["về"]
        ]
    );
}

#[test]
fn a_hashtag_opens_a_message_not_a_comment() {
    let runs = text::runs("# a comment\n#\n#throwback hôm qua\n");
    assert_eq!(runs, [vec!["throwback", "hôm", "qua"]]);
}

#[test]
fn json_strings_are_escaped_the_json_way() {
    use crate::dev::render::json_escape;
    // Debug formatting would print `\u{300}` and `\u{1b}`, which JSON rejects.
    assert_eq!(json_escape("\u{300}a"), "\u{300}a");
    assert_eq!(json_escape("a\u{1b}\"\\"), "a\\u001b\\\"\\\\");
}

#[test]
fn a_shipped_pair_is_a_right_predict_from_the_shipped_data() {
    let tally = replay("Cảm Ơn", true, false);
    assert_eq!(tally.predict_chances, 1);
    assert_eq!(tally.predict_right, [1, 0]);
    assert_eq!(tally.predict_shown(), 1);
    // `cảm` itself may complete from the shipped syllables; `ơn` saves all its keys.
    let first_word = replay("cảm", true, false).saved;
    assert_eq!(tally.saved, first_word + keys("ơn", InputMethod::Telex));
}

#[test]
fn without_shipped_data_predict_is_silent_on_a_new_install() {
    let tally = replay("cảm ơn", false, false);
    assert_eq!(tally.predict_chances, 1);
    assert_eq!(tally.predict_shown(), 0);
    assert_eq!(
        tally.top_silent(1),
        [(&("cảm".to_owned(), "ơn".to_owned()), 1)]
    );
}

#[test]
fn a_learned_pair_is_credited_to_personal_data() {
    let text = "mua rau\n".repeat(5);
    let tally = replay(&text, true, true);
    assert!(tally.by_source(Source::Personal).0 > 0, "{tally:?}");
    assert_eq!(tally.by_source(Source::Shipped).0, 0);
    let tally = replay(&text, true, false);
    assert_eq!(tally.predict_shown(), 0, "nothing learned, nothing shipped");
}

#[test]
fn a_one_letter_word_drops_the_context() {
    // `x` is asked after `cảm`, but `ơn` comes with no context: one chance only.
    let tally = replay("cảm x ơn", true, false);
    assert_eq!(tally.predict_chances, 1);
    assert_eq!(tally.predict_wrong, [1, 0]);
}

#[test]
fn a_completion_saves_the_keys_left_after_its_prefix() {
    let tally = replay("được", true, false);
    assert_eq!(tally.completion_words, 1);
    assert_eq!(tally.completion_found, 1, "{tally:?}");
    let prefix: String = "được"
        .chars()
        .take(tally.completion_prefix_letters)
        .collect();
    let method = InputMethod::Telex;
    assert_eq!(tally.saved, keys("được", method) - keys(&prefix, method));
}

#[test]
fn the_shipped_text_replays_the_same_way_every_time() {
    let runs = text::load(&shipped_text()).expect("benchmarks/chat-vi.txt");
    let first = measure(&runs, &options(true, true)).expect("no lexicon");
    let second = measure(&runs, &options(true, true)).expect("no lexicon");
    assert!(first.predict_chances > 0 && first.completion_words > 0);
    assert_eq!(
        (
            first.saved,
            first.predict_right,
            first.predict_wrong,
            first.bar
        ),
        (
            second.saved,
            second.predict_right,
            second.predict_wrong,
            second.bar
        )
    );
    assert_eq!(first.top_wrong(50), second.top_wrong(50));
}

/// Parse threshold flags the way `funput dev suggest` would.
fn thresholds(flags: &[&str]) -> SuggestionConfig {
    #[derive(Parser)]
    struct Cli {
        #[command(flatten)]
        args: SuggestArgs,
    }
    let args = std::iter::once("suggest").chain(flags.iter().copied());
    Options::from(&Cli::parse_from(args).args).config
}

fn personal(text: &str, config: SuggestionConfig) -> (usize, usize) {
    let options = Options {
        config,
        ..options(false, true)
    };
    let tally = measure(&text::runs(text), &options).expect("no lexicon");
    tally.by_source(Source::Personal)
}

#[test]
fn no_flags_keep_the_keyboards_config() {
    assert_eq!(thresholds(&[]), SuggestionConfig::default());
    let config = thresholds(&[
        "--predict-uses",
        "3",
        "--dominance",
        "60",
        "--evidence",
        "5",
    ]);
    assert_eq!(
        (
            config.context_predict_uses,
            config.context_dominance_percent,
            config.context_evidence_seen
        ),
        (3, 60, 5)
    );
}

#[test]
fn full_dominance_silences_a_context_with_two_followers() {
    let text = format!("mua rau\nmua cá\n{}", "mua rau\n".repeat(4));
    assert!(personal(&text, SuggestionConfig::default()).1 > 0);
    assert_eq!(personal(&text, thresholds(&["--dominance", "100"])), (0, 0));
}

#[test]
fn more_predict_uses_wait_for_more_sightings() {
    let three = "mua rau\n".repeat(3);
    let four = "mua rau\n".repeat(4);
    assert_eq!(
        personal(&three, thresholds(&["--predict-uses", "2"])),
        (1, 1)
    );
    assert_eq!(
        personal(&three, thresholds(&["--predict-uses", "3"])),
        (0, 0)
    );
    assert_eq!(
        personal(&four, thresholds(&["--predict-uses", "3"])),
        (1, 1)
    );
}

#[test]
fn the_sweep_covers_the_defaults_once() {
    let configs: Vec<_> = grid().collect();
    assert_eq!(configs.len(), 45);
    let defaults = configs
        .iter()
        .filter(|&&config| config == SuggestionConfig::default())
        .count();
    assert_eq!(
        defaults, 1,
        "the sweep must show today's defaults to compare"
    );
}
