//! `doubled`: `en.tsv` on stdin, the engine's doubled-key word list on stdout.
//!
//! The list is `crates/funput-engine/data/doubled/words.txt`: the English words a
//! Telex revert shortens (`password` → `pasword`), which the engine gives back their
//! raw keys at the word boundary. Design: `docs/features/doubled-key-restore.md`.
//!
//! A word is left out when what the revert leaves is itself a more frequent word:
//! `off` shortens to `of`, and typing `off` for `of` is the habit the revert exists
//! for. `pass` stays, because `pas` is far rarer. Output is sorted and lowercase, one
//! word per line, so the engine can binary-search it in place.

mod overrides;
mod scan;

use std::collections::{BTreeSet, HashMap};
use std::io::{self, Read, Write};
use std::path::PathBuf;

use crate::{forms, rank};
use overrides::Overrides;

pub(crate) struct Options {
    overrides: Option<PathBuf>,
}

impl Options {
    pub(crate) fn parse(args: &[String]) -> Result<Self, String> {
        match args {
            [] => Ok(Self { overrides: None }),
            [flag, path] if flag == "--overrides" => Ok(Self {
                overrides: Some(PathBuf::from(path)),
            }),
            _ => Err("doubled takes only --overrides FILE".to_owned()),
        }
    }
}

pub(crate) fn run(options: &Options, mut input: impl Read, out: &mut impl Write) -> io::Result<()> {
    let mut tsv = String::new();
    input.read_to_string(&mut tsv)?;
    let overrides = rank::read_optional(options.overrides.as_ref())?;
    let overrides = Overrides::parse(&overrides).map_err(invalid)?;
    let ranks = ranks(&tsv);
    let mut words: BTreeSet<String> = ranks
        .iter()
        .filter(|&(word, &rank)| shortened_to_a_rarer_form(word, rank, &ranks))
        .map(|(word, _)| word.clone())
        .collect();
    overrides.apply(&mut words).map_err(invalid)?;
    for word in &words {
        writeln!(out, "{word}")?;
    }
    out.flush()?;
    eprintln!("listed {} doubled-key words", words.len());
    Ok(())
}

/// Each listable word, lowercase, with its place in the frequency order. The first
/// spelling of a word wins, as `en.tsv` already keeps one per lowercase key.
fn ranks(tsv: &str) -> HashMap<String, usize> {
    let mut ranks = HashMap::new();
    let words = forms::entries(tsv).filter_map(|line| line.split('\t').next());
    for word in words.filter(|word| forms::admissible(word)) {
        let next = ranks.len();
        ranks.entry(word.to_ascii_lowercase()).or_insert(next);
    }
    ranks
}

/// Whether some setup shortens `word`, and none shortens it to a more frequent word.
fn shortened_to_a_rarer_form(word: &str, rank: usize, ranks: &HashMap<String, usize>) -> bool {
    let mut shortened = scan::SETUPS
        .iter()
        .filter_map(|&(method, rules)| scan::shortened(word, method, rules))
        .peekable();
    shortened.peek().is_some()
        && shortened.all(|form| ranks.get(&form).is_none_or(|&other| other > rank))
}

fn invalid(message: String) -> io::Error {
    io::Error::new(io::ErrorKind::InvalidData, message)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn generated(tsv: &str) -> String {
        let mut out = Vec::new();
        run(&Options { overrides: None }, tsv.as_bytes(), &mut out).unwrap();
        String::from_utf8(out).unwrap()
    }

    #[test]
    fn lists_what_a_revert_shortens_sorted() {
        assert_eq!(
            generated("# ranked\npassword\t0\ncorrect\t1\ntext\t2\n"),
            "correct\npassword\n"
        );
    }

    #[test]
    fn leaves_out_a_word_whose_shortened_form_is_more_frequent() {
        assert_eq!(generated("of\t0\noff\t1\npass\t2\npas\t3\n"), "pass\n");
    }

    #[test]
    fn parses_only_the_overrides_flag() {
        assert!(Options::parse(&[]).is_ok());
        assert!(Options::parse(&["--overrides".into(), "x.txt".into()]).is_ok());
        assert!(Options::parse(&["--top".into(), "3".into()]).is_err());
    }
}
