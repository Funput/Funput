//! One entry of the journal or of an edit file: a word, and a tag byte saying what
//! happened to it.
//!
//! Every entry is `u16 length · u8 tag · bytes`. The tag grew out of the v2/v3
//! journal's flag byte, so its first two values keep that byte's meaning:
//!
//! | tag | entry                                   |
//! |-----|-----------------------------------------|
//! | 0   | learned, not following the entry before |
//! | 1   | learned, following the entry before     |
//! | 2–5 | edit: add, forget, block, unblock       |
//!
//! This file is the one place an [`EditKind`] meets a byte.

use std::io;

use crate::binary::{Cursor, invalid_data, put_u16};
use crate::dictionary::EditKind;

const LEARNED: u8 = 0;
const CHAINED: u8 = 1;
const ADD: u8 = 2;
const FORGET: u8 = 3;
const BLOCK: u8 = 4;
const UNBLOCK: u8 = 5;

/// What an entry has to go on to read its tag.
#[derive(Clone, Copy)]
pub(crate) enum TagFormat {
    /// A v1 journal: no tag at all, and nothing in it can be read as adjacent.
    Absent,
    /// A v2 or v3 journal: any non-zero byte means "followed the entry before".
    Flag,
    /// A v4 journal or an edit file: the table above, and nothing else.
    Tagged,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) enum Record {
    Learned { token: String, chained: bool },
    Edit { kind: EditKind, word: String },
}

impl Record {
    pub(crate) fn heap_bytes(&self) -> usize {
        match self {
            Self::Learned { token: text, .. } | Self::Edit { word: text, .. } => text.capacity(),
        }
    }

    pub(crate) fn encode(&self, out: &mut Vec<u8>) {
        let (tag, text) = match self {
            Self::Learned { token, chained } => (if *chained { CHAINED } else { LEARNED }, token),
            Self::Edit { kind, word } => (edit_tag(*kind), word),
        };
        put_u16(out, text.len() as u16);
        out.push(tag);
        out.extend_from_slice(text.as_bytes());
    }

    pub(crate) fn decode(cursor: &mut Cursor<'_>, format: TagFormat) -> io::Result<Self> {
        let len = cursor.u16()? as usize;
        let tag = match format {
            TagFormat::Absent => LEARNED,
            TagFormat::Flag => u8::from(cursor.u8()? != 0),
            TagFormat::Tagged => cursor.u8()?,
        };
        let text = std::str::from_utf8(cursor.take(len)?)
            .map_err(|_| invalid_data())?
            .to_owned();
        Ok(match tag {
            LEARNED | CHAINED => Self::Learned {
                token: text,
                chained: tag == CHAINED,
            },
            _ => Self::Edit {
                kind: edit_kind(tag).ok_or_else(invalid_data)?,
                word: text,
            },
        })
    }
}

fn edit_tag(kind: EditKind) -> u8 {
    match kind {
        EditKind::Add => ADD,
        EditKind::Forget => FORGET,
        EditKind::Block => BLOCK,
        EditKind::Unblock => UNBLOCK,
    }
}

fn edit_kind(tag: u8) -> Option<EditKind> {
    match tag {
        ADD => Some(EditKind::Add),
        FORGET => Some(EditKind::Forget),
        BLOCK => Some(EditKind::Block),
        UNBLOCK => Some(EditKind::Unblock),
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn round_trip(record: Record) {
        let mut bytes = Vec::new();
        record.encode(&mut bytes);
        let mut cursor = Cursor::new(&bytes);
        assert_eq!(
            Record::decode(&mut cursor, TagFormat::Tagged).ok(),
            Some(record)
        );
        assert!(cursor.is_empty());
    }

    #[test]
    fn every_kind_of_entry_survives_a_round_trip() {
        round_trip(Record::Learned {
            token: "xin".into(),
            chained: false,
        });
        round_trip(Record::Learned {
            token: "chào".into(),
            chained: true,
        });
        for kind in [
            EditKind::Add,
            EditKind::Forget,
            EditKind::Block,
            EditKind::Unblock,
        ] {
            round_trip(Record::Edit {
                kind,
                word: "đm".into(),
            });
        }
    }

    #[test]
    fn an_unknown_tag_is_damage_but_an_old_flag_is_not() {
        let bytes = [1, 0, 9, b'a'];
        assert!(Record::decode(&mut Cursor::new(&bytes), TagFormat::Tagged).is_err());
        let chained = Record::decode(&mut Cursor::new(&bytes), TagFormat::Flag).ok();
        assert_eq!(
            chained,
            Some(Record::Learned {
                token: "a".into(),
                chained: true
            })
        );
    }
}
