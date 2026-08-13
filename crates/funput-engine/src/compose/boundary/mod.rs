//! Word-boundary handling — end-of-word clears composition state.

use funput_core::{InputMethod, is_complete_syllable};

use crate::ImeResult;
use crate::compose::RestoreOverride;
use crate::model::Session;

pub(crate) fn is_word_boundary(method: InputMethod, key: char) -> bool {
    let full_telex_shortcut = method.is_advanced_telex() && matches!(key, '[' | ']');
    !full_telex_shortcut && (key.is_whitespace() || key.is_ascii_punctuation())
}

pub(crate) fn should_restore(session: &Session) -> bool {
    if session.restore_override == Some(RestoreOverride::ForceVietnamese) {
        return false;
    }
    session.config.smart_restore
        && !session.buffer.is_empty()
        && session.keys != session.buffer
        && !is_complete_syllable(&session.buffer)
        && !keystrokes_intend_vietnamese(session)
}

fn keystrokes_intend_vietnamese(session: &Session) -> bool {
    let unresolved_w =
        session.config.method.is_telex_family() && session.buffer.contains(['w', 'W']);
    (session.buffer.contains(['đ', 'Đ']) && !unresolved_w)
        || session.keys.contains(|c: char| c.is_ascii_digit())
}

fn english_restore_result(session: &Session, boundary_key: char) -> ImeResult {
    let backspace = session.buffer.chars().count();
    let output = format!("{}{}", session.keys, boundary_key);
    ImeResult::send(backspace, output)
}

/// The letter-casing pattern of typed keys, used to mirror the expansion's case
/// (`vn` → lowercase, `Vn` → Title Case, `VN` → UPPERCASE) — the same "propagate
/// case" convention used by common text expanders like Espanso.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum ShortcutCase {
    Lower,
    Upper,
    Title,
}

/// Classify the case pattern of `keys` from its cased characters (letters), ignoring
/// digits and punctuation. Returns `None` when the keys don't fit a clean pattern
/// (e.g. `vNa`), so the caller falls back to an exact, unmodified lookup — this keeps
/// deliberately mixed-case triggers (like `iOS`) working exactly as defined.
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

fn shortcut_expansion(session: &Session, boundary_key: char) -> Option<ImeResult> {
    // Gated here rather than by clearing the table: the rows stay loaded, so the
    // switch is instant in both directions and nothing has to be re-pushed.
    if !session.config.shortcuts_enabled || session.keys.is_empty() {
        return None;
    }
    let case = classify_case(&session.keys);
    let lookup_key = match case {
        Some(_) => session.keys.to_lowercase(),
        None => session.keys.clone(),
    };
    let expansion = session.shortcuts.get(&lookup_key)?;
    let cased = match case {
        Some(case) => apply_shortcut_case(expansion, case),
        None => expansion.clone(),
    };
    let output = format!("{cased}{boundary_key}");
    Some(ImeResult::send(session.buffer.chars().count(), output))
}

fn update_caps_on_boundary(session: &mut Session, key: char) {
    if !session.config.auto_capitalize {
        return;
    }
    match key {
        '.' | '!' | '?' => session.cap_sentence_ended = true,
        '\n' | '\r' => {
            session.cap_armed = true;
            session.cap_sentence_ended = false;
        }
        ' ' | '\t' if session.cap_sentence_ended => session.cap_armed = true,
        ' ' | '\t' | '"' | '\'' | '(' | ')' | '[' | ']' | '{' | '}' => {}
        _ => {
            session.cap_sentence_ended = false;
            session.cap_armed = false;
        }
    }
}

pub(crate) fn on_word_boundary(session: &mut Session, boundary_key: char) -> ImeResult {
    let result = if let Some(expansion) = shortcut_expansion(session, boundary_key) {
        expansion
    } else if should_restore(session) {
        english_restore_result(session, boundary_key)
    } else {
        ImeResult::none()
    };
    update_caps_on_boundary(session, boundary_key);
    session.clear();
    result
}

#[cfg(test)]
mod tests;
