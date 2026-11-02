//! One line per threshold combination, for `--sweep`.

use std::path::Path;

use funput_suggestions::SuggestionConfig;

use super::print::{method, tone};
use super::{Source, Tally, pct};
use crate::dev::suggest::Options;
use crate::dev::suggest::sweep::TARGET_RIGHT_PCT;

pub(in crate::dev::suggest) fn print_sweep_header(text: &Path, options: &Options) {
    println!(
        "Funput Predict thresholds — {}, shipped data {}, learning {} (text: {})",
        method(options.method),
        if options.seed {
            tone(options.tone)
        } else {
            "off"
        },
        if options.learn { "on" } else { "off" },
        text.display()
    );
    println!(
        "`*` = personal Predict right in at least {TARGET_RIGHT_PCT:.0}% of the times it spoke"
    );
    println!(
        "uses  dom%  evid   shown  personal right/shown       shipped right/shown  keys saved"
    );
}

pub(in crate::dev::suggest) fn print_sweep_row(config: &SuggestionConfig, tally: &Tally) {
    let (right, shown) = source(tally, Source::Personal);
    let (shipped_right, shipped_shown) = source(tally, Source::Shipped);
    let share = pct(right, shown);
    let mark = if shown > 0 && share >= TARGET_RIGHT_PCT {
        '*'
    } else {
        ' '
    };
    let default = if *config == SuggestionConfig::default() {
        "  (default)"
    } else {
        ""
    };
    println!(
        "{:>4}  {:>4}  {:>4}  {:>6}  {:>5}/{:<5} {:>5.1}% {mark}       {:>5}/{:<5}     {:>6.1}%{default}",
        config.context_predict_uses,
        config.context_dominance_percent,
        config.context_evidence_seen,
        tally.predict_shown(),
        right,
        shown,
        share,
        shipped_right,
        shipped_shown,
        pct(tally.saved, tally.keys),
    );
}

/// (right, shown) for the Predicts that came from `source`.
fn source(tally: &Tally, source: Source) -> (usize, usize) {
    let i = source as usize;
    (
        tally.predict_right[i],
        tally.predict_right[i] + tally.predict_wrong[i],
    )
}
