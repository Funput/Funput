//! The composition rules whose English collisions are listed, mirroring the
//! engine's `compose::collision::Collision`: the same variants, and each name is
//! the engine's data directory for it.

use funput_core::{InputMethod, telex_defers_w};

/// A composition rule that turns some English words into real syllables.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub(crate) enum Class {
    /// Plain Telex holds a `w` typed between the onset and the nucleus back for
    /// the vowel after it: `two` → `tơ`.
    DeferredW,
}

impl Class {
    pub(crate) const ALL: [Self; 1] = [Self::DeferredW];

    /// The class's directory under the engine's `data/collision/`.
    pub(crate) const fn name(self) -> &'static str {
        match self {
            Self::DeferredW => "deferred_w",
        }
    }

    /// The method the rule runs in; the scan types the word in no other.
    pub(crate) const fn method(self) -> InputMethod {
        match self {
            Self::DeferredW => InputMethod::Telex,
        }
    }

    /// Whether the rule shapes `word`'s keys at all — the same gate the engine
    /// applies before it searches the list.
    pub(crate) fn admits(self, word: &str) -> bool {
        match self {
            Self::DeferredW => telex_defers_w(word),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn deferred_w_admits_a_w_between_onset_and_nucleus() {
        assert!(Class::DeferredW.admits("two"));
        assert!(Class::DeferredW.admits("swan"));
        assert!(!Class::DeferredW.admits("tow"));
        assert!(!Class::DeferredW.admits("was"));
    }
}
