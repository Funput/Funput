//! Word-boundary handling — end-of-word clears composition state.

mod shortcut;

use funput_core::{InputMethod, is_bare_shaped_vowel, is_complete_syllable};

use crate::ImeResult;
use crate::compose::RestoreOverride;
use crate::correction;
use crate::model::Session;

/// What ends a word with no input method in play — English mode, where nothing
/// composes and so no key is spoken for.
pub(crate) fn is_english_boundary(key: char) -> bool {
    key.is_whitespace() || key.is_ascii_punctuation()
}

pub(crate) fn is_word_boundary(method: InputMethod, key: char) -> bool {
    let full_telex_shortcut = method.is_advanced_telex() && matches!(key, '[' | ']');
    !full_telex_shortcut && is_english_boundary(key)
}

/// The boundary's read of the word it is about to commit.
///
/// One struct because one `is_complete_syllable` call answers both questions, and
/// that call allocates: it builds the rhyme it validates. English restore and typo
/// correction each need the verdict, and asking twice would put a second allocation
/// on every word boundary.
pub(crate) struct Verdict {
    /// The buffer is a finished Vietnamese syllable.
    pub(crate) complete: bool,
    /// English restore wants this word back as its raw keystrokes.
    pub(crate) restore: bool,
}

pub(crate) fn judge(session: &Session) -> Verdict {
    let pinned = session.restore_override == Some(RestoreOverride::ForceVietnamese);
    let restore_candidate = !pinned
        && session.config.smart_restore
        && !session.buffer.is_empty()
        && session.keys != session.buffer;
    // With restore already refused and correction unable to act, nobody needs the
    // verdict — and the engine must not start paying for a syllable check it never
    // made before.
    if !restore_candidate && !correction::wants_verdict(session) {
        return Verdict {
            complete: false,
            restore: false,
        };
    }
    let complete = !session.buffer.is_empty() && is_complete_syllable(&session.buffer);
    Verdict {
        complete,
        restore: restore_candidate && !complete && !keystrokes_intend_vietnamese(session),
    }
}

/// The restore half of [`judge`], for tests that ask about it on its own.
#[cfg(test)]
pub(crate) fn should_restore(session: &Session) -> bool {
    judge(session).restore
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

/// English-mode word boundary: gõ tắt is all that is left to do. There is no
/// composition to restore (the keys are already the text on screen) and
/// auto-capitalize is a Vietnamese-mode feature, so neither runs here.
pub(crate) fn on_english_boundary(session: &mut Session, boundary_key: char) -> ImeResult {
    let result = shortcut::expansion(session, boundary_key).unwrap_or_else(ImeResult::none);
    session.clear();
    result
}

pub(crate) fn on_word_boundary(session: &mut Session, boundary_key: char) -> ImeResult {
    if let Some(expansion) = shortcut::expansion(session, boundary_key) {
        return finish(session, boundary_key, expansion);
    }
    let verdict = judge(session);
    // Typo correction gets the word before English restore does, and is deliberately
    // not hung off `restore`: the eager restore in `pipeline` has usually already
    // rewritten the buffer to the raw keys by now, which is what makes `restore`
    // false for precisely the mistyped words correction exists for. What it parks is
    // answered by the platform on the next call; this keystroke behaves as it always
    // has, so nothing here can change the text the app already shows.
    if correction::offer(session, boundary_key, &verdict) {
        return finish(session, boundary_key, ImeResult::none());
    }
    let result = if verdict.restore {
        english_restore_result(session, boundary_key)
    } else {
        ImeResult::none()
    };
    finish(session, boundary_key, result)
}

fn finish(session: &mut Session, boundary_key: char, result: ImeResult) -> ImeResult {
    update_caps_on_boundary(session, boundary_key);
    session.clear();
    result
}

#[cfg(test)]
mod tests;
