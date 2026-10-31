//! The `int` codes the dictionary calls trade in, the same values the C ABI uses,
//! and the flattening of a listing into the parallel arrays Kotlin reads.
//!
//! An unknown code from Kotlin is refused rather than guessed at: an edit becomes
//! `None` and is answered `INVALID`, a filter or sort makes the listing null.

use funput_suggestions::{
    DictionaryEntry, DictionaryFilter, DictionarySort, EditKind, EditOutcome, Origin,
};

pub(super) const APPLIED: i32 = 0;
pub(super) const UNCHANGED: i32 = 1;
pub(super) const INVALID: i32 = 2;
pub(super) const LIMIT_REACHED: i32 = 3;

pub(super) const ORIGIN_LEARNED: i64 = 0;
pub(super) const ORIGIN_ADDED: i64 = 1;

/// How many numbers each entry contributes to the details array.
pub(super) const DETAILS_PER_ENTRY: usize = 3;

pub(super) fn edit_kind(code: i32) -> Option<EditKind> {
    match code {
        0 => Some(EditKind::Add),
        1 => Some(EditKind::Forget),
        2 => Some(EditKind::Block),
        3 => Some(EditKind::Unblock),
        _ => None,
    }
}

pub(super) fn outcome(outcome: EditOutcome) -> i32 {
    match outcome {
        EditOutcome::Applied => APPLIED,
        EditOutcome::Unchanged => UNCHANGED,
        EditOutcome::Invalid => INVALID,
        EditOutcome::LimitReached => LIMIT_REACHED,
    }
}

pub(super) fn filter(code: i32) -> Option<DictionaryFilter> {
    match code {
        0 => Some(DictionaryFilter::All),
        1 => Some(DictionaryFilter::Learned),
        2 => Some(DictionaryFilter::Added),
        _ => None,
    }
}

pub(super) fn sort(code: i32) -> Option<DictionarySort> {
    match code {
        0 => Some(DictionarySort::MostUsed),
        1 => Some(DictionarySort::Recent),
        2 => Some(DictionarySort::Alphabetical),
        _ => None,
    }
}

/// A listing as two parallel arrays: the words, and for each one its uses, the
/// sequence it was last used at, and its origin.
pub(super) fn flatten(entries: &[DictionaryEntry<'_>]) -> (Vec<String>, Vec<i64>) {
    let words = entries.iter().map(|entry| entry.word.to_owned()).collect();
    let details = entries
        .iter()
        .flat_map(|entry| {
            let origin = match entry.origin {
                Origin::Learned => ORIGIN_LEARNED,
                Origin::Added => ORIGIN_ADDED,
            };
            let last_used = i64::try_from(entry.last_used).unwrap_or(i64::MAX);
            let row: [i64; DETAILS_PER_ENTRY] = [i64::from(entry.uses), last_used, origin];
            row
        })
        .collect();
    (words, details)
}
