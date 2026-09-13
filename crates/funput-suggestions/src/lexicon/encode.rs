//! `en.tsv` → `en.lex`. Build-time only: compiled for tests and behind the
//! `lexicon-build` feature, never into a keyboard.
//!
//! The input is what `funput-lexicon-tool rank` writes — `word<TAB>rank` lines
//! and `#` comments — but nothing here trusts that: every rule the reader will
//! enforce is checked first, with the line that broke it, so a bad list fails
//! at build time and not on a phone.

use std::io;

use super::format::{self, BLOCK, Header, MAX_LEN, MAX_WORDS, MIN_LEN, VERSION};
use crate::binary::{checksum, put_u16, put_u32};
use crate::index::TOP_K;

struct Word<'a> {
    text: &'a str,
    rank: u16,
}

impl Word<'_> {
    fn key(&self) -> &[u8] {
        self.text.as_bytes()
    }
}

/// Encodes a ranked word list. The result depends on the words and their ranks,
/// not on the order of the lines.
pub fn encode(tsv: &str) -> io::Result<Vec<u8>> {
    let mut words = parse(tsv)?;
    words.sort_unstable_by(|left, right| format::compare_keys(left.key(), right.key()));
    if let Some(pair) = words
        .windows(2)
        .find(|pair| format::compare_keys(pair[0].key(), pair[1].key()).is_eq())
    {
        let (left, right) = (pair[0].text, pair[1].text);
        return Err(invalid(format!("`{left}` and `{right}` are the same word")));
    }

    // Blocks, then Words, then Heavy.
    let mut body = Vec::new();
    let mut section = Vec::new();
    for (id, word) in words.iter().enumerate() {
        if id % BLOCK == 0 {
            put_u32(&mut body, section.len() as u32);
        }
        section.push(word.text.len() as u8);
        put_u16(&mut section, word.rank);
        section.extend_from_slice(word.text.as_bytes());
    }
    body.extend_from_slice(&section);
    let heavy = heavy_entries(&words);
    for (lo, plen, top) in &heavy {
        put_u16(&mut body, *lo);
        body.push(*plen);
        top.iter().for_each(|id| put_u16(&mut body, *id));
    }

    let header = Header {
        version: VERSION,
        flags: 0,
        word_count: words.len() as u32,
        words_len: section.len() as u32,
        heavy_count: heavy.len() as u32,
        body_crc: checksum(&body),
    };
    let mut bytes = Vec::new();
    header.write(&mut bytes);
    bytes.extend_from_slice(&body);
    Ok(bytes)
}

fn parse(tsv: &str) -> io::Result<Vec<Word<'_>>> {
    let mut words = Vec::new();
    for (index, line) in tsv.lines().map(str::trim_end).enumerate() {
        if line.is_empty() || line.starts_with('#') {
            continue;
        }
        let bad = |why: &str| invalid(format!("line {}: {why}: {line}", index + 1));
        let (text, rank) = line
            .split_once('\t')
            .ok_or_else(|| bad("expected word<TAB>rank"))?;
        if !format::admissible(text.as_bytes()) {
            return Err(bad(&format!(
                "a word is {MIN_LEN} to {MAX_LEN} ASCII letters"
            )));
        }
        let rank = rank
            .parse()
            .map_err(|_| bad("rank is not a number up to 65535"))?;
        words.push(Word { text, rank });
    }
    if words.len() > MAX_WORDS {
        return Err(invalid(format!(
            "{} words, more than {MAX_WORDS}",
            words.len()
        )));
    }
    match words
        .iter()
        .find(|word| usize::from(word.rank) >= words.len())
    {
        Some(word) => Err(invalid(format!(
            "`{}` ranks {}, past the last word",
            word.text, word.rank
        ))),
        None => Ok(words),
    }
}

/// Every prefix shared by more than [`TOP_K`] words, with its best three by
/// rank, ordered by `(lo, plen)`. Sorted words sharing a prefix are adjacent, so
/// each prefix length is one linear pass.
fn heavy_entries(words: &[Word<'_>]) -> Vec<(u16, u8, [u16; TOP_K])> {
    let mut entries = Vec::new();
    for plen in MIN_LEN..=MAX_LEN {
        let mut start = 0;
        while start < words.len() {
            let Some(prefix) = words[start].text.as_bytes().get(..plen) else {
                start += 1;
                continue;
            };
            let run = words[start..]
                .iter()
                .take_while(|word| format::has_prefix(word.text.as_bytes(), prefix));
            let end = start + run.count();
            if end - start > TOP_K {
                let mut ids: Vec<usize> = (start..end).collect();
                ids.sort_unstable_by_key(|id| (words[*id].rank, *id));
                entries.push((
                    start as u16,
                    plen as u8,
                    [ids[0], ids[1], ids[2]].map(|id| id as u16),
                ));
            }
            start = end;
        }
    }
    entries.sort_unstable_by_key(|(lo, plen, _)| (*lo, *plen));
    entries
}

fn invalid(message: String) -> io::Error {
    io::Error::new(io::ErrorKind::InvalidData, message)
}
