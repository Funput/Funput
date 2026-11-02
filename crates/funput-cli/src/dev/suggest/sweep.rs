//! Predict's thresholds: override them for one run, or sweep a grid of them to
//! pick defaults from a real text.
//!
//! All three live in `SuggestionConfig` and only `judge` in
//! `funput-suggestions/src/bigram/query/predict.rs` reads them, for the bigram and
//! the trigram alike. The completion path is untouched by them.

use std::path::Path;

use clap::Args;
use funput_suggestions::SuggestionConfig;

use super::report::{self, Tally};
use super::{Options, measure};

/// The share of personal Predicts that must be right for a row to count as a
/// candidate default: a wrong guess is worse than silence.
pub(super) const TARGET_RIGHT_PCT: f64 = 70.0;

const USES: [u16; 3] = [2, 3, 4];
const DOMINANCE: [u8; 5] = [40, 50, 60, 70, 80];
const EVIDENCE: [u16; 3] = [3, 4, 5];

#[derive(Debug, Args)]
pub(super) struct Thresholds {
    /// Override `context_predict_uses`: sightings of a pair before Predict may offer it.
    #[arg(long)]
    predict_uses: Option<u16>,
    /// Override `context_dominance_percent`: the share of a context's sightings its
    /// leading follower needs before Predict speaks.
    #[arg(long)]
    dominance: Option<u8>,
    /// Override `context_evidence_seen`: sightings of a context before Predict's
    /// silence is the user's answer and shipped data stays out.
    #[arg(long)]
    evidence: Option<u16>,
    /// Replay once per combination of thresholds and print one line for each.
    #[arg(long, conflicts_with_all = ["predict_uses", "dominance", "evidence", "json"])]
    pub(super) sweep: bool,
}

impl Thresholds {
    /// The keyboards' config with the overrides applied.
    pub(super) fn config(&self) -> SuggestionConfig {
        let mut config = SuggestionConfig::default();
        if let Some(uses) = self.predict_uses {
            config.context_predict_uses = uses;
        }
        if let Some(dominance) = self.dominance {
            config.context_dominance_percent = dominance;
        }
        if let Some(evidence) = self.evidence {
            config.context_evidence_seen = evidence;
        }
        config
    }
}

/// Every combination the sweep tries, the shipped defaults among them.
pub(super) fn grid() -> impl Iterator<Item = SuggestionConfig> {
    USES.into_iter().flat_map(|uses| {
        DOMINANCE.into_iter().flat_map(move |dominance| {
            EVIDENCE.into_iter().map(move |evidence| SuggestionConfig {
                context_predict_uses: uses,
                context_dominance_percent: dominance,
                context_evidence_seen: evidence,
                ..SuggestionConfig::default()
            })
        })
    })
}

/// Replay `runs` once per grid point and print a row each.
pub(super) fn run(text: &Path, runs: &[Vec<String>], options: &Options) -> Result<(), String> {
    report::print_sweep_header(text, options);
    for config in grid() {
        let tally: Tally = measure(
            runs,
            &Options {
                config,
                ..options.clone()
            },
        )?;
        report::print_sweep_row(&config, &tally);
    }
    Ok(())
}
