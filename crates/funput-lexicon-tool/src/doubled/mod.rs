//! `doubled`: `en.tsv` on stdin, the engine's two doubled-key lists in `--out DIR`.
//!
//! - `words.txt` — the English words a Telex revert shortens (`password` →
//!   `pasword`); the engine gives them back their raw keys at the word boundary.
//! - `decisive.txt` — the raw-key prefixes at which it may do so mid-word, before
//!   the boundary (see `decisive`).
//!
//! Design: `docs/features/doubled-key-restore.md`. A word is left out when what the
//! revert leaves is what a doubled key may well have been typed for: a more frequent
//! word one doubled key shorter (`off` → `of`, the habit the revert exists for), or a
//! single letter (`ww` → `w`). `pass` stays, because `pas` is far rarer, and so do
//! `markers` and `casts`, whose lost key sits apart from its twin (see `habit`).
//! Both lists come from one scan, sorted and lowercase, one entry per line, so the
//! engine can binary-search them in place.

mod decisive;
mod habit;
mod overrides;
mod scan;

use std::collections::{BTreeSet, HashMap};
use std::fs;
use std::io::{self, Read};
use std::path::{Path, PathBuf};

use crate::{MIN_LEN, forms, rank};
use decisive::Words;
use overrides::Overrides;

pub(crate) struct Options {
    overrides: Option<PathBuf>,
    out: PathBuf,
}

impl Options {
    pub(crate) fn parse(args: &[String]) -> Result<Self, String> {
        let (mut overrides, mut out) = (None, None);
        let mut args = args.iter();
        while let Some(arg) = args.next() {
            let mut value = || {
                args.next()
                    .map(PathBuf::from)
                    .ok_or(format!("{arg} needs a value"))
            };
            match arg.as_str() {
                "--overrides" => overrides = Some(value()?),
                "--out" => out = Some(value()?),
                other => return Err(format!("unknown option {other}")),
            }
        }
        let out = out.ok_or("--out DIR is required")?;
        Ok(Self { overrides, out })
    }
}

/// The word list and the decisive prefixes, as the engine reads them.
struct Lists {
    words: BTreeSet<String>,
    decisive: BTreeSet<String>,
}

pub(crate) fn run(options: &Options, mut input: impl Read) -> io::Result<()> {
    let mut tsv = String::new();
    input.read_to_string(&mut tsv)?;
    let overrides = rank::read_optional(options.overrides.as_ref())?;
    let lists = generate(&tsv, &overrides).map_err(invalid)?;
    fs::create_dir_all(&options.out)?;
    write(&options.out.join("words.txt"), &lists.words)?;
    write(&options.out.join("decisive.txt"), &lists.decisive)?;
    eprintln!(
        "listed {} doubled-key words and {} decisive prefixes",
        lists.words.len(),
        lists.decisive.len()
    );
    Ok(())
}

fn generate(tsv: &str, overrides: &str) -> Result<Lists, String> {
    let overrides = Overrides::parse(overrides)?;
    let ranks = ranks(tsv);
    let (mut reverted, mut words) = (BTreeSet::new(), BTreeSet::new());
    for (word, &rank) in &ranks {
        if let Some(listed) = verdict(word, rank, &ranks) {
            reverted.insert(word.clone());
            if listed {
                words.insert(word.clone());
            }
        }
    }
    overrides.apply(&mut words)?;
    let excluded = Words::new(reverted.difference(&words).cloned());
    let english = Words::new(ranks.into_keys());
    let decisive = decisive::prefixes(&words, &english, &excluded);
    Ok(Lists { words, decisive })
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

/// `None` when no setup shortens `word`. Otherwise whether to list it: not when
/// some setup shortens it to what a doubled key may well have been typed for.
fn verdict(word: &str, rank: usize, ranks: &HashMap<String, usize>) -> Option<bool> {
    let shortened: Vec<String> = scan::SETUPS
        .iter()
        .filter_map(|&setup| scan::shortened(word, setup))
        .collect();
    (!shortened.is_empty()).then(|| !shortened.iter().any(|form| vetoes(word, rank, form, ranks)))
}

/// Whether `form`, what a revert left of `word`, may be what the user typed `word`
/// for: a doubled key collapsed (`off` → `of`, never `markers` → `makers`) into a
/// more frequent word, or into a single letter (`ww` → `w`, the only way to type `w`
/// in Telex nâng cao), which `en.tsv` never ranks.
fn vetoes(word: &str, rank: usize, form: &str, ranks: &HashMap<String, usize>) -> bool {
    habit::collapses_doubled_keys(word, form)
        && (form.len() < MIN_LEN || ranks.get(form).is_some_and(|&other| other < rank))
}

fn write(path: &Path, list: &BTreeSet<String>) -> io::Result<()> {
    let mut text = String::new();
    for entry in list {
        text.push_str(entry);
        text.push('\n');
    }
    fs::write(path, text)
}

fn invalid(message: String) -> io::Error {
    io::Error::new(io::ErrorKind::InvalidData, message)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn generated(tsv: &str) -> (Vec<String>, Vec<String>) {
        let lists = generate(tsv, "").unwrap();
        (
            lists.words.into_iter().collect(),
            lists.decisive.into_iter().collect(),
        )
    }

    #[test]
    fn lists_what_a_revert_shortens_and_where_it_is_decided() {
        let (words, decisive) = generated("# ranked\npassword\t0\ncorrect\t1\ntext\t2\n");
        assert_eq!(words, ["correct", "password"]);
        assert_eq!(decisive, ["corre", "passw"]);
    }

    #[test]
    fn leaves_out_a_word_whose_shortened_form_is_more_frequent() {
        let (words, decisive) = generated("of\t0\noff\t1\npass\t2\npas\t3\n");
        assert_eq!(words, ["pass"]);
        assert!(decisive.is_empty());
    }

    #[test]
    fn only_a_doubled_key_can_mean_the_shortened_word() {
        // `markers` loses an `r` that sits apart from its twin: nobody types it for `makers`.
        let (words, _) = generated("makers\t0\nmarkers\t1\n");
        assert_eq!(words, ["markers"]);
    }

    #[test]
    fn a_double_shortened_to_one_letter_is_the_habit() {
        // Telex nâng cao shortens `ww` to `w`, a letter `en.tsv` never ranks.
        let (words, _) = generated("ww\t0\n");
        assert!(words.is_empty(), "{words:?}");
    }

    #[test]
    fn a_left_out_word_keeps_its_own_prefixes_undecided() {
        let (words, decisive) = generated("of\t0\noff\t1\noffer\t2\n");
        assert_eq!(words, ["offer"]);
        assert_eq!(decisive, ["offe"]);
    }

    #[test]
    fn needs_an_out_dir_and_knows_its_flags() {
        let args = |list: &[&str]| list.iter().map(|arg| (*arg).to_owned()).collect::<Vec<_>>();
        assert!(Options::parse(&args(&["--out", "dir"])).is_ok());
        assert!(Options::parse(&args(&["--overrides", "x.txt", "--out", "dir"])).is_ok());
        assert!(Options::parse(&args(&[])).is_err());
        assert!(Options::parse(&args(&["--out"])).is_err());
        assert!(Options::parse(&args(&["--top", "3", "--out", "dir"])).is_err());
    }
}
