//! Shortcut expansion at a word boundary, including case propagation when
//! `shortcut_smart_case` is on.

use crate::ImeResult;
use crate::compose::doubled;
use crate::model::{NumberGlue, Session};

/// The letter-casing pattern of typed keys, used to raise the expansion's case to
/// match (`vn` → as stored, `Vn` → first letter capitalized, `VN` → UPPERCASE), the
/// way UniKey does. Case is only ever raised, never lowered: what the user stored in
/// the table is the floor. Only consulted while `shortcut_smart_case` is on.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum ShortcutCase {
    Lower,
    Upper,
    Title,
}

/// Classify the case pattern of `keys` from its cased characters (letters), ignoring
/// digits and punctuation. Returns `None` when the keys don't fit a clean pattern
/// (e.g. `vNa`), so the caller falls back to an exact, unmodified lookup — this keeps
/// deliberately mixed-case triggers (like `iOS`) working exactly as defined. That same
/// `None` path is what the smart-case switch reuses when it is off.
fn classify_case(keys: &str) -> Option<ShortcutCase> {
    let mut letters = keys.chars().filter(|c| c.is_alphabetic());
    let first = letters.next()?;
    let rest: Vec<char> = letters.collect();
    if first.is_lowercase() {
        rest.iter()
            .all(|c| c.is_lowercase())
            .then_some(ShortcutCase::Lower)
    } else if !rest.is_empty() && rest.iter().all(|c| c.is_uppercase()) {
        Some(ShortcutCase::Upper)
    } else if rest.iter().all(|c| c.is_lowercase()) {
        // A single uppercase letter (`rest` empty) also lands here: capitalizing one
        // word reads more naturally than shouting the whole expansion.
        Some(ShortcutCase::Title)
    } else {
        None
    }
}

/// Raise the case of `expansion` to match `case`. Nothing is ever lowered: a stored
/// `TP. HCM` stays `TP. HCM` under `Tp`. Unicode-aware `to_uppercase` keeps accented
/// Vietnamese letters right (`đ` → `Đ`, `ệ` → `Ệ`).
fn apply_shortcut_case(expansion: &str, case: ShortcutCase) -> String {
    match case {
        ShortcutCase::Lower => expansion.to_string(),
        ShortcutCase::Upper => expansion.to_uppercase(),
        ShortcutCase::Title => capitalize_first_letter(expansion),
    }
}

/// Uppercase the first letter of `expansion`, leaving every other char as stored.
/// Leading non-alphanumerics (brackets, quotes, whitespace) are skipped over; an
/// expansion whose first alphanumeric is a digit (`3 con mèo`) comes back unchanged.
fn capitalize_first_letter(expansion: &str) -> String {
    let Some((at, letter)) = expansion
        .char_indices()
        .find(|(_, c)| c.is_alphanumeric())
        .filter(|(_, c)| c.is_alphabetic())
    else {
        return expansion.to_string();
    };
    let mut result = String::with_capacity(expansion.len());
    result.push_str(&expansion[..at]);
    result.extend(letter.to_uppercase());
    result.push_str(&expansion[at + letter.len_utf8()..]);
    result
}

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
    // Off, every trigger takes the exact-match path below: `tp` expands, `Tp`/`TP` do
    // not, and the expansion is never re-cased. Reusing the mixed-case fallback rather
    // than adding a second lookup keeps one description of "no case propagation".
    let case = if session.config.shortcut_smart_case {
        classify_case(word)
    } else {
        None
    };
    // A lowercase word already is its own key; only re-cased words pay for a copy.
    let expansion = match case {
        Some(ShortcutCase::Lower) | None => session.shortcuts.get(word)?,
        Some(_) => session.shortcuts.get(&word.to_lowercase())?,
    };
    Some(match case {
        Some(case) => apply_shortcut_case(expansion, case),
        None => expansion.clone(),
    })
}
