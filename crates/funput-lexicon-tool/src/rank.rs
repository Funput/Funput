//! `rank`: from counts and word lists to `en.tsv`.

use std::collections::HashMap;
use std::fs;
use std::io::{self, Write};
use std::path::PathBuf;

use crate::forms;

const HEADER: &str = "\
# Funput English lexicon: word<TAB>rank, most frequent first. Generated; do not edit.
# Words: SCOWL/ESDB size 60, American. Ranking: Google Books Ngram v3, 2000-2019.
# Attribution: NOTICE.md. Regenerate: crates/funput-lexicon-tool/README.md.
";

pub(crate) struct Options {
    allowed: PathBuf,
    supplement: Option<PathBuf>,
    blocklist: Option<PathBuf>,
    top: usize,
    counts: Vec<PathBuf>,
}

impl Options {
    pub(crate) fn parse(args: &[String]) -> Result<Self, String> {
        let (mut allowed, mut supplement, mut blocklist) = (None, None, None);
        let (mut top, mut counts) = (0, Vec::new());
        let mut args = args.iter();
        while let Some(arg) = args.next() {
            let mut value = || args.next().ok_or(format!("{arg} needs a value"));
            match arg.as_str() {
                "--allowed" => allowed = Some(PathBuf::from(value()?)),
                "--supplement" => supplement = Some(PathBuf::from(value()?)),
                "--blocklist" => blocklist = Some(PathBuf::from(value()?)),
                "--top" => top = value()?.parse().map_err(|_| "--top takes a number")?,
                flag if flag.starts_with("--") => return Err(format!("unknown option {flag}")),
                path => counts.push(PathBuf::from(path)),
            }
        }
        let allowed = allowed.ok_or("--allowed is required")?;
        if counts.is_empty() {
            return Err("no count files given".to_owned());
        }
        Ok(Self {
            allowed,
            supplement,
            blocklist,
            top,
            counts,
        })
    }
}

pub(crate) fn run(options: &Options, out: &mut impl Write) -> io::Result<()> {
    let allowed = fs::read_to_string(&options.allowed)?;
    let mut forms = forms::canonical(allowed.lines());
    // A supplement entry is spelled as written, overriding SCOWL's form.
    let supplement = read_optional(options.supplement.as_ref())?;
    let mut floors = HashMap::new();
    for line in forms::entries(&supplement) {
        let (word, floor) = supplement_entry(line).ok_or_else(|| invalid(line))?;
        let key = word.to_ascii_lowercase();
        if let Some(rank) = floor {
            floors.insert(key.clone(), rank);
        }
        forms.insert(key, word.to_owned());
    }
    let blocklist = read_optional(options.blocklist.as_ref())?;
    let blocked = forms::blocked(forms::entries(&blocklist))
        .map_err(|message| io::Error::new(io::ErrorKind::InvalidData, message))?;

    let mut counts = HashMap::new();
    for path in &options.counts {
        add_counts(&fs::read_to_string(path)?, &mut counts)?;
    }

    out.write_all(HEADER.as_bytes())?;
    let ranked = forms::ranked(&forms, &counts, &blocked, &floors, options.top);
    for (rank, (word, _)) in ranked.iter().enumerate() {
        writeln!(out, "{word}\t{rank}")?;
    }
    out.flush()
}

fn read_optional(path: Option<&PathBuf>) -> io::Result<String> {
    path.map_or(Ok(String::new()), fs::read_to_string)
}

/// `word` or `word<TAB>rank`. A word the lexicon cannot hold is an error rather
/// than a silent skip: someone typed it into the supplement on purpose.
fn supplement_entry(line: &str) -> Option<(&str, Option<usize>)> {
    let (word, floor) = match line.split_once('\t') {
        Some((word, rank)) => (word.trim(), Some(rank.trim().parse().ok()?)),
        None => (line, None),
    };
    forms::admissible(word).then_some((word, floor))
}

fn invalid(line: &str) -> io::Error {
    io::Error::new(io::ErrorKind::InvalidData, format!("bad line: {line}"))
}

/// Sums `word\tcount` lines into `counts`; a word appears once per spelling.
fn add_counts(text: &str, counts: &mut HashMap<String, u64>) -> io::Result<()> {
    for line in text.lines() {
        let parsed = line
            .split_once('\t')
            .and_then(|(word, count)| Some((word, count.parse::<u64>().ok()?)));
        let Some((word, count)) = parsed else {
            return Err(invalid(line));
        };
        let total = counts.entry(word.to_owned()).or_insert(0);
        *total = total.saturating_add(count);
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sums_every_spelling_of_a_word() {
        let mut counts = HashMap::new();
        add_counts("the\t500\nthe\t120\n", &mut counts).unwrap();
        add_counts("the\t1\n", &mut counts).unwrap();
        assert_eq!(counts["the"], 621);
    }

    #[test]
    fn reads_a_supplement_word_with_or_without_a_floor() {
        assert_eq!(supplement_entry("selfie"), Some(("selfie", None)));
        assert_eq!(supplement_entry("cannot\t300"), Some(("cannot", Some(300))));
        assert_eq!(supplement_entry("cannot\tsoon"), None);
        assert_eq!(supplement_entry("Wi-Fi"), None);
    }

    #[test]
    fn refuses_a_malformed_count_line() {
        assert!(add_counts("the 500\n", &mut HashMap::new()).is_err());
    }

    #[test]
    fn requires_a_word_list_and_counts() {
        let args = |list: &[&str]| list.iter().map(|arg| (*arg).to_owned()).collect::<Vec<_>>();
        assert!(Options::parse(&args(&["counts.tsv"])).is_err());
        assert!(Options::parse(&args(&["--allowed", "wl.txt"])).is_err());
        assert!(Options::parse(&args(&["--allowed", "wl.txt", "--top", "x", "c.tsv"])).is_err());
        let options =
            Options::parse(&args(&["--allowed", "wl.txt", "--top", "5", "c.tsv"])).unwrap();
        assert_eq!((options.top, options.counts.len()), (5, 1));
    }
}
