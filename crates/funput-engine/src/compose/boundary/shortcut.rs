//! Shortcut expansion at a word boundary. Smart case lives in [`case`].

mod case;

use crate::ImeResult;
use crate::compose::doubled;
use crate::model::{NumberGlue, Session};

pub(super) fn expansion(session: &mut Session, boundary_key: char) -> Option<ImeResult> {
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
    let shown = super::shortcut_undo::plain_form(session);
    let mut written = cased;
    written.push(boundary_key);
    super::shortcut_undo::arm(session, &matched, &shown, &written, boundary_key);
    Some(ImeResult::send(backspace, written))
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
    let (form, case) = case::table_form(word, session.config.shortcut_smart_case);
    let expansion = session.shortcuts.get(form.as_ref())?;
    Some(match case {
        Some(case) => case::apply(expansion, case),
        None => expansion.clone(),
    })
}

/// Whether `word` begins a trigger (or is one), cased as a lookup would. Backspace
/// re-opens such a word though it is no syllable, so `ctss` + Space, ⌫⌫, `g` + Space
/// still reaches `ctsg`. Walks the whole table: never on the typing path.
pub(crate) fn starts_a_trigger(session: &Session, word: &str) -> bool {
    session.config.shortcuts_enabled && !word.is_empty() && {
        let (form, _) = case::table_form(word, session.config.shortcut_smart_case);
        session
            .shortcuts
            .keys()
            .any(|trigger| trigger.starts_with(&*form))
    }
}
