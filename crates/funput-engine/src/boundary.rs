//! Word-boundary handling — end-of-word clears composition state.

use crate::session::Session;

/// A word boundary ends the current composition: any whitespace or ASCII
/// punctuation. Digits are *not* boundaries — VNI uses `1`–`9` as modifiers.
///
/// v1 ignores non-ASCII punctuation (em dash, smart quotes, guillemets).
pub fn is_word_boundary(key: char) -> bool {
    key.is_whitespace() || key.is_ascii_punctuation()
}

/// End-of-word: reset composition state. E3 adds English restore before clear.
pub fn on_word_boundary(session: &mut Session) {
    session.clear();
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn word_boundary_chars() {
        // Whitespace and ASCII punctuation are boundaries.
        for c in [' ', '\n', '\t', ',', '.', '!', '?', ')', '-', '"'] {
            assert!(is_word_boundary(c), "{c:?} should be a boundary");
        }
        // Letters and digits are not (digits are VNI modifiers).
        for c in ['a', 'z', 'A', '1', '9'] {
            assert!(!is_word_boundary(c), "{c:?} should not be a boundary");
        }
    }

    #[test]
    fn on_word_boundary_clears_session() {
        let mut session = Session::new();
        session.buffer.push('á');
        session.keys.push_str("as");
        on_word_boundary(&mut session);
        assert!(session.buffer.is_empty());
        assert!(session.keys.is_empty());
    }
}
