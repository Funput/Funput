//! Shortcut expansion at a word boundary, including case propagation when
//! `shortcut_smart_case` is on.

use crate::ImeResult;
use crate::model::{NumberGlue, Session};

/// The letter-casing pattern of typed keys, used to mirror the expansion's case
/// (`vn` → lowercase, `Vn` → Title Case, `VN` → UPPERCASE) — the same "propagate
/// case" convention used by common text expanders like Espanso. Only consulted while
/// `shortcut_smart_case` is on.
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

/// Re-case `expansion` to match `case`, using Unicode-aware `to_uppercase`/
/// `to_lowercase` so accented Vietnamese letters (`đ` → `Đ`, `ệ` → `Ệ`) come out right.
fn apply_shortcut_case(expansion: &str, case: ShortcutCase) -> String {
    match case {
        ShortcutCase::Lower => expansion.to_string(),
        ShortcutCase::Upper => expansion.to_uppercase(),
        ShortcutCase::Title => {
            let mut result = String::with_capacity(expansion.len());
            let mut start_of_word = true;
            for ch in expansion.chars() {
                if ch.is_whitespace() {
                    start_of_word = true;
                    result.push(ch);
                } else if start_of_word {
                    result.extend(ch.to_uppercase());
                    start_of_word = false;
                } else {
                    result.extend(ch.to_lowercase());
                }
            }
            result
        }
    }
}

pub(super) fn expansion(session: &Session, boundary_key: char) -> Option<ImeResult> {
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
    // A trigger is written however the user thinks of the word: as keys (`vn`,
    // `ddc`) or as the Vietnamese they compose to (`đc`, `mấy`). The keys go first so
    // a table written for them keeps its meaning, then the word on screen.
    let keys = &session.keys;
    let buffer = &session.buffer;
    let cased = lookup(session, keys)
        .or_else(|| (buffer != keys).then(|| lookup(session, buffer)).flatten())?;
    let output = format!("{cased}{boundary_key}");
    Some(ImeResult::send(session.buffer.chars().count(), output))
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
