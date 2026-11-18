//! The gõ tắt table: what the user stored, and how a typed word reaches it.

use std::collections::HashMap;

use super::case::{self, ShortcutCase};
use super::index::CaseIndex;

/// Trigger → expansion exactly as the user stored them, plus the [`CaseIndex`] that
/// lets `tp` and `Tp` reach `TP` without walking the table at every word boundary.
/// The index is kept up to date here, off the typing path. Boxed and `None` until a
/// capitalized trigger is stored, so a table without one costs the engine a pointer.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub(crate) struct ShortcutTable {
    entries: HashMap<String, String>,
    raised: Option<Box<CaseIndex>>,
}

/// A trigger found for a typed word: its expansion as stored, and the case to raise
/// it to when the word was typed in a higher case than the trigger is stored in.
pub(super) struct Hit<'a> {
    expansion: &'a str,
    raise: Option<ShortcutCase>,
}

impl Hit<'_> {
    pub(super) fn cased(&self) -> String {
        match self.raise {
            Some(case) => case::apply(self.expansion, case),
            None => self.expansion.to_owned(),
        }
    }
}

impl ShortcutTable {
    pub(crate) fn entries(&self) -> &HashMap<String, String> {
        &self.entries
    }

    pub(crate) fn is_empty(&self) -> bool {
        self.entries.is_empty()
    }

    /// Store `trigger`, replacing any expansion it had.
    pub(crate) fn insert(&mut self, trigger: String, expansion: String) {
        if CaseIndex::covers(&trigger) {
            self.raised.get_or_insert_default().hold(&trigger);
        }
        self.entries.insert(trigger, expansion);
    }

    pub(crate) fn remove(&mut self, trigger: &str) {
        if self.entries.remove(trigger).is_none() || !CaseIndex::covers(trigger) {
            return;
        }
        if let Some(index) = self.raised.as_mut() {
            index.release(trigger, self.entries.keys());
            if index.is_empty() {
                self.raised = None;
            }
        }
    }

    pub(crate) fn clear(&mut self) {
        self.entries.clear();
        self.raised = None;
    }

    /// The trigger `word` reaches. The exact spelling comes first, so a table holding
    /// both `tp` and `TP` answers each as typed. With smart case on, a cleanly cased
    /// word then tries its lowercase form, then a trigger stored in capitals.
    pub(super) fn find(&self, word: &str, smart: bool) -> Option<Hit<'_>> {
        if let Some(expansion) = self.entries.get(word) {
            return Some(Hit {
                expansion,
                raise: None,
            });
        }
        let (folded, typed) = case::table_form(word, smart);
        let typed = typed?;
        if typed > ShortcutCase::Lower
            && let Some(expansion) = self.entries.get(folded.as_ref())
        {
            return Some(Hit {
                expansion,
                raise: Some(typed),
            });
        }
        let trigger = self.indexed(&folded)?;
        Some(Hit {
            expansion: self.entries.get(trigger)?,
            raise: case::raise(typed, case::classify_case(trigger)?),
        })
    }

    /// Whether some trigger `word` could reach begins with it, cased as [`Self::find`]
    /// would match. Walks the table without allocating.
    pub(super) fn has_trigger_starting_with(&self, word: &str, smart: bool) -> bool {
        let (form, case) = case::table_form(word, smart);
        let indexed = || {
            self.raised
                .as_ref()
                .is_some_and(|index| index.has_key_starting_with(&form))
        };
        self.entries
            .keys()
            .any(|trigger| trigger.starts_with(&*form))
            || (case.is_some() && indexed())
    }

    fn indexed(&self, folded: &str) -> Option<&str> {
        self.raised.as_deref()?.get(folded)
    }
}

#[cfg(test)]
mod tests {
    use super::ShortcutTable;

    fn table(triggers: &[&str]) -> ShortcutTable {
        let mut table = ShortcutTable::default();
        for trigger in triggers {
            table.insert((*trigger).into(), format!("<{trigger}>"));
        }
        table
    }

    fn reached(table: &ShortcutTable, word: &str) -> Option<String> {
        table.find(word, true).map(|hit| hit.cased())
    }

    #[test]
    fn a_capitalized_trigger_is_reached_from_every_clean_case() {
        let table = table(&["TP"]);
        for word in ["TP", "Tp", "tp"] {
            assert_eq!(reached(&table, word).as_deref(), Some("<TP>"), "{word}");
        }
        assert_eq!(reached(&table, "tP"), None);
    }

    #[test]
    fn the_index_prefers_title_case_whatever_the_insertion_order() {
        for order in [["Tp", "TP"], ["TP", "Tp"]] {
            assert_eq!(reached(&table(&order), "tp").as_deref(), Some("<Tp>"));
        }
    }

    #[test]
    fn removing_the_indexed_spelling_hands_over_to_the_next() {
        let mut table = table(&["Tp", "TP"]);
        table.remove("Tp");
        assert_eq!(reached(&table, "tp").as_deref(), Some("<TP>"));
        table.remove("TP");
        assert_eq!(reached(&table, "tp"), None);
        assert!(table.raised.is_none());
    }

    #[test]
    fn removing_a_spelling_the_index_does_not_hold_keeps_the_index() {
        let mut table = table(&["Tp", "TP"]);
        table.remove("TP");
        assert_eq!(table.indexed("tp"), Some("Tp"));
        assert_eq!(reached(&table, "tp").as_deref(), Some("<Tp>"));
    }

    #[test]
    fn clear_empties_the_index_too() {
        let mut table = table(&["TP"]);
        table.clear();
        assert!(table.is_empty());
        assert_eq!(reached(&table, "tp"), None);
    }

    #[test]
    fn smart_case_off_matches_only_the_exact_spelling() {
        let table = table(&["TP"]);
        assert!(table.find("TP", false).is_some());
        assert!(table.find("tp", false).is_none());
    }
}
