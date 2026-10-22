//! Which form of the live word the app shows after a keystroke.

use funput_core::TransformKind;

use crate::compose::RestoreOverride;
use crate::compose::doubled;
use crate::model::Session;

/// Which form [`displayed`] picked, so [`super::process`] knows what `keys` must follow.
pub(super) enum Shown {
    /// What the core composed — or the Vietnamese form a flip pinned.
    Composed(String),
    /// `keys`: eager English restore, or a raw flip on a word that had no revert.
    Keys,
    /// The keys as typed, any a revert swallowed included: a decisive prefix, or a raw
    /// flip on a word that had a revert.
    Literal,
}

/// The form the app shows after this key, from the composition core produced.
///
/// A manual flip pins the form for the rest of the word, overriding every automatic
/// decision below it.
pub(super) fn displayed(
    session: &Session,
    composed: String,
    kind: TransformKind,
    key: char,
) -> Shown {
    match session.restore_override {
        Some(RestoreOverride::ForceVietnamese) => Shown::Composed(composed),
        // The raw form is what the user typed, including any key a revert swallowed.
        Some(RestoreOverride::ForceRaw) if doubled::has_literal(session) => Shown::Literal,
        Some(RestoreOverride::ForceRaw) => Shown::Keys,
        // Doubled-key restore: the shortened form can no longer begin an English word
        // (`pasw`), so the keys a revert swallowed come back now (`passw`).
        None if doubled::decides_raw(session, &composed) => Shown::Literal,
        // Eager English restore: flip to the raw keystrokes the instant the word can
        // no longer be Vietnamese (`tẽt` → `text` on the closing `t`). Gated by the
        // smart + eager toggles. Skip on Reverted (a deliberate user restore) and when
        // nothing was transformed (`keys == composed`, e.g. a literal digit `ng1`).
        None if session.config.smart_restore
            && session.config.eager_restore
            && kind != TransformKind::Reverted
            && session.keys != composed
            && is_dead_end(session, &composed, key) =>
        {
            Shown::Keys
        }
        None => Shown::Composed(composed),
    }
}

/// Whether `composed` can no longer become Vietnamese, judged for this keystroke.
///
/// VNI may keep the finals only place names use (`Pa8h` → `Păh`, as in Chư Păh),
/// but a digit landing on a word that carried no mark yet is how numbers glue to
/// English (`bar1`, `ver2`), so that keystroke is judged strictly. A letter after
/// a digit (the `h` of `Pa8h`), or a digit on a word already marked (`Pa8h1` →
/// `Pắh`), only extends a word the user shaped on purpose. What a strict reading
/// restores stays in `vn_form`, so Flip still recovers `Pah8` → `Păh`.
fn is_dead_end(session: &Session, composed: &str, key: char) -> bool {
    let bare_before = || {
        session
            .keys
            .strip_suffix(key)
            .is_some_and(|before| before == session.buffer)
    };
    let rules = session.config.syllable_rules;
    if key.is_ascii_digit() && bare_before() {
        rules.is_definitely_invalid(composed)
    } else {
        rules.is_definitely_invalid_in(composed, session.config.method)
    }
}
