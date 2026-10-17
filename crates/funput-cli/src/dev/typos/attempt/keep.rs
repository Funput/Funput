//! `funput dev typos --keep`: words typed on purpose, which must come out as typed.
//!
//! Chat abbreviations, brand names, a name without its marks — none is a Vietnamese
//! syllable, and several sit one key from one. Each is typed as its raw keystrokes
//! with the same wandering finger as the main run. The number that matters is
//! `rewritten`: a word whose every key landed where it was aimed, which correction
//! replaced anyway.
//!
//! `--prior uniform` runs it with no word store, and so no veto: how much protection
//! the engine alone gives a deliberate word no list knows. Any other prior vetoes
//! the way a keyboard does, through `is_known_word` — which knows this very list,
//! so there the number to see is zero.

use std::path::Path;
use std::{fs, io};

use crate::dev::encode::encode;
use crate::dev::typos::report::{print_human, print_json};
use crate::dev::typos::{Options, measure_typed};

/// The words in `path`: whitespace-separated, `#` comments, blank lines ignored.
pub(in crate::dev) fn load_words(path: &Path) -> io::Result<Vec<String>> {
    let text = fs::read_to_string(path)?;
    Ok(text
        .lines()
        .map(|line| line.split('#').next().unwrap_or_default())
        .flat_map(str::split_whitespace)
        .map(str::to_owned)
        .collect())
}

/// The keys a user presses for `word`: its letters as they are, unless it carries a
/// mark only a Vietnamese input method can type.
fn keys_for(word: &str, options: &Options) -> String {
    if word.is_ascii() {
        word.to_owned()
    } else {
        encode(word, options.method)
    }
}

/// Run the `--keep` measurement over `path` and print the report.
pub(in crate::dev) fn run_keep(path: &Path, options: &Options) -> io::Result<()> {
    let words = load_words(path)?;
    let tally = measure_keep(&words, options);
    if options.json {
        print_json(path, &words, &tally, options);
    } else {
        print_human(path, &words, &tally, options);
    }
    Ok(())
}

pub(in crate::dev) fn measure_keep(
    words: &[String],
    options: &Options,
) -> crate::dev::typos::report::Tally {
    let keys: Vec<String> = words.iter().map(|word| keys_for(word, options)).collect();
    measure_typed(words, &keys, options)
}
