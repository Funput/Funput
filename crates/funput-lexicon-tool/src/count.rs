//! `count`: one streaming pass over a decompressed 1-gram file.
//!
//! The English 1-grams are 12.8 GB compressed, more than a dev machine wants to
//! keep, so they are piped straight from the download into this and only the
//! counts survive. Output is one line per spelling that passed; `rank` sums the
//! spellings of a word (`The`, `the`, `THE`) itself.

use std::io::{self, BufRead, Write};
use std::ops::RangeInclusive;

use crate::MAX_LEN;

/// The years the ranking reflects. v3 ends at 2019; starting at 2000 ranks the
/// words people write today rather than those in print a century ago.
const YEARS: RangeInclusive<u64> = 2000..=2019;

/// A spelling seen fewer times than this over [`YEARS`] cannot move a word
/// anywhere near the cut, and dropping it keeps the output a few megabytes.
const MIN_SPELLING_COUNT: u64 = 100;

pub(crate) fn run(mut input: impl BufRead, out: &mut impl Write) -> io::Result<()> {
    let mut line = Vec::with_capacity(4096);
    loop {
        line.clear();
        if input.read_until(b'\n', &mut line)? == 0 {
            break;
        }
        if let Some((word, count)) = parse(&line)
            && count >= MIN_SPELLING_COUNT
        {
            writeln!(out, "{word}\t{count}")?;
        }
    }
    out.flush()
}

/// `word\tyear,matches,volumes\t…` → the lowercased word and its matches over
/// [`YEARS`]. Tagged entries (`run_VERB`) and anything but plain ASCII letters
/// are skipped: the untagged entry already counts every use of the word.
fn parse(line: &[u8]) -> Option<(String, u64)> {
    let line = line.strip_suffix(b"\n").unwrap_or(line);
    let mut fields = line.split(|byte| *byte == b'\t');
    let word = fields.next()?;
    if word.is_empty() || word.len() > MAX_LEN || !word.iter().all(u8::is_ascii_alphabetic) {
        return None;
    }
    let mut total = 0u64;
    for field in fields {
        let mut parts = field.split(|byte| *byte == b',');
        let year = number(parts.next()?)?;
        let matches = number(parts.next()?)?;
        if YEARS.contains(&year) {
            total = total.saturating_add(matches);
        }
    }
    let word = std::str::from_utf8(word).ok()?.to_ascii_lowercase();
    Some((word, total))
}

fn number(digits: &[u8]) -> Option<u64> {
    std::str::from_utf8(digits).ok()?.parse().ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sums_only_the_ranked_years_and_lowercases() {
        let line = b"Halifax\t1999,7,1\t2000,2,2\t2019,3,1\t2020,5,5\n";
        assert_eq!(parse(line), Some(("halifax".to_owned(), 5)));
    }

    #[test]
    fn skips_tagged_and_non_letter_entries() {
        assert_eq!(parse(b"run_VERB\t2000,9,9"), None);
        assert_eq!(parse(b"don't\t2000,9,9"), None);
        assert_eq!(parse(b"caf\xc3\xa9\t2000,9,9"), None);
        assert_eq!(parse(b"\t2000,9,9"), None);
    }

    #[test]
    fn rejects_a_malformed_year_field() {
        assert_eq!(parse(b"word\t2000"), None);
        assert_eq!(parse(b"word\tyear,9,9"), None);
    }

    #[test]
    fn writes_only_spellings_over_the_floor() {
        let input: &[u8] = b"the\t2001,500,1\nrare\t2001,3,1\nThe\t2002,120,1\n";
        let mut out = Vec::new();
        run(input, &mut out).unwrap();
        assert_eq!(out, b"the\t500\nthe\t120\n");
    }
}
