//! Shortcut expansion at a word boundary. Smart case lives in [`case`], the table in
//! [`table`] and its case-folded index in [`index`].

mod case;
mod index;
mod table;

pub(crate) use table::ShortcutTable;

use crate::ImeResult;
use crate::compose::doubled;
use crate::model::{NumberGlue, Session};

/// What the live word expands to: the trigger form that matched, the expansion
/// cased to match it, and how many characters on screen it replaces.
struct Rewrite {
    matched: String,
    cased: String,
    backspace: usize,
}

pub(super) fn expansion(session: &mut Session, boundary_key: char) -> Option<ImeResult> {
    let Rewrite {
        matched,
        cased,
        backspace,
    } = rewrite(session)?;
    let shown = super::shortcut_undo::plain_form(session);
    let mut written = cased;
    written.push(boundary_key);
    super::shortcut_undo::arm(session, &matched, &shown, &written, boundary_key);
    Some(ImeResult::send(backspace, written))
}

/// Expand the live word without typing a boundary character, for a key the host
/// sends on to the app itself (Enter, Tab). No undo is armed: by the time Backspace
/// could ask, the line is sent or the focus has moved on. A trigger the user just
/// took back stays as typed, the same as at Space.
pub(crate) fn expansion_in_place(session: &mut Session) -> Option<ImeResult> {
    if super::shortcut_undo::declined(session) {
        return None;
    }
    let Rewrite {
        cased, backspace, ..
    } = rewrite(session)?;
    Some(ImeResult::send(backspace, cased))
}

fn rewrite(session: &Session) -> Option<Rewrite> {
    // Gated here rather than by clearing the table: the rows stay loaded, so the
    // switch is instant in both directions and nothing has to be re-pushed. A word
    // glued to a number (`500k`) is a unit on that number, never a trigger.
    if !session.config.shortcuts_enabled
        || session.shortcuts.is_empty()
        || session.keys.is_empty()
        || session.glue == NumberGlue::Word
    {
        return None;
    }
    let (matched, cased) = matched_expansion(session)?;
    // Undo restores the word, not the raw keys: `ddc` composes to `đc` before it
    // expands. What has to be deleted is what the app shows right now: `buffer`.
    let backspace = session.buffer.chars().count();
    Some(Rewrite {
        matched,
        cased,
        backspace,
    })
}

/// The expansion and the form that matched it: typed keys, then `keys`, then buffer.
///
/// A trigger is written however the user thinks of the word: as keys (`vn`, `ddc`)
/// or as the Vietnamese they compose to (`đc`, `mấy`). Keys as typed come before
/// what a revert left of them, so `pass` reaches a `pass` trigger whether or not the
/// word was decided mid-word, and still reaches `pas`.
fn matched_expansion(session: &Session) -> Option<(String, String)> {
    let typed = doubled::typed_keys(session);
    let keys = session.keys.as_str();
    let buffer = session.buffer.as_str();
    let hit = |word: &str| lookup(session, word).map(|expansion| (word.to_owned(), expansion));
    hit(typed)
        .or_else(|| (keys != typed).then(|| hit(keys)).flatten())
        .or_else(|| {
            (buffer != keys && buffer != typed)
                .then(|| hit(buffer))
                .flatten()
        })
}

/// The expansion `word` triggers, re-cased to match it.
fn lookup(session: &Session, word: &str) -> Option<String> {
    let smart = session.config.shortcut_smart_case;
    session.shortcuts.find(word, smart).map(|hit| hit.cased())
}

/// Whether `word` begins a trigger (or is one), cased as a lookup would. Backspace
/// re-opens such a word though it is no syllable, so `ctss` + Space, ⌫⌫, `g` + Space
/// still reaches `ctsg`. Walks the whole table: never on the typing path.
///
/// A word that opens with a digit is refused: typed fresh it is a number with a unit
/// glued on (`500k`), which never expands, and re-opening must not change that. So is
/// one holding a word boundary, which no host offers as a single word.
pub(crate) fn starts_a_trigger(session: &Session, word: &str) -> bool {
    let opens_with_digit = word.starts_with(|c: char| c.is_ascii_digit());
    if !session.config.shortcuts_enabled
        || word.is_empty()
        || opens_with_digit
        || word.chars().any(super::is_english_boundary)
    {
        return false;
    }
    session
        .shortcuts
        .has_trigger_starting_with(word, session.config.shortcut_smart_case)
}
