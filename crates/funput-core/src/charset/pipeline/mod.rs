//! Converting a whole document, in the two steps a converter UI actually needs.
//!
//! [`convert`](super::convert) is one pass: `source → Atom → target`. That is the
//! right shape for a string, and the wrong one for a window, for two reasons.
//!
//! **A window shows the result before it writes it.** The bytes a file receives are
//! not the characters a pane shows — [`encode_bytes`] narrows anything that will not
//! fit in a byte to `?`. A window that previews with one call and saves with another
//! shows text the file will not hold. So [`render`] returns **both**, made together,
//! and their agreement is structural rather than a convention someone has to keep.
//!
//! **A window changes the target, over and over.** Reading the source is the
//! expensive half and it does not depend on the target, so it is split out:
//! [`read`] once, [`render`] per target. That also fixes the subtler problem — a
//! one-pass `source → target` and a two-step `source → Unicode → target` are not the
//! same conversion, because the round trip through a real Unicode string is an extra
//! `Atom::from_char(atom.to_char())`. That is identity for a letter the source
//! defines and **not** for a code it does not: TCVN3's `0xC2` passes through as `Â`,
//! whose real TCVN3 code is `0xA2`. Having one way to do this is what stops two
//! shells from disagreeing about the same document.
//!
//! Not to be confused with [`pivot`](super::pivot), which is the `Atom` every codec
//! maps to. This is the pipeline *over* it.

mod cost;

use super::{Charset, convert, encode_bytes, is_byte_oriented};

pub use cost::Cost;

/// A document read into precomposed Unicode — the half that does not depend on the
/// target, so a window can keep it while the user tries one target after another.
#[non_exhaustive]
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Pivoted {
    /// The document as precomposed Unicode.
    pub text: String,
    /// Characters the **source** charset never defined, so reading them was a guess.
    /// A number near the length of the document is the clearest sign there is that
    /// the wrong source was picked.
    pub undefined: usize,
    /// Characters understood exactly whose spelling the source wrote differently.
    /// Not a loss.
    pub normalized: usize,
}

/// What a target makes of a document: the characters, the bytes, and the cost.
///
/// `text` and `bytes` are two views of one result, not two results. For a
/// byte-oriented target `text` is `bytes` read back as Latin-1, so a pane showing
/// `text` shows exactly what a file holding `bytes` holds — `?` included.
#[non_exhaustive]
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Rendered {
    pub text: String,
    pub bytes: Vec<u8>,
    pub cost: Cost,
}

/// Read a document into Unicode. The expensive half, and the target-independent one.
///
/// `from` must be what the text actually **is**; guessing it is
/// [`detect`](super::detect)'s job, not this one's.
pub fn read(text: &str, from: Charset) -> Pivoted {
    let out = convert(text, from, Charset::Unicode);
    Pivoted {
        text: out.text,
        undefined: out.unmapped,
        normalized: out.normalized,
    }
}

/// Write a read document out as `to` spells it.
///
/// Built on [`encode_bytes`] rather than beside it, so the bytes here are the bytes
/// a file gets, and `text` is derived from them rather than computed in parallel.
pub fn render(pivoted: &Pivoted, to: Charset) -> Rendered {
    let (bytes, out) = encode_bytes(&pivoted.text, to);
    let cost = Cost::of(pivoted, to, &out);
    // Latin-1 is total, so reading the bytes back cannot fail — and it is the only
    // honest answer to "what will the pane show", because it is what the file holds.
    let text = if is_byte_oriented(to) {
        bytes.iter().copied().map(char::from).collect()
    } else {
        out.text
    };
    Rendered { text, bytes, cost }
}

#[cfg(test)]
mod tests;
