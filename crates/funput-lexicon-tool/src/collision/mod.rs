//! `collision`: `en.tsv` on stdin, one word list per composition rule in `--out DIR`.
//!
//! A word is listed for a [`Class`] when the class's rule shapes its keys and some
//! setup of the class's method leaves it a Vietnamese syllable at the word boundary
//! (`two` → `tơ`). The engine then writes those words' keys back at the boundary.
//! Each list is `<out>/<class>/words.txt`; hand corrections are read from
//! `<data>/<class>/overrides.txt` when it exists. Sorted, lowercase, one per line.
//! Design: `docs/features/english-collision-restore.md`.

mod class;
mod scan;

use std::collections::{BTreeMap, BTreeSet};
use std::fs;
use std::io::{self, Read};
use std::path::PathBuf;

use crate::wordlist::overrides::Overrides;
use crate::wordlist::setup::SETUPS;
use crate::wordlist::{invalid, ranks, write};
use class::Class;

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

    /// The class's hand corrections, or none when there is no file for it.
    fn overrides(&self, class: Class) -> io::Result<String> {
        let Some(data) = &self.data else {
            return Ok(String::new());
        };
        let path = data.join(class.name()).join("overrides.txt");
        if path.exists() {
            fs::read_to_string(path)
        } else {
            Ok(String::new())
        }
    }
}

pub(crate) fn run(options: &Options, mut input: impl Read) -> io::Result<()> {
    let mut tsv = String::new();
    input.read_to_string(&mut tsv)?;
    let mut overrides = BTreeMap::new();
    for class in Class::ALL {
        overrides.insert(class, options.overrides(class)?);
    }
    let lists = generate(&tsv, |class| overrides[&class].as_str()).map_err(invalid)?;
    for (class, words) in &lists {
        let dir = options.out.join(class.name());
        fs::create_dir_all(&dir)?;
        write(&dir.join("words.txt"), words)?;
        eprintln!("listed {} {} collisions", words.len(), class.name());
    }
    Ok(())
}

fn generate<'a>(
    tsv: &str,
    overrides: impl Fn(Class) -> &'a str,
) -> Result<BTreeMap<Class, BTreeSet<String>>, String> {
    let words: BTreeSet<String> = ranks(tsv).into_keys().collect();
    let mut lists = BTreeMap::new();
    for class in Class::ALL {
        let mut listed: BTreeSet<String> = words
            .iter()
            .filter(|word| collides(class, word))
            .cloned()
            .collect();
        Overrides::parse(overrides(class))?.apply(&mut listed)?;
        lists.insert(class, listed);
    }
    Ok(lists)
}

/// Whether `class`'s rule shapes `word` and some setup of its method leaves it
/// Vietnamese. The cheap gate runs first, so few words are ever typed.
fn collides(class: Class, word: &str) -> bool {
    class.admits(word)
        && SETUPS
            .iter()
            .filter(|setup| setup.method == class.method())
            .any(|&setup| scan::left_vietnamese(word, setup))
}

#[cfg(test)]
mod tests {
    use super::*;

    fn listed(tsv: &str, overrides: &str) -> Result<Vec<String>, String> {
        let mut lists = generate(tsv, |_| overrides)?;
        Ok(lists
            .remove(&Class::DeferredW)
            .unwrap()
            .into_iter()
            .collect())
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
    fn needs_an_out_dir_and_knows_its_flags() {
        let args = |list: &[&str]| list.iter().map(|arg| (*arg).to_owned()).collect::<Vec<_>>();
        assert!(Options::parse(&args(&["--out", "dir"])).is_ok());
        assert!(Options::parse(&args(&["--data", "data", "--out", "dir"])).is_ok());
        assert!(Options::parse(&args(&[])).is_err());
        assert!(Options::parse(&args(&["--out"])).is_err());
        assert!(Options::parse(&args(&["--overrides", "x", "--out", "dir"])).is_err());
    }
}
