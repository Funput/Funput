//! Type the runs back through the suggestion engine the way a keyboard does:
//! ask Predict after a space, ask for completions while a word is typed, then
//! learn the word with the one before it.

use funput_core::InputMethod;
use funput_suggestions::SuggestionEngine;

use super::complete::{complete, keys, lowercase};
use super::report::{Source, Tally};

/// Shortest word the shells learn, and the shortest prefix they ask about.
pub(super) const MIN_LETTERS: usize = 2;
/// Longest word the shells learn (`max_token_scalars`).
const MAX_LETTERS: usize = 32;

/// The engines one replay drives.
pub(super) struct Engines {
    /// What the keyboard runs.
    pub(super) main: SuggestionEngine,
    /// The same engine with the shipped data off, learning the same words, so
    /// a Predict can be traced to the shipped pairs or to what was learned.
    /// `None` when the main engine already has the shipped data off.
    pub(super) twin: Option<SuggestionEngine>,
    /// Whether committed words are learned, as on a device.
    pub(super) learn: bool,
}

/// Replay every run, adding to `tally`.
pub(super) fn replay(
    runs: &[Vec<String>],
    engines: &mut Engines,
    method: InputMethod,
    tally: &mut Tally,
) {
    for run in runs {
        let mut older: Option<&str> = None;
        let mut previous: Option<&str> = None;
        for word in run {
            type_word(word, older, previous, engines, method, tally);
            let letters = word.chars().count();
            if (MIN_LETTERS..=MAX_LETTERS).contains(&letters) {
                engines.commit(previous, word);
                older = previous;
                previous = Some(word);
            } else {
                // The trackers complete no token, so the context is gone.
                older = None;
                previous = None;
            }
        }
    }
}

impl Engines {
    fn commit(&mut self, previous: Option<&str>, word: &str) {
        if !self.learn {
            return;
        }
        self.main.learn_after(previous, word);
        if let Some(twin) = &mut self.twin {
            twin.learn_after(previous, word);
        }
    }

    /// Where `offered` came from: personal when the engine without shipped
    /// data offers it after `context` too, or when there is no such engine.
    fn source(&self, context: &str, offered: &str) -> Source {
        match &self.twin {
            Some(twin) if !holds(twin, context, offered) => Source::Shipped,
            _ => Source::Personal,
        }
    }
}

fn holds(engine: &SuggestionEngine, context: &str, word: &str) -> bool {
    lowercase(engine.suggest_after_words(context, ""))
        .iter()
        .any(|offered| offered == word)
}

fn type_word(
    word: &str,
    older: Option<&str>,
    previous: Option<&str>,
    engines: &Engines,
    method: InputMethod,
    tally: &mut Tally,
) {
    let keys = keys(word, method);
    tally.words += 1;
    tally.keys += keys;
    if let Some(previous) = previous {
        let context = match older {
            Some(older) => format!("{older} {previous}"),
            None => previous.to_owned(),
        };
        if predict(word, previous, &context, engines, tally) {
            tally.saved += keys;
            return;
        }
        complete(word, keys, &context, engines, method, tally);
    } else {
        complete(word, keys, "", engines, method, tally);
    }
}

/// Ask Predict before `word` is typed; true when the bar held `word`.
fn predict(
    word: &str,
    previous: &str,
    context: &str,
    engines: &Engines,
    tally: &mut Tally,
) -> bool {
    let bar = lowercase(engines.main.suggest_after_words(context, ""));
    let Some(top) = bar.first() else {
        tally.predict.silent(previous, word);
        return false;
    };
    match bar.iter().position(|offered| offered == word) {
        Some(slot) => {
            let source = engines.source(context, word);
            tally.predict.hit(source, slot, bar.len());
            true
        }
        // A wrong bar is charged to the data its first word came from.
        None => {
            let source = engines.source(context, top);
            tally.predict.miss(source, previous, &bar, word);
            false
        }
    }
}
