//! `collision`: `en.tsv` on stdin, one word list per composition rule in `--out DIR`.
//!
//! A word is listed for a [`Collision`] class when the class's rule shapes its keys
//! and some setup of the class's method leaves it a Vietnamese syllable at the word
//! boundary (`two` → `tơ`). The engine then writes those words' keys back at the
//! boundary. The classes, their gates and their names come from the engine itself,
//! so the two cannot drift. Each list is `<out>/<class>/words.txt`; hand corrections
//! are read from `<data>/<class>/overrides.txt` when it exists. Sorted, lowercase,
//! one per line.
//! Design: `docs/features/english-collision-restore.md`.

mod scan;

use std::collections::BTreeSet;
use std::fs;
use std::io::{self, Read};
use std::path::PathBuf;

use crate::wordlist::overrides::Overrides;
use crate::wordlist::setup::SETUPS;
use crate::wordlist::{invalid, ranks, write};
use funput_engine::Collision;

pub(crate) struct Options {
    data: Option<PathBuf>,
    out: PathBuf,
}

impl Options {
    pub(crate) fn parse(args: &[String]) -> Result<Self, String> {
        let (mut data, mut out) = (None, None);
        let mut args = args.iter();
        while let Some(arg) = args.next() {
            let mut value = || {
                args.next()
                    .map(PathBuf::from)
                    .ok_or(format!("{arg} needs a value"))
            };
            match arg.as_str() {
                "--data" => data = Some(value()?),
                "--out" => out = Some(value()?),
                other => return Err(format!("unknown option {other}")),
            }
        }
        let out = out.ok_or("--out DIR is required")?;
        Ok(Self { data, out })
    }

    /// The class's hand corrections, or none when it has no `overrides.txt`.
    ///
    /// A `--data` directory that does not exist is an error, not an empty set: a
    /// mistyped path would otherwise drop every correction and write the words they
    /// remove straight back into the lists.
    fn overrides(&self, class: Collision) -> io::Result<String> {
        let Some(data) = &self.data else {
            return Ok(String::new());
        };
        if !data.is_dir() {
            let message = format!("--data {} is not a directory", data.display());
            return Err(io::Error::new(io::ErrorKind::NotFound, message));
        }
        let path = data.join(class.name()).join("overrides.txt");
        if path.is_file() {
            fs::read_to_string(path)
        } else {
            Ok(String::new())
        }
    }
}

pub(crate) fn run(options: &Options, mut input: impl Read) -> io::Result<()> {
    let mut tsv = String::new();
    input.read_to_string(&mut tsv)?;
    let mut overrides = Vec::with_capacity(Collision::ALL.len());
    for class in Collision::ALL {
        overrides.push(options.overrides(class)?);
    }
    let lists = generate(&tsv, &overrides).map_err(invalid)?;
    for (class, words) in Collision::ALL.into_iter().zip(&lists) {
        let dir = options.out.join(class.name());
        fs::create_dir_all(&dir)?;
        write(&dir.join("words.txt"), words)?;
        eprintln!("listed {} {} collisions", words.len(), class.name());
    }
    Ok(())
}

/// One list per class, in [`Collision::ALL`] order, each with the hand corrections
/// at the same index of `overrides` applied.
fn generate(tsv: &str, overrides: &[String]) -> Result<Vec<BTreeSet<String>>, String> {
    let words: BTreeSet<String> = ranks(tsv).into_keys().collect();
    Collision::ALL
        .into_iter()
        .zip(overrides)
        .map(|(class, overrides)| {
            let mut listed: BTreeSet<String> = words
                .iter()
                .filter(|word| collides(class, word))
                .cloned()
                .collect();
            Overrides::parse(overrides)?.apply(&mut listed)?;
            Ok(listed)
        })
        .collect()
}

/// Whether `class`'s rule shapes `word` and some setup of its method leaves it
/// Vietnamese. The cheap gate runs first, so few words are ever typed.
fn collides(class: Collision, word: &str) -> bool {
    class.shapes(word)
        && SETUPS
            .iter()
            .filter(|setup| setup.method == class.method())
            .any(|&setup| scan::left_vietnamese(word, setup))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn listed(tsv: &str, overrides: &str) -> Result<Vec<String>, String> {
        let lists = generate(tsv, &[overrides.to_owned()])?;
        Ok(lists[0].iter().cloned().collect())
    }

    #[test]
    fn lists_words_a_deferred_w_turns_into_syllables() {
        let tsv = "# ranked\ntwo\t0\ntow\t1\nswan\t2\nswift\t3\ntwin\t4\nhow\t5\n";
        // `tow`/`how` collide without a deferred w; `swift`/`twin` come back as typed.
        assert_eq!(listed(tsv, "").unwrap(), ["swan", "two"]);
    }

    #[test]
    fn applies_the_hand_corrections() {
        let tsv = "two\t0\nswan\t1\n";
        assert_eq!(listed(tsv, "# why\n-swan\n").unwrap(), ["two"]);
        assert!(listed(tsv, "-tow\n").is_err(), "a no-op override fails");
    }

    #[test]
    fn a_missing_data_dir_is_an_error() {
        let args = ["--data", "no/such/dir", "--out", "dir"].map(str::to_owned);
        let options = Options::parse(&args).unwrap();
        assert!(options.overrides(Collision::DeferredW).is_err());
        let no_data = Options::parse(&["--out", "dir"].map(str::to_owned)).unwrap();
        assert_eq!(no_data.overrides(Collision::DeferredW).unwrap(), "");
    }

    #[test]
    fn needs_an_out_dir_and_knows_its_flags() {
        let args = |list: &[&str]| list.iter().map(|arg| (*arg).to_owned()).collect::<Vec<_>>();
        assert!(Options::parse(&args(&["--out", "dir"])).is_ok());
        assert!(Options::parse(&args(&["--data", "data", "--out", "dir"])).is_ok());
        assert!(Options::parse(&args(&[])).is_err());
        assert!(Options::parse(&args(&["--out"])).is_err());
        assert!(Options::parse(&args(&["--overrides", "x", "--out", "dir"])).is_err());
    }
}
