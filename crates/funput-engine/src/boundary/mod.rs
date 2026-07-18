//! Word-boundary handling — end-of-word clears composition state.

use funput_core::{InputMethod, is_complete_syllable};

use crate::flip::RestoreOverride;
use crate::result::ImeResult;
use crate::session::Session;

pub(crate) fn is_word_boundary(key: char) -> bool {
    key.is_whitespace() || key.is_ascii_punctuation()
}

pub(crate) fn should_restore(session: &Session) -> bool {
    if session.restore_override == Some(RestoreOverride::ForceVietnamese) {
        return false;
    }
    session.smart_restore
        && !session.buffer.is_empty()
        && session.keys != session.buffer
        && !is_complete_syllable(&session.buffer)
        && !keystrokes_intend_vietnamese(session)
}

fn keystrokes_intend_vietnamese(session: &Session) -> bool {
    let unresolved_w = session.method == InputMethod::Telex && session.buffer.contains(['w', 'W']);
    (session.buffer.contains(['đ', 'Đ']) && !unresolved_w)
        || session.keys.contains(|c: char| c.is_ascii_digit())
}

fn english_restore_result(session: &Session, boundary_key: char) -> ImeResult {
    let backspace = session.buffer.chars().count();
    let output = format!("{}{}", session.keys, boundary_key);
    ImeResult::send(backspace, output)
}

fn shortcut_expansion(session: &Session, boundary_key: char) -> Option<ImeResult> {
    if session.keys.is_empty() {
        return None;
    }
    let expansion = session.shortcuts.get(&session.keys)?;
    let output = format!("{expansion}{boundary_key}");
    Some(ImeResult::send(session.buffer.chars().count(), output))
}

fn update_caps_on_boundary(session: &mut Session, key: char) {
    if !session.auto_capitalize {
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
