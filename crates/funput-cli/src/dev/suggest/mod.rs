//! `funput dev suggest`: replay a chat-style text through the suggestion engine
//! the way the iOS and Android keyboards drive it, and count what the bar would
//! have saved — and how often Predict guessed wrong. The offline check that
//! `docs/features/vietnamese-seed-suggestion.md` asks of every change to the
//! shipped pairs.

mod complete;
mod replay;
mod report;
mod sweep;
#[cfg(test)]
mod tests;
mod text;

use std::path::PathBuf;

use clap::{Args, ValueEnum};
use funput_core::{InputMethod, ToneStyle};
use funput_suggestions::{SeedLanguage, SuggestionConfig, SuggestionEngine};

use crate::cli::MethodArg;
use replay::Engines;
use report::Tally;

/// The text a run replays when none is given: small, hand-written, MIT.
const DEFAULT_TEXT: &str = "benchmarks/chat-vi.txt";

#[derive(Debug, Args)]
pub struct SuggestArgs {
    /// Text to replay, chat-style prose. Defaults to `benchmarks/chat-vi.txt`.
    text: Option<PathBuf>,
    /// Input method used to count keystrokes.
    #[arg(short, long, value_enum, default_value_t = MethodArg::Telex)]
    method: MethodArg,
    /// Tone placement the shipped data is spelled in.
    #[arg(long, value_enum, default_value_t = ToneArg::Modern)]
    tone: ToneArg,
    /// Leave the shipped Vietnamese data off (a shell that never enables it).
    #[arg(long)]
    no_seed: bool,
    /// Do not learn the words typed: measure what a new install sees.
    #[arg(long)]
    no_learn: bool,
    /// Attach this packed English lexicon (`en.lex`), as the keyboards do.
    #[arg(long)]
    lexicon: Option<PathBuf>,
    /// Print up to N of the most frequent wrong and silent Predicts.
    #[arg(long, default_value_t = 10)]
    show: usize,
    /// Emit machine-readable JSON instead of a human report.
    #[arg(long)]
    json: bool,
    #[command(flatten)]
    thresholds: sweep::Thresholds,
}

#[derive(Clone, Copy, Debug, ValueEnum)]
enum ToneArg {
    Modern,
    Traditional,
}

/// What one run measures with.
#[derive(Clone, Debug)]
pub(super) struct Options {
    config: SuggestionConfig,
    method: InputMethod,
    tone: ToneStyle,
    seed: bool,
    learn: bool,
    lexicon: Option<PathBuf>,
    show: usize,
    json: bool,
}

impl From<&SuggestArgs> for Options {
    fn from(args: &SuggestArgs) -> Self {
        Self {
            config: args.thresholds.config(),
            method: args.method.into(),
            tone: match args.tone {
                ToneArg::Modern => ToneStyle::Modern,
                ToneArg::Traditional => ToneStyle::Traditional,
            },
            seed: !args.no_seed,
            learn: !args.no_learn,
            lexicon: args.lexicon.clone(),
            show: args.show,
            json: args.json,
        }
    }
}

/// Run `funput dev suggest`. Errors are messages ready to print.
pub fn run(args: &SuggestArgs) -> Result<(), String> {
    let path = args
        .text
        .clone()
        .unwrap_or_else(|| PathBuf::from(DEFAULT_TEXT));
    let options = Options::from(args);
    let runs = text::load(&path).map_err(|e| format!("cannot read {}: {e}", path.display()))?;
    if args.thresholds.sweep {
        return sweep::run(&path, &runs, &options);
    }
    let tally = measure(&runs, &options)?;
    if options.json {
        report::print_json(&path, &tally, &options);
    } else {
        report::print_human(&path, &tally, &options);
    }
    Ok(())
}

/// Replay `runs` on fresh engines set up as `options` says.
fn measure(runs: &[Vec<String>], options: &Options) -> Result<Tally, String> {
    let mut main = engine(options, options.seed);
    if let Some(lexicon) = &options.lexicon {
        main.attach_lexicon(lexicon)
            .map_err(|e| format!("cannot open lexicon {}: {e}", lexicon.display()))?;
    }
    let twin = options.seed.then(|| engine(options, false));
    let mut engines = Engines {
        main,
        twin,
        learn: options.learn,
    };
    let mut tally = Tally::default();
    replay::replay(runs, &mut engines, options.method, &mut tally);
    Ok(tally)
}

/// The engine the keyboards build (in memory, `SuggestionConfig::default()` unless
/// the thresholds were overridden).
fn engine(options: &Options, seed: bool) -> SuggestionEngine {
    let mut engine = SuggestionEngine::in_memory(options.config);
    if seed {
        engine.set_seed_language(SeedLanguage::Vietnamese(options.tone));
    }
    engine
}
