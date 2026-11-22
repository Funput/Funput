//! The case-folded index behind [`super::ShortcutTable`]: how `tp` and `Tp` reach a
//! trigger stored as `TP`.

use std::collections::HashMap;

use super::case::{self, TriggerCase};

/// Lowercase form → the trigger stored in Title Case or UPPERCASE under it. Lowercase
/// triggers need no entry (a folded word looks them up directly), and mixed-case ones
/// (`iOS`) only ever match exactly. When two capitalized spellings fold together
/// (`Tp`, `TP`), the index holds the one nearer to lowercase, then the first in byte
/// order, so the choice never rests on hash order.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub(super) struct CaseIndex(HashMap<String, String>);

impl CaseIndex {
    /// Whether `trigger` is spelled in Title Case or UPPERCASE, the spellings indexed.
    pub(super) fn covers(trigger: &str) -> bool {
        case::stored_case(trigger).is_some_and(|case| case > TriggerCase::Lower)
    }

    pub(super) fn is_empty(&self) -> bool {
        self.0.is_empty()
    }

    /// The capitalized trigger held for the lowercase form `folded`.
    pub(super) fn get(&self, folded: &str) -> Option<&str> {
        self.0.get(folded).map(String::as_str)
    }

    /// Whether some indexed trigger's lowercase form begins with `form`.
    pub(super) fn has_key_starting_with(&self, form: &str) -> bool {
        self.0.keys().any(|folded| folded.starts_with(form))
    }

    /// Index a newly stored capitalized `trigger`, unless a preferred spelling of the
    /// same trigger already holds its place.
    pub(super) fn hold(&mut self, trigger: &str) {
        let folded = trigger.to_lowercase();
        if self
            .get(&folded)
            .is_none_or(|held| rank(trigger) < rank(held))
        {
            self.0.insert(folded, trigger.to_owned());
        }
    }

    /// Forget a removed `trigger`. If it held its place, the preferred spelling left
    /// among `stored` takes over: a scan, but only ever on an edit of the table.
    pub(super) fn release<'a>(&mut self, trigger: &str, stored: impl Iterator<Item = &'a String>) {
        let folded = trigger.to_lowercase();
        if self.get(&folded) != Some(trigger) {
            return;
        }
        let next = stored
            .filter(|other| Self::covers(other) && other.to_lowercase() == folded)
            .min_by(|a, b| rank(a).cmp(&rank(b)))
            .cloned();
        match next {
            Some(next) => self.0.insert(folded, next),
            None => self.0.remove(&folded),
        };
    }
}

/// The order capitalized spellings of one trigger are preferred in.
fn rank(trigger: &str) -> (Option<TriggerCase>, &str) {
    (case::stored_case(trigger), trigger)
}
