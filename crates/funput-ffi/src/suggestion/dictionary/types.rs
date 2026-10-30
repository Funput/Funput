//! The plain values the dictionary calls trade in, and their mapping onto the
//! library's enums. Codes are `u32` constants rather than C enums: a host can pass
//! any number, and an unknown one must be refused, not undefined behaviour.

use funput_suggestions::{
    DictionaryEntry, DictionaryFilter, DictionarySort, EditKind, EditOutcome, Origin,
};

use crate::abi;
use crate::suggestion::SUGGESTION_CHARS_CAP;

pub const FUNPUT_DICTIONARY_EDIT_ADD: u32 = 0;
pub const FUNPUT_DICTIONARY_EDIT_FORGET: u32 = 1;
pub const FUNPUT_DICTIONARY_EDIT_BLOCK: u32 = 2;
pub const FUNPUT_DICTIONARY_EDIT_UNBLOCK: u32 = 3;

pub const FUNPUT_DICTIONARY_APPLIED: u32 = 0;
pub const FUNPUT_DICTIONARY_UNCHANGED: u32 = 1;
/// Also the answer to a null handle, an unknown code, or text that is not UTF-32.
pub const FUNPUT_DICTIONARY_INVALID: u32 = 2;
pub const FUNPUT_DICTIONARY_LIMIT_REACHED: u32 = 3;

pub const FUNPUT_DICTIONARY_FILTER_ALL: u32 = 0;
pub const FUNPUT_DICTIONARY_FILTER_LEARNED: u32 = 1;
pub const FUNPUT_DICTIONARY_FILTER_ADDED: u32 = 2;

pub const FUNPUT_DICTIONARY_SORT_MOST_USED: u32 = 0;
pub const FUNPUT_DICTIONARY_SORT_RECENT: u32 = 1;
pub const FUNPUT_DICTIONARY_SORT_ALPHABETICAL: u32 = 2;

pub const FUNPUT_DICTIONARY_ORIGIN_LEARNED: u32 = 0;
pub const FUNPUT_DICTIONARY_ORIGIN_ADDED: u32 = 1;
/// Only in the list `funput_suggestion_blocked_list` returns.
pub const FUNPUT_DICTIONARY_ORIGIN_BLOCKED: u32 = 2;

/// One word of a dictionary list, by value.
#[repr(C)]
#[derive(Clone, Copy)]
pub struct FunputDictionaryEntry {
    pub count: u32,
    pub chars: [u32; SUGGESTION_CHARS_CAP],
    pub uses: u32,
    /// One of `FUNPUT_DICTIONARY_ORIGIN_*`.
    pub origin: u32,
    /// Comparable between entries of one store; not a time.
    pub last_used: u64,
}

impl FunputDictionaryEntry {
    pub(crate) fn from_entry(entry: &DictionaryEntry<'_>) -> Self {
        let origin = match entry.origin {
            Origin::Learned => FUNPUT_DICTIONARY_ORIGIN_LEARNED,
            Origin::Added => FUNPUT_DICTIONARY_ORIGIN_ADDED,
        };
        Self::with_text(entry.word, entry.uses, origin, entry.last_used)
    }

    pub(crate) fn blocked(word: &str) -> Self {
        Self::with_text(word, 0, FUNPUT_DICTIONARY_ORIGIN_BLOCKED, 0)
    }

    fn with_text(word: &str, uses: u32, origin: u32, last_used: u64) -> Self {
        let mut chars = [0; SUGGESTION_CHARS_CAP];
        let count = abi::copy_codepoints(&mut chars, word.chars()) as u32;
        Self {
            count,
            chars,
            uses,
            origin,
            last_used,
        }
    }
}

pub(crate) fn edit_kind(code: u32) -> Option<EditKind> {
    match code {
        FUNPUT_DICTIONARY_EDIT_ADD => Some(EditKind::Add),
        FUNPUT_DICTIONARY_EDIT_FORGET => Some(EditKind::Forget),
        FUNPUT_DICTIONARY_EDIT_BLOCK => Some(EditKind::Block),
        FUNPUT_DICTIONARY_EDIT_UNBLOCK => Some(EditKind::Unblock),
        _ => None,
    }
}

pub(crate) fn outcome_code(outcome: EditOutcome) -> u32 {
    match outcome {
        EditOutcome::Applied => FUNPUT_DICTIONARY_APPLIED,
        EditOutcome::Unchanged => FUNPUT_DICTIONARY_UNCHANGED,
        EditOutcome::Invalid => FUNPUT_DICTIONARY_INVALID,
        EditOutcome::LimitReached => FUNPUT_DICTIONARY_LIMIT_REACHED,
    }
}

pub(crate) fn filter(code: u32) -> Option<DictionaryFilter> {
    match code {
        FUNPUT_DICTIONARY_FILTER_ALL => Some(DictionaryFilter::All),
        FUNPUT_DICTIONARY_FILTER_LEARNED => Some(DictionaryFilter::Learned),
        FUNPUT_DICTIONARY_FILTER_ADDED => Some(DictionaryFilter::Added),
        _ => None,
    }
}

pub(crate) fn sort(code: u32) -> Option<DictionarySort> {
    match code {
        FUNPUT_DICTIONARY_SORT_MOST_USED => Some(DictionarySort::MostUsed),
        FUNPUT_DICTIONARY_SORT_RECENT => Some(DictionarySort::Recent),
        FUNPUT_DICTIONARY_SORT_ALPHABETICAL => Some(DictionarySort::Alphabetical),
        _ => None,
    }
}
