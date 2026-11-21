//! The human rendering of a [`Tally`].

use std::path::Path;

use funput_core::{InputMethod, ToneStyle};

use super::{Predicts, Source, Tally, pct};
use crate::dev::suggest::Options;

pub(in crate::dev::suggest) fn print_human(text: &Path, tally: &Tally, options: &Options) {
    println!(
        "Funput suggestions — {} words, {}, shipped data {}, learning {}, en.lex {} (text: {})",
        tally.words,
        method(options.method),
        if options.seed {
            tone(options.tone)
        } else {
            "off"
        },
        on_off(options.learn),
        on_off(options.lexicon.is_some()),
        text.display()
    );
    println!(
        "Thresholds: predict uses {}, dominance {}%, evidence {}",
        options.config.context_predict_uses,
        options.config.context_dominance_percent,
        options.config.context_evidence_seen
    );
    predicts(&tally.predict);
    println!("Completion (while typing a word of 3+ letters)");
    println!("  words         {:>7}", tally.completion_words);
    println!(
        "  found         {:>7}  ({:.1}% of words, after {:.1} letters on average)",
        tally.completion_found,
        pct(tally.completion_found, tally.completion_words),
        average(tally.completion_prefix_letters, tally.completion_found)
    );
    let queries: usize = tally.bar.iter().sum();
    let fill: Vec<String> = (0..4)
        .map(|slots| format!("{slots} slots {:.0}%", pct(tally.bar[slots], queries)))
        .collect();
    println!("  bar filled    {}  ({queries} queries)", fill.join("  "));
    println!(
        "Keystrokes saved {:>5} of {} ({:.1}%)",
        tally.saved,
        tally.keys,
        pct(tally.saved, tally.keys)
    );
    samples(&tally.predict, options.show);
}

fn predicts(predict: &Predicts) {
    let shown = predict.shown();
    println!("Predict (after a space)");
    println!("  chances       {:>7}", predict.chances);
    println!(
        "  shown         {:>7}  ({:.1}% of chances, {:.1} words a bar)",
        shown,
        pct(shown, predict.chances),
        average(predict.slots, shown)
    );
    println!(
        "    right       {:>7}  ({:.1}% of shown)  {}",
        predict.right_total(),
        pct(predict.right_total(), shown),
        by_source(predict)
    );
    println!(
        "      first     {:>7}  ({:.1}% of shown)",
        predict.right_first,
        pct(predict.right_first, shown)
    );
    println!(
        "    wrong       {:>7}  ({:.1}% of shown)",
        predict.wrong_total(),
        pct(predict.wrong_total(), shown)
    );
}

fn samples(predict: &Predicts, show: usize) {
    let wrong = predict.top_wrong(show);
    if !wrong.is_empty() {
        println!("\nWrong Predicts: previous → bar (typed instead), times");
        for ((previous, guess, word, source), n) in wrong {
            println!("  {previous} → {guess} ({word}), {n}×  [{}]", source.name());
        }
    }
    let silent = predict.top_silent(show);
    if !silent.is_empty() {
        println!("\nSilent Predicts: word pairs the bar had nothing for, times");
        for ((previous, word), n) in silent {
            println!("  {previous} {word}, {n}×");
        }
    }
}

fn by_source(predict: &Predicts) -> String {
    Source::ALL
        .iter()
        .map(|&source| {
            let (right, shown) = predict.by_source(source);
            format!("{} {right}/{shown}", source.name())
        })
        .collect::<Vec<_>>()
        .join("  ")
}

fn average(sum: usize, count: usize) -> f64 {
    if count == 0 {
        0.0
    } else {
        sum as f64 / count as f64
    }
}

pub(super) fn method(method: InputMethod) -> &'static str {
    if method.is_telex_family() {
        "Telex"
    } else {
        "VNI"
    }
}

pub(super) fn tone(tone: ToneStyle) -> &'static str {
    match tone {
        ToneStyle::Modern => "modern",
        ToneStyle::Traditional => "traditional",
    }
}

fn on_off(on: bool) -> &'static str {
    if on { "on" } else { "off" }
}
