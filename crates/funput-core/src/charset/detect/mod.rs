//! Guessing which charset a piece of text is written in.
//!
//! Every candidate is tried: decode the text as that charset and see how much of
//! the result is spellable Vietnamese. The right charset produces words; a wrong
//! one produces letters that do not go together. `None` when the evidence does not
//! separate them, which is the honest answer more often than it looks — pure ASCII
//! reads the same under all four, and no amount of it says anything.
//!
//! # What it cannot do
//!
//! Every judgement here is *relative*: the best of four hypotheses wins. A charset
//! nobody implemented has no hypothesis of its own, so it is answered with its
//! nearest neighbour. VISCII shares Latin-1's letters with TCVN3, so VISCII text
//! is confidently reported as TCVN3 and converts to something subtly wrong. No
//! cheap threshold fixes this — the only real fix is to implement VISCII.
//! `tests.rs` pins the behaviour so the next reader finds it already known.
//!
//! # Cost
//!
//! Detection decodes the text once per candidate, so it reads a **prefix** rather
//! than a whole document. An interface that detects on the prefix and converts the
//! whole text is doing the right thing.

mod score;

use super::{ALL, Charset, Conversion, convert, decode_bytes};
use score::{Score, score};

/// How much text is enough to decide. Four full decodings run over this, and the
/// answer stops changing long before it: a few hundred words already separate the
/// charsets by a wide margin.
const PREFIX: usize = 8 * 1024;

/// Guess the charset `text` is written in, or `None` when the evidence does not
/// pick one out.
///
/// `None` covers three different situations on purpose — no evidence at all
/// (empty, digits), a genuine tie between two charsets that spell this text the
/// same way, and text that reads identically under every charset. A caller that
/// needs to tell them apart should ask [`convert`] for each candidate and compare
/// [`Conversion`] itself.
pub fn detect(text: &str) -> Option<Charset> {
    best(|charset| convert(prefix(text), charset, Charset::Unicode))
}

/// Guess the charset of raw bytes — the door for a file rather than a clipboard.
///
/// Not the same question as [`detect`] on the same content, and the difference is
/// real rather than an oversight: bytes can be invalid UTF-8, and that is strong
/// evidence *against* the two Unicode charsets which `detect` cannot see, because
/// by the time it has a `&str` the damage has already been repaired.
pub fn detect_bytes(bytes: &[u8]) -> Option<Charset> {
    let head = &bytes[..PREFIX.min(bytes.len())];
    best(|charset| decode_bytes(head, charset))
}

/// The candidate that explains `decoded` best, if one does.
fn best(decoded: impl Fn(Charset) -> Conversion) -> Option<Charset> {
    let mut ranked: Vec<(Score, Charset)> = ALL
        .iter()
        .map(|&charset| (score(&decoded(charset)), charset))
        .collect();
    // Descending, so the winner and the runner-up are the first two.
    ranked.sort_unstable_by_key(|&(score, _)| core::cmp::Reverse(score));

    let (winner, charset) = ranked[0];
    let (runner_up, _) = ranked[1];
    (winner > runner_up && winner.is_plausible()).then_some(charset)
}

/// The leading `PREFIX` bytes, cut at a character boundary.
fn prefix(text: &str) -> &str {
    if text.len() <= PREFIX {
        return text;
    }
    let mut end = PREFIX;
    while end > 0 && !text.is_char_boundary(end) {
        end -= 1;
    }
    &text[..end]
}

#[cfg(test)]
mod tests;
