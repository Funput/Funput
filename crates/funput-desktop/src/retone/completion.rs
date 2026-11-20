//! Whether the word at the caret opens an app's completion list.
//!
//! Chat and code apps complete a word that starts with a sigil from a list the
//! typing opens: `@` a person (Mattermost, Slack, Teams, GitHub), `#` a channel or
//! tag, `:` an emoji, `~` a Mattermost channel. Enter then picks from the list
//! instead of breaking a line (#712), so the shell must not read it as a line start,
//! and must not expand gõ tắt on a key the list is about to take.

use super::CommittedTail;

/// What a word starts with when an app completes it from a list.
const SIGILS: [char; 4] = ['@', '#', ':', '~'];

impl CommittedTail {
    /// Whether the word at the caret, its live part `composing` included, starts with
    /// a completion sigil. A sigil the engine took as a word boundary sits at the end
    /// of the shadow, the name typed after it in `composing`.
    ///
    /// Reads backwards to the last whitespace and allocates nothing: the hook calls
    /// this on Enter, Tab, Up and Down. A shadow cleared since the sigil was typed
    /// answers `false`, which leaves Enter a line start, as it was.
    pub fn opens_completion(&self, composing: &str) -> bool {
        composing
            .chars()
            .rev()
            .chain(self.tail.chars().rev())
            .take_while(|c| !c.is_whitespace())
            .last()
            .is_some_and(|first| SIGILS.contains(&first))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn tail(text: &str) -> CommittedTail {
        let mut tail = CommittedTail::new();
        tail.tail.push_str(text);
        tail
    }

    #[test]
    fn a_sigil_in_the_shadow_opens_the_word_composing_after_it() {
        assert!(tail("chào @").opens_completion("pcod"));
        assert!(tail("@").opens_completion("pc"));
        assert!(tail("ok :").opens_completion("smi"));
        assert!(tail("#").opens_completion("general"));
        assert!(tail("~").opens_completion("town"));
    }

    #[test]
    fn a_lone_sigil_already_opens_the_list() {
        assert!(tail("chào @").opens_completion(""));
    }

    #[test]
    fn a_sigil_inside_a_word_opens_nothing() {
        assert!(!tail("a@b.").opens_completion("com"));
        assert!(!tail("lưu ý:").opens_completion(""));
        assert!(!tail("(@").opens_completion("pc"));
    }

    #[test]
    fn only_the_word_at_the_caret_counts() {
        assert!(!tail("@pcod chào ").opens_completion("ban"));
        assert!(!tail("@pcod ").opens_completion(""));
    }

    #[test]
    fn a_cleared_shadow_knows_of_no_sigil() {
        assert!(!CommittedTail::new().opens_completion("pcod"));
        assert!(!CommittedTail::new().opens_completion(""));
    }
}
