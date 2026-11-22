//! One Backspace undoes the gõ tắt expansion that just landed.
//!
//! The beat lives until the next key. The expansion itself is remembered past it, so
//! a host that sees the caret back right after it — the user typed on, then deleted
//! back — can arm the beat again ([`rearm`]). Taking it puts the word back as it was
//! shown, boundary character included, and refuses that same trigger once: see
//! [`refusal`]. [`Session::clear`] at the boundary keeps all of it: the platform
//! answers after the word is already gone.

mod refusal;

use crate::ImeResult;
use crate::compose::doubled;
use crate::model::Session;
use refusal::Refusal;

pub(super) use refusal::declined;
pub(crate) use refusal::{backspaced, reopened};

/// The last expansion, whether Backspace may take it now, and a trigger the user
/// already refused.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub(crate) struct ShortcutUndo {
    expansion: Option<Expansion>,
    /// Backspace may take [`Self::expansion`] back: set by the expansion and by
    /// [`rearm`], cleared by the next key.
    armed: bool,
    /// The trigger taken back. The next word that ends is kept as typed if it is this
    /// one; either way the refusal is then spent, and Backspace into the word spends it
    /// sooner.
    suppressed: Option<Refusal>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
struct Expansion {
    /// Text on screen before the expansion: the trigger's symbol prefix, if any,
    /// then the word. Backspace puts this back.
    shown: String,
    /// Where the word starts in `shown`, past the prefix.
    word_at: usize,
    /// The word's form that matched, without the symbols around it.
    matched: String,
    /// The expansion, plus the boundary character unless the trigger ends with it
    /// (`ct;`). Backspace deletes this.
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
    word_at: usize,
    written: &str,
    boundary: char,
) {
    let undo = slot(session);
    undo.expansion = Some(Expansion {
        shown: shown.to_owned(),
        word_at,
        matched: matched.to_owned(),
        written: written.to_owned(),
        boundary,
    });
    undo.armed = true;
}

/// The next key spent the one-tap undo. The expansion stays remembered for
/// [`rearm`], and a refusal already recorded stays until the next word ends: see
/// [`declined`].
pub(crate) fn lapse(session: &mut Session) {
    if let Some(undo) = session.shortcut_undo.as_mut() {
        undo.armed = false;
    }
}

/// The host saw the caret back right after the last expansion: arm the undo again.
/// Nothing to arm while a word is being composed, or once the expansion is gone.
pub(crate) fn rearm(session: &mut Session) -> bool {
    if !session.buffer.is_empty() {
        return false;
    }
    let Some(undo) = session.shortcut_undo.as_mut() else {
        return false;
    };
    undo.armed = undo.expansion.is_some();
    undo.armed
}

/// The caret left the field. The expansion, its undo and the one-time refusal are
/// all stale.
pub(crate) fn dismiss(session: &mut Session) {
    session.shortcut_undo = None;
}

fn armed(session: &Session) -> Option<&Expansion> {
    let undo = session.shortcut_undo.as_ref()?;
    undo.expansion.as_ref().filter(|_| undo.armed)
}

pub(crate) fn is_armed(session: &Session) -> bool {
    armed(session).is_some()
}

pub(crate) fn shown(session: &Session) -> Option<&str> {
    armed(session).map(|expansion| expansion.shown.as_str())
}

pub(crate) fn written(session: &Session) -> Option<&str> {
    armed(session).map(|expansion| expansion.written.as_str())
}

/// Put the shown word back, with the same boundary character, and refuse the trigger once.
pub(crate) fn undo(session: &mut Session) -> Option<ImeResult> {
    let undo = session.shortcut_undo.as_mut().filter(|undo| undo.armed)?;
    let expansion = undo.expansion.take()?;
    undo.armed = false;
    // The refusal is for the word: a host re-opens `ct`, never the `;` before it.
    let word = expansion.shown[expansion.word_at..].to_owned();
    undo.suppressed = Some(Refusal::new(expansion.matched, word));
    let mut output = expansion.shown;
    output.push(expansion.boundary);
    Some(ImeResult::send(expansion.written.chars().count(), output))
}
