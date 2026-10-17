//! Looking at the live word before it ends: the boundary's search, run early for a
//! host that wants to show the repair coming.
//!
//! Every call here is explicit — none of it is on the keystroke path, so the hot
//! path's allocation budget is untouched. What a preview finds stands until the next
//! letter key, and the boundary takes it as it is rather than searching the same
//! word twice.

use crate::correction::state::{CorrectionState, Preview};
use crate::correction::{gate, searched};
use crate::model::Session;

/// Search the live word as the boundary will, and return how many candidates it
/// reaches.
///
/// 0 for a word that is still alive — one more key could make it a syllable, so
/// there is nothing to repair yet — as well as for one the gate refuses or one the
/// user already undid. A word is only searched once: asking again before the next
/// letter key answers from what the first call found.
pub(crate) fn preview(session: &mut Session) -> usize {
    if !gate::eligible_live(session) {
        return 0;
    }
    let Some(mut state) = session.correction.take() else {
        return 0;
    };
    let found = if is_suppressed(&state, &session.keys) {
        0
    } else if reusable(&state, &session.keys) {
        state.len
    } else if is_dead(session) {
        let found = searched(&mut state, session);
        remember(&mut state, &session.keys);
        found
    } else {
        0
    };
    session.correction = Some(state);
    found
}

/// Whether what the last preview found still describes `keys`.
pub(crate) fn reusable(state: &CorrectionState, keys: &str) -> bool {
    state
        .preview
        .as_ref()
        .is_some_and(|preview| preview.keys == keys)
}

/// Forget the preview: the word it looked at has changed. Its candidates go with it
/// unless the boundary has already parked them for the platform's answer.
pub(crate) fn invalidate(session: &mut Session) {
    if let Some(state) = session.correction.as_mut() {
        state.preview = None;
        if state.pending.is_none() {
            state.len = 0;
        }
    }
}

/// The user tapped the word as they typed it: leave it, and do not offer this
/// spelling again the next time it is typed. Works on the word being typed or on the
/// one a boundary just parked, whichever is in hand; a host with neither asks for
/// nothing.
pub(crate) fn refuse(session: &mut Session) {
    let Some(state) = session.correction.as_mut() else {
        return;
    };
    let pending = state.pending.take();
    let word = pending
        .as_ref()
        .map_or(session.keys.as_str(), |pending| pending.keys.as_str());
    if word.is_empty() {
        return;
    }
    let suppressed = state.suppressed.get_or_insert_with(String::new);
    suppressed.clear();
    suppressed.push_str(word);
    state.preview = None;
    state.last = None;
    state.len = 0;
}

/// The word the user undid is not previewed either — read without consuming, so the
/// boundary still gets to honour (and count) the refusal.
fn is_suppressed(state: &CorrectionState, keys: &str) -> bool {
    state.suppressed.as_deref() == Some(keys)
}

/// Whether the live word can no longer become a syllable, judged without
/// allocating first.
///
/// Two cheap signs settle most words: eager restore has already collapsed the buffer
/// to its raw keys (the composition it abandoned is still in `vn_form`), or the
/// reachability rules say no further key can rescue it. Only a word they call dead
/// pays for the syllable check the boundary would — a sanity check that it is not,
/// after all, complete. A word still alive is not searched: the repair may come
/// from the user's next key, not from the engine.
fn is_dead(session: &Session) -> bool {
    let restored = session.buffer == session.keys && session.vn_form != session.keys;
    let rules = session.config.syllable_rules;
    (restored || rules.is_definitely_invalid_in(&session.buffer, session.config.method))
        && !rules.is_complete_syllable(&session.buffer)
}

fn remember(state: &mut CorrectionState, keys: &str) {
    let preview = state.preview.get_or_insert_with(Preview::default);
    preview.keys.clear();
    preview.keys.push_str(keys);
}
