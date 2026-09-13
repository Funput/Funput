//! Files the reader must refuse. Structural edits are resealed with a fresh CRC,
//! so they reach the structural checks instead of stopping at the checksum.

use std::io::{self, ErrorKind, Write};

use proptest::prelude::*;

use super::*;
use crate::binary::checksum;
use crate::lexicon::format::{MAX_FILE_BYTES, VERSION};
use crate::lexicon::{Lexicon, Stats, verify};

fn open(bytes: &[u8]) -> io::Result<Lexicon> {
    Lexicon::from_bytes(bytes.to_vec())
}

fn resealed(mut bytes: Vec<u8>) -> Vec<u8> {
    let sum = checksum(&bytes[HEADER_BYTES..]);
    bytes[20..24].copy_from_slice(&sum.to_le_bytes());
    bytes
}

fn refused(bytes: &[u8], kind: ErrorKind) {
    match open(bytes) {
        Ok(_) => panic!("accepted a damaged file"),
        Err(error) => assert_eq!(error.kind(), kind, "{error}"),
    }
}

/// File position of word `text`'s head.
fn word_position(bytes: &[u8], text: &str) -> usize {
    let header = Header::parse(bytes).unwrap();
    let words = decoded_words(bytes);
    words_start(&header) + words.iter().find(|word| word.2 == text).unwrap().0
}

fn heavy_position(bytes: &[u8], index: usize) -> usize {
    let header = Header::parse(bytes).unwrap();
    words_start(&header) + header.words_len as usize + index * HEAVY_BYTES
}

fn id_of(bytes: &[u8], text: &str) -> u16 {
    decoded_words(bytes)
        .iter()
        .position(|word| word.2 == text)
        .unwrap() as u16
}

#[test]
fn the_fixture_opens_with_the_layout_its_header_describes() {
    let bytes = fixture_bytes();
    let header = Header::parse(&bytes).unwrap();
    let lexicon = open(&bytes).unwrap();
    let layout = lexicon.layout();
    assert_eq!(
        (layout.word_count, layout.heavy_count),
        (60, header.heavy_count as usize)
    );
    assert_eq!(layout.heavy.end, bytes.len());
    assert!(!lexicon.is_mapped());
    assert!(
        open(&resealed(bytes)).is_ok(),
        "resealing alone must not break a file"
    );
}

#[test]
fn verify_reports_what_open_accepts_and_refuses_what_it_refuses() {
    let bytes = fixture_bytes();
    let header = Header::parse(&bytes).unwrap();
    let stats = verify(&bytes).unwrap();
    assert_eq!(
        stats,
        Stats {
            words: 60,
            heavy_prefixes: header.heavy_count as usize,
            bytes: bytes.len()
        }
    );
    assert!(verify(&bytes[..bytes.len() - 1]).is_err());
}

#[test]
fn every_truncation_is_refused() {
    let bytes = fixture_bytes();
    for len in 0..bytes.len() {
        assert!(
            open(&bytes[..len]).is_err(),
            "accepted the first {len} bytes"
        );
    }
}

#[test]
fn a_file_past_the_size_limit_is_refused_unread() {
    refused(
        &vec![0; MAX_FILE_BYTES as usize + 1],
        ErrorKind::InvalidData,
    );
}

#[test]
fn header_damage_is_refused_and_a_newer_version_says_so() {
    let bytes = fixture_bytes();
    let edited = |at: usize, value: &[u8]| {
        let mut copy = bytes.clone();
        copy[at..at + value.len()].copy_from_slice(value);
        copy
    };
    refused(&edited(0, b"FPLY"), ErrorKind::InvalidData);
    refused(&edited(4, &0u16.to_le_bytes()), ErrorKind::InvalidData);
    refused(
        &edited(4, &(VERSION + 1).to_le_bytes()),
        ErrorKind::Unsupported,
    );
    refused(&edited(6, &1u16.to_le_bytes()), ErrorKind::InvalidData);
    refused(&edited(8, &61u32.to_le_bytes()), ErrorKind::InvalidData);
    refused(&edited(16, &0u32.to_le_bytes()), ErrorKind::InvalidData);
    let crc = u32::from_le_bytes(bytes[20..24].try_into().unwrap());
    refused(
        &edited(20, &(crc ^ 1).to_le_bytes()),
        ErrorKind::InvalidData,
    );
}

#[test]
fn a_consistent_but_wrong_file_is_refused() {
    let bytes = fixture_bytes();
    let run = decoded_heavy(&bytes)
        .iter()
        .position(|entry| entry.1 == 2)
        .unwrap();
    let th = heavy_position(&bytes, run);
    type Edit = Box<dyn Fn(&mut Vec<u8>)>;
    let edits: Vec<(&str, Edit)> = vec![
        ("block offset", Box::new(|b| b[HEADER_BYTES + 4] += 1)),
        (
            "out of order",
            Box::new(|b| {
                let at = word_position(b, "about") + 3;
                b[at..at + 5].copy_from_slice(b"zzzzz")
            }),
        ),
        (
            "duplicate key",
            Box::new(|b| {
                let at = word_position(b, "are") + 3;
                b[at..at + 3].copy_from_slice(b"AND")
            }),
        ),
        (
            "not a letter",
            Box::new(|b| {
                let at = word_position(b, "the") + 3;
                b[at] = b'1'
            }),
        ),
        (
            "rank past the end",
            Box::new(|b| {
                let at = word_position(b, "the") + 1;
                b[at..at + 2].copy_from_slice(&60u16.to_le_bytes())
            }),
        ),
        (
            "empty word",
            Box::new(|b| {
                let at = word_position(b, "the");
                b[at] = 0
            }),
        ),
        (
            "heavy out of order",
            Box::new(|b| {
                let (x, y) = (heavy_position(b, 0), heavy_position(b, 1));
                let first: Vec<u8> = b[x..x + 9].to_vec();
                b.copy_within(y..y + 9, x);
                b[y..y + 9].copy_from_slice(&first)
            }),
        ),
        (
            "run starts earlier",
            Box::new(move |b| {
                let lo = u16::from_le_bytes([b[th], b[th + 1]]) + 1;
                b[th..th + 2].copy_from_slice(&lo.to_le_bytes())
            }),
        ),
        ("not heavy", Box::new(move |b| b[th + 2] = 5)),
        (
            "id past the end",
            Box::new(move |b| b[th + 3..th + 5].copy_from_slice(&60u16.to_le_bytes())),
        ),
        (
            "id outside the run",
            Box::new(move |b| {
                let id = id_of(b, "about");
                b[th + 3..th + 5].copy_from_slice(&id.to_le_bytes())
            }),
        ),
        (
            "top out of rank order",
            Box::new(move |b| {
                let first = [b[th + 3], b[th + 4]];
                b.copy_within(th + 5..th + 7, th + 3);
                b[th + 5..th + 7].copy_from_slice(&first)
            }),
        ),
    ];
    for (name, edit) in edits {
        let mut copy = bytes.clone();
        edit(&mut copy);
        assert!(
            open(&resealed(copy)).is_err(),
            "accepted a file with: {name}"
        );
    }
}

#[test]
fn open_maps_the_file_and_agrees_with_from_bytes() {
    let bytes = fixture_bytes();
    let mut file = tempfile::NamedTempFile::new().unwrap();
    file.write_all(&bytes).unwrap();
    let mapped = Lexicon::open(file.path()).unwrap();
    assert!(mapped.is_mapped());
    assert_eq!(mapped.layout(), open(&bytes).unwrap().layout());

    file.as_file().set_len(bytes.len() as u64 - 1).unwrap();
    assert!(Lexicon::open(file.path()).is_err());
    file.as_file().set_len(MAX_FILE_BYTES + 1).unwrap();
    assert!(Lexicon::open(file.path()).is_err());
    let missing = Lexicon::open(&file.path().with_extension("missing"));
    assert_eq!(
        missing.err().map(|error| error.kind()),
        Some(ErrorKind::NotFound)
    );
}

proptest! {
    /// Whatever a damaged-but-resealed file does, it is refused or read — never
    /// a panic.
    #[test]
    fn random_damage_never_panics(edits in prop::collection::vec((any::<prop::sample::Index>(), any::<u8>()), 1..8)) {
        let mut bytes = fixture_bytes();
        for (index, value) in edits {
            let at = HEADER_BYTES + index.index(bytes.len() - HEADER_BYTES);
            bytes[at] = value;
        }
        if let Ok(lexicon) = open(&resealed(bytes)) {
            for prefix in ["th", "the", "TH", "wh", "ab", "zz", "iphone"] {
                let _ = lexicon.top3(prefix);
            }
        }
    }
}
