//! The composition rules that collide with English, one variant per rule.
//!
//! A class pairs a rule's gate — the method it runs in and how to tell from the raw
//! keys that it shaped the word — with the English words it is known to turn into
//! real syllables. This is the one definition: `funput-lexicon-tool collision`
//! reads the same gate and name to generate each list, so adding a rule is a
//! variant here plus its data directory.

use funput_core::{InputMethod, telex_defers_w};

use crate::compose::sorted_lines::SortedLines;
use crate::model::EngineConfig;

/// English words plain Telex composes into a syllable through a deferred `w`
/// (`two` → `tơ`, `swan` → `săn`). Lowercase, sorted.
static DEFERRED_W: SortedLines =
    SortedLines::new(include_str!("../../../data/collision/deferred_w/words.txt"));

/// A composition rule that turns some English words into real Vietnamese syllables,
/// which collision restore gives back at the word boundary
/// ([`EngineConfig::restore_collisions`]).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Collision {
    /// Plain Telex holds a `w` typed between the onset and the nucleus back as the
    /// horn or breve of the vowel after it: `two` → `tơ`.
    DeferredW,
}

impl Collision {
    /// Every class, in the order the boundary asks them.
    pub const ALL: [Self; 1] = [Self::DeferredW];

    /// The class's directory under `data/collision/`, which holds its `words.txt`
    /// and `overrides.txt`.
    pub const fn name(self) -> &'static str {
        match self {
            Self::DeferredW => "deferred_w",
        }
    }

    /// The method the rule runs in. No other method ever restores its words.
    pub const fn method(self) -> InputMethod {
        match self {
            Self::DeferredW => InputMethod::Telex,
        }
    }

    /// Whether the rule shapes `keys` at all, the method aside. Cheap and
    /// allocation-free, so the boundary asks it before searching the list.
    pub fn shapes(self, keys: &str) -> bool {
        match self {
            Self::DeferredW => telex_defers_w(keys),
        }
    }

    /// The class whose rule shaped `keys` under `config` and whose list holds them.
    ///
    /// Digits glued to the end are a number, not part of the word: `two2` is `two`.
    /// Keys seeded from the screen (Backspace, `adopt`) carry composed letters, which
    /// no list can hold, so such a word never matches.
    pub(crate) fn matching(config: &EngineConfig, keys: &str) -> Option<Self> {
        let word = keys.trim_end_matches(|key: char| key.is_ascii_digit());
        Self::ALL.into_iter().find(|class| {
            config.method == class.method() && class.shapes(word) && class.words().contains(word)
        })
    }

    /// The English words this rule is known to turn into syllables.
    pub(super) fn words(self) -> SortedLines {
        match self {
            Self::DeferredW => DEFERRED_W,
        }
    }
}
