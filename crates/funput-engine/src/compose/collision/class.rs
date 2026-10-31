//! The composition rules that collide with English, one variant per rule.
//!
//! A class pairs a rule's gate — the method it runs in and how to tell from the raw
//! keys that it shaped the word — with the English words it is known to turn into
//! real syllables. Adding one is a variant here, its data directory and the
//! matching class in `funput-lexicon-tool collision`, which must agree on the name.

use funput_core::{InputMethod, telex_defers_w};

use crate::compose::sorted_lines::SortedLines;
use crate::model::EngineConfig;

/// English words plain Telex composes into a syllable through a deferred `w`
/// (`two` → `tơ`, `swan` → `săn`). Lowercase, sorted.
static DEFERRED_W: SortedLines =
    SortedLines::new(include_str!("../../../data/collision/deferred_w/words.txt"));

/// A composition rule that turns some English words into real syllables.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Collision {
    /// Plain Telex holds a `w` typed between the onset and the nucleus back as the
    /// horn or breve of the vowel after it.
    DeferredW,
}

impl Collision {
    /// Every class, in the order the boundary asks them.
    pub(crate) const ALL: [Self; 1] = [Self::DeferredW];

    /// The class whose rule shaped `keys` under `config` and whose list holds them.
    ///
    /// Digits glued to the end are a number, not part of the word: `two2` is `two`.
    /// Keys seeded from the screen (Backspace, `adopt`) carry composed letters, which
    /// no list can hold, so such a word never matches.
    pub(crate) fn matching(config: &EngineConfig, keys: &str) -> Option<Self> {
        let word = keys.trim_end_matches(|key: char| key.is_ascii_digit());
        Self::ALL
            .into_iter()
            .find(|class| class.shaped(config, word) && class.words().contains(word))
    }

    /// The English words this rule is known to turn into syllables.
    pub(crate) fn words(self) -> SortedLines {
        match self {
            Self::DeferredW => DEFERRED_W,
        }
    }

    /// Whether this rule is the one that shaped `keys` under `config`. Cheap and
    /// allocation-free, so it runs before the list is searched.
    fn shaped(self, config: &EngineConfig, keys: &str) -> bool {
        match self {
            Self::DeferredW => config.method == InputMethod::Telex && telex_defers_w(keys),
        }
    }
}
