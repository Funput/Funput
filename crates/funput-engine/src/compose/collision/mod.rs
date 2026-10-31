//! Collision restore — give an English word back when composition made it a real
//! Vietnamese syllable.
//!
//! English restore at the word boundary trusts the syllable check: whatever
//! composes into a complete Vietnamese syllable is kept, and that is right for
//! almost every word. A *collision* is the exception a composition rule creates:
//! a common English word whose keys the rule reads as diacritics, landing on a
//! syllable that is real but is not what anyone typing those keys meant. Telex
//! holds a `w` typed after the onset back for the vowel behind it, so `two` comes
//! out `tơ` (issue #661).
//!
//! Each such rule is a [`Collision`] class with its own embedded word list. The
//! boundary writes the raw keys back only for a listed word that the class's rule
//! actually shaped, so nothing outside the lists changes: `tow` and `cwon` still
//! type `tơ` and `cơn`. Mid-word nothing switches — `tơ`, `să` and `sư` begin many
//! real syllables, so only the finished word can say it was English.
//!
//! The lists live in `data/collision/<class>/words.txt`, generated from the English
//! lexicon by `funput-lexicon-tool collision`, with hand corrections in the
//! `overrides.txt` beside each. Regenerate them when `en.tsv` or the core changes;
//! the tool's `collision_table` test fails until you do.
//! Design: `docs/features/english-collision-restore.md`.

mod class;

pub(crate) use class::Collision;

use crate::model::{EngineConfig, Session};

/// Whether the feature runs under `config`. It is part of English restore.
fn active(config: &EngineConfig) -> bool {
    config.smart_restore && config.restore_collisions
}

/// Whether the word boundary should write `session.keys` over what is shown.
///
/// A manual flip is the user's own choice for the word and outranks the lists, and
/// a word already shown as typed has nothing to restore.
pub(crate) fn restorable(session: &Session) -> bool {
    active(&session.config)
        && session.restore_override.is_none()
        && session.buffer != session.keys
        && Collision::matching(&session.config, &session.keys).is_some()
}

#[cfg(test)]
mod tests;
