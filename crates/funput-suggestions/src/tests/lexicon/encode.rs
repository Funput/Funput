use std::io::ErrorKind;

use super::*;
use crate::binary::checksum;
use crate::lexicon::format::{MAGIC, MAX_WORDS, VERSION};

#[test]
fn the_header_describes_the_body() {
    let bytes = fixture_bytes();
    let header = Header::parse(&bytes).unwrap();
    assert_eq!(&bytes[..4], MAGIC);
    assert_eq!(
        (header.version, header.flags, header.word_count),
        (VERSION, 0, 60)
    );
    assert!(header.heavy_count > 0);
    let heavy_len = header.heavy_count as usize * HEAVY_BYTES;
    assert_eq!(
        bytes.len(),
        words_start(&header) + header.words_len as usize + heavy_len
    );
    assert_eq!(header.body_crc, checksum(&bytes[HEADER_BYTES..]));
}

#[test]
fn words_are_sorted_by_lowercase_key_and_keep_their_case_and_rank() {
    let words = decoded_words(&fixture_bytes());
    let mut expected: Vec<(String, u16)> = fixture()
        .iter()
        .enumerate()
        .map(|(rank, word)| ((*word).to_owned(), rank as u16))
        .collect();
    expected.sort_by_key(|(word, _)| word.to_ascii_lowercase());
    let found: Vec<(String, u16)> = words
        .into_iter()
        .map(|(_, rank, text)| (text, rank))
        .collect();
    assert_eq!(found, expected);
}

#[test]
fn every_block_entry_points_at_its_sixteenth_word() {
    let bytes = fixture_bytes();
    let words = decoded_words(&bytes);
    for (block, chunk) in words.chunks(BLOCK).enumerate() {
        assert_eq!(
            u32_at(&bytes, HEADER_BYTES + block * 4) as usize,
            chunk[0].0
        );
    }
}

#[test]
fn heavy_holds_every_prefix_of_more_than_three_words_with_its_best_by_rank() {
    let bytes = fixture_bytes();
    let words = decoded_words(&bytes);
    let key = |id: usize| words[id].2.to_ascii_lowercase();
    let mut expected = Vec::new();
    for lo in 0..words.len() {
        for plen in 2..=words[lo].2.len() {
            let prefix = &key(lo)[..plen];
            if lo > 0 && key(lo - 1).starts_with(prefix) {
                continue; // not where this prefix's run begins
            }
            let mut group: Vec<usize> = (lo..words.len())
                .take_while(|id| key(*id).starts_with(prefix))
                .collect();
            if group.len() > 3 {
                group.sort_by_key(|id| (words[*id].1, *id));
                expected.push((
                    lo as u16,
                    plen as u8,
                    [0, 1, 2].map(|slot| group[slot] as u16),
                ));
            }
        }
    }
    expected.sort();
    assert!(
        expected.iter().any(|(_, plen, _)| *plen == 3),
        "the fixture needs a `the` run"
    );
    assert_eq!(decoded_heavy(&bytes), expected);
}

#[test]
fn line_order_comments_and_blank_lines_do_not_change_the_file() {
    let mut lines: Vec<String> = tsv(&fixture())
        .lines()
        .map(|line| format!("{line}\n"))
        .collect();
    lines.reverse();
    let noisy = format!("# Generated\n\n{}\n# end\n", lines.concat());
    assert_eq!(encode(&noisy).unwrap(), fixture_bytes());
    assert_eq!(encode(&tsv(&fixture())).unwrap(), fixture_bytes());
}

#[test]
fn an_empty_list_is_a_header_and_nothing_else() {
    let bytes = encode("# nothing\n").unwrap();
    assert_eq!(bytes.len(), HEADER_BYTES);
    assert_eq!(Header::parse(&bytes).unwrap().word_count, 0);
}

#[test]
fn refuses_what_the_reader_would_reject_and_says_which_line() {
    let too_long = format!("{}\t0\n", "x".repeat(33));
    for (input, reason) in [
        ("The\t0\nthe\t1\n", "are the same word"),
        (
            "go\t0\ndon't\t1\n",
            "line 2: a word is 2 to 32 ASCII letters",
        ),
        ("a\t0\n", "ASCII letters"),
        ("café\t0\n", "ASCII letters"),
        (too_long.as_str(), "ASCII letters"),
        ("word\n", "expected word<TAB>rank"),
        ("word\tsoon\n", "rank is not a number"),
        ("word\t70000\n", "rank is not a number"),
        ("word\t1\n", "past the end"),
    ] {
        let error = encode(input).unwrap_err();
        assert_eq!(error.kind(), ErrorKind::InvalidData);
        assert!(error.to_string().contains(reason), "{input:?}: {error}");
    }
}

#[test]
fn refuses_more_words_than_a_u16_id_can_name() {
    let name = |n: usize| {
        (0..4)
            .map(|place| (b'a' + (n / 26usize.pow(place) % 26) as u8) as char)
            .collect::<String>()
    };
    let list: String = (0..=MAX_WORDS)
        .map(|n| format!("{}\t0\n", name(n)))
        .collect();
    assert!(
        encode(&list)
            .unwrap_err()
            .to_string()
            .contains("more than 65535")
    );
}
