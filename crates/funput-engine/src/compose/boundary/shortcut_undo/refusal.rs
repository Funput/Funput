//! The trigger the user just took back, refused once.
//!
//! The refusal belongs to the word the undo put back. It answers the next word that
//! ends, and lapses as soon as Backspace edits that word: deleting it and typing the
//! trigger again is a new request, not the refused one (#742).

use crate::compose::doubled;
use crate::model::Session;

#[derive(Debug, Clone, PartialEq, Eq)]
pub(in crate::compose::boundary) struct Refusal {
    /// The form that matched (`typed` keys, `keys`, or `buffer`).
    matched: String,
    /// The word as the undo put it back on screen (`đc` for a `ddc` trigger).
    shown: String,
}

impl Refusal {
    pub(super) fn new(matched: String, shown: String) -> Self {
        Self { matched, shown }
    }

    fn matches_word(&self, session: &Session) -> bool {
        let matched = self.matched.as_str();
        !matched.is_empty()
            && (doubled::typed_keys(session) == matched
                || session.keys == matched
                || session.buffer == matched)
    }
}

/// Whether the word that just ended is the trigger the user refused, and so keeps
/// its screen form instead of expanding or being repaired.
///
/// The refusal is for the very next word, not for the trigger forever: the first
/// boundary that ends a real word spends it, match or not. Otherwise an `a` →
/// `anh` refused in one sentence would silently fail to expand in a later one.
/// An empty boundary (a second space) ends no word, so it leaves the refusal be.
/// English restore still runs for a refused word; only gõ tắt and typo correction
/// stand down.
pub(in crate::compose::boundary) fn declined(session: &mut Session) -> bool {
    if session.keys.is_empty() {
        return false;
    }
    spend(session).is_some_and(|refusal| refusal.matches_word(session))
}

/// Backspace took a character off the live word, so whatever ends next is being
/// retyped: the refusal lapses.
pub(crate) fn backspaced(session: &mut Session) {
    spend(session);
}

/// The host re-opened `word` behind the caret. The refused word itself, intact, keeps
/// its refusal (⌫ over the space, then Space, leaves it as typed); any other word
/// means Backspace already ate into it.
pub(crate) fn reopened(session: &mut Session, word: &str) {
    let refused = session
        .shortcut_undo
        .as_ref()
        .and_then(|undo| undo.suppressed.as_ref());
    if refused.is_some_and(|refusal| refusal.shown != word) {
        spend(session);
    }
}

/// Take the refusal out, freeing the undo slot once nothing else is held in it.
fn spend(session: &mut Session) -> Option<Refusal> {
    let undo = session.shortcut_undo.as_mut()?;
    let refusal = undo.suppressed.take()?;
    if undo.expansion.is_none() {
        session.shortcut_undo = None;
    }
    Some(refusal)
}
