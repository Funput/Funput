//! One Backspace undoes the gõ tắt expansion that just landed.
//!
//! The beat lives until the next key. Taking it puts the word back as it was shown,
//! boundary character included, and refuses that same trigger once. [`Session::clear`]
//! at the boundary keeps both: the platform answers after the word is already gone.

use crate::ImeResult;
use crate::compose::doubled;
use crate::model::Session;

/// Armed undo for the expansion on screen, plus a trigger the user already refused.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub(crate) struct ShortcutUndo {
    armed: Option<Armed>,
    /// The form that matched (`typed` keys, `keys`, or `buffer`). The next word that
    /// ends is kept as typed if it is this one; either way the refusal is then spent.
    suppressed: Option<String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
struct Armed {
    /// Word on screen before the expansion. Backspace puts this back.
    shown: String,
    matched: String,
    /// Expansion plus the boundary character. Backspace deletes this.
    written: String,
    boundary: char,
}

/// The word as the boundary would have left it with no expansion: what undo puts
/// back, and what the `↩` chip names. Usually the composed word (`đc`), but a word
/// English restore would turn back into keys (`text`, not `tẽt`) comes back as
/// those keys, exactly as a refused retype does.
pub(super) fn plain_form(session: &Session) -> String {
    if doubled::restorable(session) {
        session.literal.as_str().to_owned()
    } else if super::judge(session).restore {
        session.keys.clone()
    } else {
        session.buffer.clone()
    }
}

fn slot(session: &mut Session) -> &mut ShortcutUndo {
    session
        .shortcut_undo
        .get_or_insert_with(|| Box::new(ShortcutUndo::default()))
}

/// Remember an expansion that just matched, before the boundary clears the word.
pub(super) fn arm(
    session: &mut Session,
    matched: &str,
    shown: &str,
    written: &str,
    boundary: char,
) {
    slot(session).armed = Some(Armed {
        shown: shown.to_owned(),
        matched: matched.to_owned(),
        written: written.to_owned(),
        boundary,
    });
}

/// The next key spent the one-tap undo. A refusal already recorded stays until the
/// next word ends: see [`declined`].
pub(crate) fn lapse(session: &mut Session) {
    let Some(undo) = session.shortcut_undo.as_mut() else {
        return;
    };
    undo.armed = None;
    if undo.suppressed.is_none() {
        session.shortcut_undo = None;
    }
}

/// The caret left the field. Both the undo and the one-time refusal are stale.
pub(crate) fn dismiss(session: &mut Session) {
    session.shortcut_undo = None;
}

pub(crate) fn is_armed(session: &Session) -> bool {
    session
        .shortcut_undo
        .as_ref()
        .is_some_and(|undo| undo.armed.is_some())
}

pub(crate) fn shown(session: &Session) -> Option<&str> {
    session
        .shortcut_undo
        .as_ref()
        .and_then(|undo| undo.armed.as_ref().map(|armed| armed.shown.as_str()))
}

pub(crate) fn written(session: &Session) -> Option<&str> {
    session
        .shortcut_undo
        .as_ref()
        .and_then(|undo| undo.armed.as_ref().map(|armed| armed.written.as_str()))
}

/// Put the shown word back, with the same boundary character, and refuse the trigger once.
pub(crate) fn undo(session: &mut Session) -> Option<ImeResult> {
    let undo = session.shortcut_undo.as_mut()?;
    let armed = undo.armed.take()?;
    undo.suppressed = Some(armed.matched);
    let mut output = armed.shown;
    output.push(armed.boundary);
    Some(ImeResult::send(armed.written.chars().count(), output))
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
pub(super) fn declined(session: &mut Session) -> bool {
    if session.keys.is_empty() {
        return false;
    }
    let Some(undo) = session.shortcut_undo.as_mut() else {
        return false;
    };
    let Some(suppressed) = undo.suppressed.take() else {
        return false;
    };
    if undo.armed.is_none() {
        session.shortcut_undo = None;
    }
    !suppressed.is_empty() && matches_word(session, &suppressed)
}

fn matches_word(session: &Session, suppressed: &str) -> bool {
    doubled::typed_keys(session) == suppressed
        || session.keys == suppressed
        || session.buffer == suppressed
}
