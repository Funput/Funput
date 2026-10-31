//! The higher-order tables, after the blocked list from v4.
//!
//! `[u8 order · u32 byte_len · body]*`, where an order-3 body is
//! `u32 count · count × (u32 older · u32 previous · edges)` and `edges` is the
//! same context count and follower list a word record carries.
//!
//! Only entries whose context is still live go out, and only their live
//! followers, for the reason `words.rs` gives. `byte_len` lets a reader step over
//! a block of an order it does not know, so a 4-gram table needs no new version.

use std::io;

use super::words::{decode_edges, encode_edges};
use crate::binary::{Cursor, invalid_data, put_u32};
use crate::ngram::{Entry, TRIGRAM, TrigramTable, WordRef};
use crate::types::WordRecord;

/// Two key words, a context count and an empty follower list.
const MIN_ENTRY_BYTES: usize = 4 + 4 + 2 + 1;

pub(super) fn encode(bytes: &mut Vec<u8>, table: &TrigramTable, words: &[WordRecord]) {
    let live: Vec<&Entry<2>> = table.entries().filter(|e| e.is_live(words)).collect();
    if live.is_empty() {
        return;
    }
    bytes.push(TRIGRAM);
    let length_at = bytes.len();
    put_u32(bytes, 0);
    put_u32(bytes, live.len() as u32);
    for entry in live {
        for word in entry.key {
            put_u32(bytes, word.index);
        }
        encode_edges(bytes, entry.context_seen, &entry.followers, words);
    }
    let length = (bytes.len() - length_at - 4) as u32;
    bytes[length_at..length_at + 4].copy_from_slice(&length.to_le_bytes());
}

/// The trigram entries, at generation zero like every record they point at.
/// One naming a word past the end of the list is dropped rather than kept dead.
pub(super) fn decode(cursor: &mut Cursor<'_>, words: usize) -> io::Result<Vec<Entry<2>>> {
    let mut entries = Vec::new();
    while !cursor.is_empty() {
        let order = cursor.u8()?;
        let length = cursor.u32()? as usize;
        let mut block = Cursor::new(cursor.take(length)?);
        if order != TRIGRAM {
            continue;
        }
        let count = block.u32()? as usize;
        if count > block.remaining() / MIN_ENTRY_BYTES {
            return Err(invalid_data());
        }
        entries.reserve(count);
        for _ in 0..count {
            let key = [word_at(block.u32()?), word_at(block.u32()?)];
            let (context_seen, followers) = decode_edges(&mut block)?;
            if key.iter().all(|word| (word.index as usize) < words) {
                entries.push(Entry {
                    key,
                    context_seen,
                    followers,
                });
            }
        }
        if !block.is_empty() {
            return Err(invalid_data());
        }
    }
    Ok(entries)
}

fn word_at(index: u32) -> WordRef {
    WordRef {
        index,
        generation: 0,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::binary::{checksum, put_u16};
    use crate::ngram::PROBE_WINDOW;

    fn words(texts: &[&str]) -> Vec<WordRecord> {
        texts
            .iter()
            .map(|text| WordRecord::new((*text).to_owned(), 1, 0))
            .collect()
    }

    fn table(words: &[WordRecord]) -> TrigramTable {
        let mut table = TrigramTable::new(PROBE_WINDOW);
        let at = |index| WordRef::at(index, words).unwrap();
        table.record([at(0), at(1)], at(2), words);
        table.record([at(0), at(1)], at(2), words);
        table
    }

    #[test]
    fn an_entry_round_trips_through_its_block() {
        let words = words(&["cảm", "ơn", "bạn"]);
        let mut bytes = Vec::new();
        encode(&mut bytes, &table(&words), &words);
        let entries = decode(&mut Cursor::new(&bytes), words.len()).unwrap();
        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].key.map(|word| word.index), [0, 1]);
        assert_eq!(entries[0].context_seen, 2);
        assert_eq!(entries[0].followers[0].word, 2);
        assert_eq!(entries[0].followers[0].uses, 2);
    }

    #[test]
    fn a_block_of_an_unknown_order_is_stepped_over() {
        let words = words(&["cảm", "ơn", "bạn"]);
        let mut bytes = vec![4];
        put_u32(&mut bytes, 3);
        bytes.extend_from_slice(b"???");
        encode(&mut bytes, &table(&words), &words);
        let entries = decode(&mut Cursor::new(&bytes), words.len()).unwrap();
        assert_eq!(entries.len(), 1);
    }

    #[test]
    fn an_entry_naming_a_word_past_the_list_is_dropped() {
        let words = words(&["cảm", "ơn", "bạn"]);
        let mut bytes = Vec::new();
        encode(&mut bytes, &table(&words), &words);
        assert!(decode(&mut Cursor::new(&bytes), 1).unwrap().is_empty());
    }

    #[test]
    fn a_v3_snapshot_reads_with_no_trigrams() {
        let words = words(&["cảm", "ơn", "bạn"]);
        // No live entry writes no block, so this is the v3 layout under a v4 stamp.
        let mut bytes = super::super::encode(&words, &[], &TrigramTable::new(PROBE_WINDOW), 7);
        bytes.truncate(bytes.len() - 4);
        let mut version = Vec::new();
        put_u16(&mut version, 3);
        bytes[8..10].copy_from_slice(&version);
        let sum = checksum(&bytes);
        put_u32(&mut bytes, sum);
        let snapshot = super::super::decode(&bytes).unwrap().unwrap();
        assert_eq!(snapshot.words.len(), 3);
        assert!(snapshot.trigrams.is_empty());
    }
}
