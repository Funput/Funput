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
    /// The form that matched (`typed` keys, `keys`, or `buffer`). The next boundary
    /// that types it again keeps the word, once, then forgets the refusal.
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

/// Remember an expansion that just matched, before the boundary clears the word.
pub(super) fn arm(
    session: &mut Session,
    matched: &str,
    shown: &str,
    written: &str,
    boundary: char,
) {
    session.shortcut_undo.armed = Some(Armed {
        shown: shown.to_owned(),
        matched: matched.to_owned(),
        written: written.to_owned(),
        boundary,
    });
}

/// The next key spent the one-tap undo. A refusal already recorded stays.
pub(crate) fn lapse(session: &mut Session) {
    session.shortcut_undo.armed = None;
}

/// The caret left the field. Both the undo and the one-time refusal are stale.
pub(crate) fn dismiss(session: &mut Session) {
    session.shortcut_undo = ShortcutUndo::default();
}

pub(crate) fn is_armed(session: &Session) -> bool {
    session.shortcut_undo.armed.is_some()
}

pub(crate) fn shown(session: &Session) -> Option<&str> {
    session
        .shortcut_undo
        .armed
        .as_ref()
        .map(|armed| armed.shown.as_str())
}

pub(crate) fn written(session: &Session) -> Option<&str> {
    session
        .shortcut_undo
        .armed
        .as_ref()
        .map(|armed| armed.written.as_str())
}

/// Put the shown word back, with the same boundary character, and refuse the trigger once.
pub(crate) fn undo(session: &mut Session) -> Option<ImeResult> {
    let armed = session.shortcut_undo.armed.take()?;
    session.shortcut_undo.suppressed = Some(armed.matched);
    let mut output = armed.shown;
    output.push(armed.boundary);
    Some(ImeResult::send(armed.written.chars().count(), output))
}

/// The word that just ended is the trigger the user refused. Keep it, once.
///
/// Called before expansion, doubled-key restore, and typo correction, so a refused
/// `cm` is not repaired into some other syllable on the way past.
pub(super) fn declined(session: &mut Session) -> bool {
    let Some(suppressed) = session.shortcut_undo.suppressed.clone() else {
        return false;
    };
    if suppressed.is_empty() || !matches_word(session, &suppressed) {
        return false;
    }
    session.shortcut_undo.suppressed = None;
    true
}

fn matches_word(session: &Session, suppressed: &str) -> bool {
    doubled::typed_keys(session) == suppressed
        || session.keys == suppressed
        || session.buffer == suppressed
}
