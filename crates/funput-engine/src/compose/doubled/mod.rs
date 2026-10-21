//! Doubled-key restore — give a word back the key a revert swallowed.
//!
//! Pressing a tone key again reverts it (`pá` + `s` → `pas`), and the revert drops
//! the second key from `keys`, so `password` used to come out `pasword`. The habit
//! the revert serves stays as it is: `tesst` → `test`, `mixx` → `mix`. What changes
//! is that the keys are kept in [`Session::literal`], and when they spell an English
//! word a revert would otherwise shorten (`pass`, `password`), the word boundary
//! writes them back. Design: `docs/features/doubled-key-restore.md`.
//!
//! A word with no revert never fills `literal`, which is the whole safety argument:
//! everything below starts from a non-empty `literal`, so Vietnamese — and every
//! other word — composes exactly as it did before the feature existed.
//!
//! The words live in `data/doubled/words.txt`, generated from the English lexicon by
//! `funput-lexicon-tool doubled`. Regenerate it when `en.tsv` or the core changes.

mod lookup;

use lookup::SortedLines;

use crate::model::{EngineConfig, Session};

/// English words a Telex revert would shorten, lowercase, sorted.
static WORDS: SortedLines = SortedLines::new(include_str!("../../../data/doubled/words.txt"));

/// Whether the feature runs under `config`. It is part of English restore, and only
/// the Telex family reverts on a letter key — VNI reverts on a digit.
fn active(config: &EngineConfig) -> bool {
    config.smart_restore && config.restore_doubled_keys && config.method.is_telex_family()
}

/// Follow a word that has had a revert: from then on every key it gets is raw.
pub(crate) fn note_key(session: &mut Session, key: char) {
    if !session.literal.is_empty() {
        session.literal.push(key);
    }
}

/// Keep the keys a revert is about to collapse. Called before the revert rewrites
/// `keys`, which at that point still hold the doubled key. A later revert in the
/// same word finds `literal` already following the keys and leaves it alone.
pub(crate) fn on_revert(session: &mut Session) {
    if session.literal.is_empty() && active(&session.config) {
        session.literal.push_str(&session.keys);
    }
}

/// Whether the word boundary should write [`Session::literal`] over what is shown.
///
/// A manual flip is the user's own choice for the word and outranks the table.
pub(crate) fn restorable(session: &Session) -> bool {
    !session.literal.is_empty()
        && active(&session.config)
        && session.restore_override.is_none()
        && session.buffer != session.literal
        && WORDS.contains(&session.literal)
}

#[cfg(test)]
mod tests;
