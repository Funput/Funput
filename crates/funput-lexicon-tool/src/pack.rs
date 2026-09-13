//! `pack`: `en.tsv` on stdin, `en.lex` on stdout.
//!
//! The format and every rule it follows live in `funput-suggestions`; this only
//! moves bytes. The result is run through the library's own validation before a
//! byte is written, so a list the keyboards would refuse fails here instead.

use std::io::{self, Read, Write};
use std::time::Instant;

use funput_suggestions::lexicon_build::{encode, verify};

pub(crate) fn run(mut input: impl Read, out: &mut impl Write) -> io::Result<()> {
    let mut tsv = String::new();
    input.read_to_string(&mut tsv)?;
    let bytes = encode(&tsv)?;
    let started = Instant::now();
    let stats = verify(&bytes)?;
    let verified = started.elapsed();
    out.write_all(&bytes)?;
    out.flush()?;
    eprintln!(
        "packed {} words, {} heavy prefixes, {} bytes; verified in {:.2} ms",
        stats.words,
        stats.heavy_prefixes,
        stats.bytes,
        verified.as_secs_f64() * 1000.0
    );
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn packs_a_list_the_library_accepts() {
        let mut out = Vec::new();
        run("# ranked\nthe\t0\nof\t1\n".as_bytes(), &mut out).unwrap();
        assert!(out.starts_with(b"FPLX"));
        assert_eq!(verify(&out).unwrap().words, 2);
    }

    #[test]
    fn writes_nothing_for_a_list_the_library_refuses() {
        let mut out = Vec::new();
        let error = run("the\t0\nThe\t1\n".as_bytes(), &mut out).unwrap_err();
        assert_eq!(error.kind(), io::ErrorKind::InvalidData);
        assert!(out.is_empty());
    }
}
