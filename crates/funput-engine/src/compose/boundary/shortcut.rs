//! Shortcut expansion at a word boundary. Smart case lives in [`case`], the table in
//! [`table`] and its case-folded index in [`index`], the spellings a word is looked
//! up by in [`candidates`].

mod candidates;
mod case;
mod index;
mod table;

pub(crate) use table::ShortcutTable;

use crate::ImeResult;
use crate::model::Session;
use candidates::Found;

/// What the live word expands to, and how many characters on screen it replaces.
struct Rewrite {
    found: Found,
    backspace: usize,
}

/// `@` and `/` end a word but expand nothing: they sit inside addresses and paths
/// (`ct@gmail.com`, `ct/abc`), almost never right after a word in prose.
fn triggers_expansion(boundary_key: char) -> bool {
    !matches!(boundary_key, '@' | '/')
}

pub(super) fn expansion(session: &mut Session, boundary_key: char) -> Option<ImeResult> {
    if !triggers_expansion(boundary_key) {
        return None;
    }
    let Rewrite { found, backspace } = rewrite(session, Some(boundary_key))?;
    // Undo puts back what the app showed: the prefix the expansion swallowed, then
    // the word as the boundary would have left it.
    let word = super::shortcut_undo::plain_form(session);
    let word_at = found.prefix.len();
    let shown = if word_at == 0 {
        word
    } else {
        found.prefix + &word
    };
    let mut written = found.cased;
    if !found.suffixed {
        written.push(boundary_key);
    }
    super::shortcut_undo::arm(
        session,
        &found.core,
        &shown,
        word_at,
        &written,
        boundary_key,
    );
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
    let Rewrite { found, backspace } = rewrite(session, None)?;
    Some(ImeResult::send(backspace, found.cased))
}

fn rewrite(session: &Session, boundary: Option<char>) -> Option<Rewrite> {
    // Gated here rather than by clearing the table: the rows stay loaded, so the
    // switch is instant in both directions and nothing has to be re-pushed.
    if !session.config.shortcuts_enabled || session.shortcuts.is_empty() || session.keys.is_empty()
    {
        return None;
    }
    let found = candidates::find(session, boundary)?;
    // Undo restores the word, not the raw keys: `ddc` composes to `đc` before it
    // expands. What has to be deleted is what the app shows right now: `buffer`,
    // and the prefix in front of it (ASCII, so its length counts characters).
    let backspace = session.buffer.chars().count() + found.prefix.len();
    Some(Rewrite { found, backspace })
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
