//! Building the syllable index from the list: rank, tone style and the link between
//! the two spellings of a syllable the styles disagree on. Runs once, when the data
//! is turned on, so it may allocate freely.

use funput_core::{ToneStyle, respell_tone};

use super::{Entry, MODERN, NO_OTHER, SyllableIndex, TRADITIONAL};
use crate::index::normalize;

impl SyllableIndex {
    pub(super) fn build(listed: impl Iterator<Item = &'static str>) -> Self {
        let mut entries: Vec<(String, Entry)> = listed
            .enumerate()
            .map(|(rank, text)| {
                let styles = if respell_tone(text, ToneStyle::Modern).is_some() {
                    TRADITIONAL
                } else if respell_tone(text, ToneStyle::Traditional).is_some() {
                    MODERN
                } else {
                    MODERN | TRADITIONAL
                };
                let rank = u16::try_from(rank).unwrap_or(u16::MAX - 1);
                let entry = Entry {
                    text,
                    rank,
                    styles,
                    other: NO_OTHER,
                };
                (normalize::folded(text), entry)
            })
            .collect();
        entries.sort_by(|left, right| left.0.cmp(&right.0).then(left.1.rank.cmp(&right.1.rank)));
        entries.dedup_by(|later, earlier| later.1.text == earlier.1.text);
        let mut entries: Box<[Entry]> = entries.into_iter().map(|(_, entry)| entry).collect();
        link_spellings(&mut entries);
        Self { entries }
    }
}

/// Points each style-specific spelling at its counterpart.
fn link_spellings(entries: &mut [Entry]) {
    for index in 0..entries.len() {
        let style = match entries[index].styles {
            MODERN => ToneStyle::Traditional,
            TRADITIONAL => ToneStyle::Modern,
            _ => continue,
        };
        let Some(other) = respell_tone(entries[index].text, style) else {
            continue;
        };
        if let Some(at) = entries.iter().position(|entry| entry.text == other) {
            entries[index].other = u16::try_from(at).unwrap_or(NO_OTHER);
        }
    }
}
