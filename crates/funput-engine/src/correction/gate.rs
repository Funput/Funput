//! Which finished words correction may look at, and the one it counts but leaves.

use funput_core::{InputMethod, is_bare_shaped_vowel};

use crate::compose::boundary::Verdict;
use crate::correction::touch::from_q8;
use crate::model::Session;

/// How close, in key pitches, a neighbour has to come to the key that registered
/// before a touch reads as landing on the edge between them.
const EDGE: f32 = 0.25;

/// Whether the word that just ended may be looked at at all.
///
/// Every clause here answers "is this a slip?", and each one that says no leaves the
/// word to the engine's existing behaviour. They are ordered cheapest first; the
/// syllable checks at the end are the only ones that allocate.
pub(super) fn eligible(session: &Session, verdict: &Verdict) -> bool {
    // The state is built when the setting goes on and dropped when it goes off, so
    // its absence is the switch.
    let Some(state) = session.correction.as_ref() else {
        return false;
    };
    session.enabled
        // A word the user flipped by hand is the form they asked for.
        && session.restore_override.is_none()
        && !session.buffer.is_empty()
        && !verdict.complete
        && state.touch.matches(&session.keys)
        && !has_literal_digit(session)
        && !is_all_uppercase(&session.buffer)
        // `aw` → `ă` is not a complete syllable, but it is exactly the letter the
        // user asked for.
        && !is_bare_shaped_vowel(&session.buffer)
}

/// A digit in the word means it is not a syllable — except under VNI, where the tone
/// and shape modifiers *are* digits.
fn has_literal_digit(session: &Session) -> bool {
    session.config.method != InputMethod::Vni && session.buffer.chars().any(|c| c.is_ascii_digit())
}

/// `NHSF` is an acronym typed in anger, not a fumbled syllable.
fn is_all_uppercase(text: &str) -> bool {
    text.chars().any(char::is_uppercase) && !text.chars().any(char::is_lowercase)
}

/// Count a word that ended as a real syllable although one of its touches landed on
/// the edge between two keys.
///
/// Correction never touches such a word — it is a word — but some of them are slips
/// onto another word (`bạn` meant, `bán` typed). How many is the number that says
/// whether sentence context is worth building; the count is all that is kept.
pub(super) fn note_valid_near_edge(session: &mut Session, verdict: &Verdict) {
    let Some(state) = session.correction.as_mut() else {
        return;
    };
    if !verdict.complete || !state.touch.matches(&session.keys) {
        return;
    }
    let near_edge = state.touch.entries().iter().any(|entry| {
        let typed = from_q8(entry.typed_distance);
        entry
            .alternates()
            .iter()
            .any(|alternate| from_q8(alternate.distance) - typed < EDGE)
    });
    if near_edge {
        state.metrics.valid_near_edge += 1;
    }
}
