//! Word-boundary handling — end-of-word clears composition state.

mod shortcut;

use funput_core::{InputMethod, is_bare_shaped_vowel, is_complete_syllable};

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

/// Whether the keystrokes behind `buffer` can only have been meant as Vietnamese,
/// which outranks the structural verdict of [`is_complete_syllable`].
///
/// Three signals say so:
/// - a composed `đ`, unless a stray `w` shows the word is still mid-intent (`dwd`);
/// - any digit in the keys — in VNI the modifiers *are* digits, so a word that
///   used one was deliberately shaped;
/// - a word that is nothing but a shaped vowel (`aw` → `ă`, `aa` → `â`). Neither
///   is a complete syllable, since Vietnamese has no open `ă`/`â` rhyme, yet an
///   isolated one is exactly the letter the user asked for. Telex needs this
///   spelled out; VNI already lands here via its digit keys, and this keeps the
///   two methods answering `Aw`/`A8` the same way. An onset still means English:
///   `caw` and `law` compose to `că`/`lă` and are restored as before.
fn keystrokes_intend_vietnamese(session: &Session) -> bool {
    let unresolved_w =
        session.config.method.is_telex_family() && session.buffer.contains(['w', 'W']);
    (session.buffer.contains(['đ', 'Đ']) && !unresolved_w)
        || session.keys.contains(|c: char| c.is_ascii_digit())
        || is_bare_shaped_vowel(&session.buffer)
}

fn english_restore_result(session: &Session, boundary_key: char) -> ImeResult {
    let backspace = session.buffer.chars().count();
    let output = format!("{}{}", session.keys, boundary_key);
    ImeResult::send(backspace, output)
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
    let result = if let Some(expansion) = shortcut::expansion(session, boundary_key) {
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
