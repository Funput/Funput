//! Doubled-key restore — give a word back the key a revert swallowed.
//!
//! Pressing a tone key again reverts it (`pá` + `s` → `pas`), and the revert drops
//! the second key from `keys`, so `password` used to come out `pasword`. The habit
//! the revert serves stays as it is: `tesst` → `test`, `mixx` → `mix`. What changes
//! is that the keys are kept in [`Session::literal`], and when they spell an English
//! word a revert would otherwise shorten (`pass`, `password`), the word boundary
//! writes them back. Design: `docs/features/doubled-key-restore.md`.
//!
//! A word with no revert never starts its [`Literal`], which is the whole safety
//! argument: everything below starts from an active one, so Vietnamese — and every
//! other word — composes exactly as it did before the feature existed.
//!
//! Most of those words are decided before they end: once the shortened form on
//! screen can no longer begin any English word (`pasw`), the raw keys (`passw`) are
//! shown at once and the rest of the word is typed on top of them.
//!
//! The keys as typed are also what Flip shows as the raw form and what gõ tắt tries
//! first, so the key a revert swallowed is never out of the user's reach.
//!
//! Both lists live in `data/doubled/` — `words.txt` for the boundary, `decisive.txt`
//! for the mid-word switch — generated from the English lexicon by
//! `funput-lexicon-tool doubled`. Regenerate them when `en.tsv` or the core changes.

mod literal;
mod lookup;

pub(crate) use literal::Literal;
use lookup::SortedLines;

use crate::model::{EngineConfig, Session};

/// English words a Telex revert would shorten, lowercase, sorted.
static WORDS: SortedLines = SortedLines::new(include_str!("../../../data/doubled/words.txt"));

/// Raw-key prefixes at which the word can no longer begin an English word the user
/// means (neither the shortened form on screen nor what a doubled key meant), in
/// every setup with eager restore on, and which begin no word left out on purpose.
/// Lowercase, sorted.
static DECISIVE: SortedLines = SortedLines::new(include_str!("../../../data/doubled/decisive.txt"));

/// Whether the feature runs under `config`. It is part of English restore, and only
/// the Telex family reverts on a letter key — VNI reverts on a digit.
fn active(config: &EngineConfig) -> bool {
    config.smart_restore && config.restore_doubled_keys && config.method.is_telex_family()
}

/// Keep the keys a revert is about to collapse. Called before the revert rewrites
/// `keys`, which at that point still hold the doubled key.
///
/// Only keys the engine saw typed can start one. Backspace and `adopt` seed `keys`
/// from the text on screen (`â` for `aa`), and a literal built on those would hand
/// Flip and gõ tắt a spelling nobody typed; the word stays out of the feature.
pub(crate) fn on_revert(session: &mut Session) {
    if active(&session.config) && session.keys.is_ascii() {
        session.literal.start(&session.keys);
    }
}

/// Whether the live word has a literal the feature may use: it had a revert, which
/// rewrote `keys` (`pass` → `pas`, or `waw` → `uaw` in Telex nâng cao), and the
/// feature is still on.
pub(crate) fn has_literal(session: &Session) -> bool {
    session.literal.is_active() && active(&session.config)
}

/// The live word's keys as the user typed them: [`Session::literal`] once a revert
/// has rewritten `keys`, `keys` otherwise.
pub(crate) fn typed_keys(session: &Session) -> &str {
    if has_literal(session) {
        session.literal.as_str()
    } else {
        &session.keys
    }
}

/// Once the screen shows the literal, `keys` takes it too: the boundary then sees a
/// word already shown as typed, and English restore has nothing left to undo.
pub(crate) fn settle_keys(session: &mut Session) {
    session.keys.clear();
    session.keys.push_str(session.literal.as_str());
}

/// Whether the word boundary should write [`Session::literal`] over what is shown.
///
/// A manual flip is the user's own choice for the word and outranks the table.
/// Digits glued to the end are a number, not part of the word: `pass1` is `pass`.
pub(crate) fn restorable(session: &Session) -> bool {
    let literal = session.literal.as_str();
    has_literal(session)
        && session.restore_override.is_none()
        && session.buffer != literal
        && WORDS.contains(literal.trim_end_matches(|key: char| key.is_ascii_digit()))
}

/// Whether to show [`Session::literal`] instead of `composed` right now, mid-word.
///
/// The prefix list is exact, so the switch lands on the key at which the literal
/// becomes decisive; it grows by one key per keystroke from the word's first revert.
/// The caller only asks with no manual flip pinned, and it rides on eager restore:
/// switched off, the word waits for its boundary like any other.
pub(crate) fn decides_raw(session: &Session, composed: &str) -> bool {
    let literal = session.literal.as_str();
    has_literal(session)
        && session.config.eager_restore
        && composed != literal
        && DECISIVE.contains(literal)
}

#[cfg(test)]
mod tests;
