//! The keys the user really typed for the live word, kept once a revert starts
//! dropping one of them from `Session::keys`.

/// Room reserved up front, so the revert that starts a word's literal never
/// allocates on the keystroke path. Every listed word fits: the lexicon tool admits
/// nothing longer (its `MAX_LEN`), and a unit test holds both lists to it.
pub(crate) const CAPACITY: usize = 32;

/// Every key typed since the word boundary, including the ones a revert swallowed
/// (`pass` while `keys` holds `pas`).
///
/// Inactive (empty) until the word's first revert, so a word without one never
/// touches it: [`Literal::follow`] does nothing until [`Literal::start`] has run.
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct Literal(String);

impl Literal {
    pub(crate) fn new() -> Self {
        Self(String::with_capacity(CAPACITY))
    }

    /// Whether the word has had a revert, and so is being followed.
    pub(crate) fn is_active(&self) -> bool {
        !self.0.is_empty()
    }

    pub(crate) fn as_str(&self) -> &str {
        &self.0
    }

    /// Start following the word from `keys`, which still hold the key the revert
    /// is about to collapse. A later revert in the same word finds it already
    /// following and leaves it alone.
    pub(crate) fn start(&mut self, keys: &str) {
        if !self.is_active() {
            self.0.push_str(keys);
        }
    }

    /// Record the key that just joined `keys`, while the word is being followed.
    pub(crate) fn follow(&mut self, key: char) {
        if self.is_active() {
            self.0.push(key);
        }
    }

    /// Drop the word, keeping the capacity for the next one.
    pub(crate) fn reset(&mut self) {
        self.0.clear();
    }
}

impl Default for Literal {
    fn default() -> Self {
        Self::new()
    }
}
