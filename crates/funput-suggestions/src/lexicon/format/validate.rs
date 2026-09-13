//! Everything a file must satisfy before the lookup may touch it.
//!
//! One pass at open, on the worker, allowed to allocate. Its promise is what
//! lets the lookup stay simple: a file accepted here cannot send a lookup out
//! of bounds, and every structural fact the lookup leans on — words sorted and
//! unique by key, block offsets true, heavy entries naming real runs — holds.
//! A random flipped byte is caught by the CRC; the checks below are for a file
//! that is consistent and still wrong.

use std::io;

use super::sections::{self, Layout};
use super::{BLOCK, HEADER_BYTES, Header, MAX_FILE_BYTES, MAX_LEN, MIN_LEN, VERSION};
use super::{admissible, compare_keys, has_prefix};
use crate::binary::{checksum, invalid_data};
use crate::index::TOP_K;

pub(crate) fn validate(bytes: &[u8]) -> io::Result<Layout> {
    if u64::try_from(bytes.len()).map_or(true, |len| len > MAX_FILE_BYTES) {
        return Err(invalid_data());
    }
    let header = Header::parse(bytes)?;
    if header.version > VERSION {
        // Written by a newer build. Not damage: an older app is reading it.
        return Err(io::Error::new(
            io::ErrorKind::Unsupported,
            "en.lex is newer than this build",
        ));
    }
    let layout = Layout::of(&header).filter(|layout| layout.heavy.end == bytes.len());
    let body = bytes.get(HEADER_BYTES..).ok_or_else(invalid_data)?;
    match layout {
        Some(layout)
            if header.version == VERSION
                && header.flags == 0
                && checksum(body) == header.body_crc =>
        {
            let words = words(bytes, &layout).ok_or_else(invalid_data)?;
            heavy(bytes, &layout, &words).ok_or_else(invalid_data)?;
            Ok(layout)
        }
        _ => Err(invalid_data()),
    }
}

/// Every word as `(rank, bytes)`, in file order, or `None` if any is malformed,
/// out of order, or not where its block says it starts.
fn words<'a>(bytes: &'a [u8], layout: &Layout) -> Option<Vec<(u16, &'a [u8])>> {
    let blocks = bytes.get(layout.blocks.clone())?;
    let section = bytes.get(layout.words.clone())?;
    let mut words: Vec<(u16, &[u8])> = Vec::with_capacity(layout.word_count);
    let mut offset = 0;
    for id in 0..layout.word_count {
        if id % BLOCK == 0 && sections::block_at(blocks, id / BLOCK)? != offset {
            return None;
        }
        let (rank, text, next) = sections::word_at(section, offset)?;
        let sorted = words
            .last()
            .is_none_or(|(_, previous)| compare_keys(previous, text).is_lt());
        if !admissible(text) || usize::from(rank) >= layout.word_count || !sorted {
            return None;
        }
        words.push((rank, text));
        offset = next;
    }
    (offset == section.len()).then_some(words)
}

/// `Some(())` when every heavy entry is in `(lo, plen)` order, starts where its
/// run starts, names a run longer than three, and lists three distinct words
/// of that run in rank order.
fn heavy(bytes: &[u8], layout: &Layout, words: &[(u16, &[u8])]) -> Option<()> {
    let section = bytes.get(layout.heavy.clone())?;
    let mut previous = None;
    for index in 0..layout.heavy_count {
        let (lo, plen, top) = sections::heavy_at(section, index)?;
        let (lo, length) = (usize::from(lo), usize::from(plen));
        let prefix = words.get(lo)?.1.get(..length)?;
        let in_run = |id: usize| {
            words
                .get(id)
                .is_some_and(|(_, text)| has_prefix(text, prefix))
        };
        let starts_run = lo == 0 || !in_run(lo - 1);
        let ordered = previous.is_none_or(|previous| previous < (lo, length));
        if !(MIN_LEN..=MAX_LEN).contains(&length) || !starts_run || !in_run(lo + TOP_K) || !ordered
        {
            return None;
        }
        let ranked = top.map(|id| (words.get(usize::from(id)).map(|(rank, _)| *rank), id));
        let best = ranked.windows(2).all(|pair| pair[0] < pair[1]);
        if !best || !top.iter().all(|id| in_run(usize::from(*id))) {
            return None;
        }
        previous = Some((lo, length));
    }
    Some(())
}
