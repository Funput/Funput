//! English lexicon tests: the encoder in `encode`.
//!
//! The helpers decode `en.lex` by hand, straight from the documented layout, so
//! the tests check the bytes against the format rather than against the code
//! that wrote them.

mod encode;

use crate::lexicon::encode::encode;
use crate::lexicon::format::{
    BLOCK, HEADER_BYTES, HEAVY_BYTES, Header, WORD_HEAD_BYTES, block_count,
};

/// A `word<TAB>rank` list, ranked in the order given.
fn tsv(words: &[&str]) -> String {
    words
        .iter()
        .enumerate()
        .map(|(rank, word)| format!("{word}\t{rank}\n"))
        .collect()
}

/// Sixty words with heavy prefixes (`th`, `the`), light ones (`wh`, `wi`),
/// words of two letters, and words whose case must survive (`iPhone`, `COVID`).
fn fixture() -> Vec<&'static str> {
    let words = vec![
        "the", "of", "and", "to", "in", "is", "that", "it", "was", "for", "on", "are", "with",
        "they", "be", "at", "one", "have", "this", "from", "or", "had", "by", "hot", "word", "but",
        "what", "some", "we", "can", "out", "other", "were", "all", "there", "when", "up", "use",
        "your", "how", "said", "an", "each", "she", "which", "do", "their", "time", "if", "will",
        "way", "about", "many", "then", "them", "these", "so", "iPhone", "Monday", "COVID",
    ];
    assert_eq!(words.len(), 60);
    words
}

fn fixture_bytes() -> Vec<u8> {
    encode(&tsv(&fixture())).unwrap()
}

fn u16_at(bytes: &[u8], at: usize) -> u16 {
    u16::from_le_bytes([bytes[at], bytes[at + 1]])
}

fn u32_at(bytes: &[u8], at: usize) -> u32 {
    u32::from_le_bytes([bytes[at], bytes[at + 1], bytes[at + 2], bytes[at + 3]])
}

fn words_start(header: &Header) -> usize {
    HEADER_BYTES + block_count(header.word_count as usize) * 4
}

/// Every word as `(offset into Words, rank, text)`, in file order.
fn decoded_words(bytes: &[u8]) -> Vec<(usize, u16, String)> {
    let header = Header::parse(bytes).unwrap();
    let start = words_start(&header);
    let mut offset = 0;
    let mut words = Vec::new();
    for _ in 0..header.word_count {
        let len = usize::from(bytes[start + offset]);
        let rank = u16_at(bytes, start + offset + 1);
        let text = &bytes[start + offset + WORD_HEAD_BYTES..][..len];
        words.push((offset, rank, String::from_utf8(text.to_vec()).unwrap()));
        offset += WORD_HEAD_BYTES + len;
    }
    assert_eq!(offset, header.words_len as usize);
    words
}

/// Every heavy entry as `(lo, plen, top)`, in file order.
fn decoded_heavy(bytes: &[u8]) -> Vec<(u16, u8, [u16; 3])> {
    let header = Header::parse(bytes).unwrap();
    let start = words_start(&header) + header.words_len as usize;
    (0..header.heavy_count as usize)
        .map(|index| {
            let at = start + index * HEAVY_BYTES;
            let top = [0, 1, 2].map(|slot| u16_at(bytes, at + 3 + slot * 2));
            (u16_at(bytes, at), bytes[at + 2], top)
        })
        .collect()
}
