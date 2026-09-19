//! The state correction keeps beside the live word, and the moves that spend it.
//!
//! All of it hangs off `Session::correction`, which exists only while the setting is
//! on. Three pieces outlive a word on purpose: the correction parked at a boundary
//! (the platform answers it on the *next* call), the undo armed behind an applied
//! correction, and the word a user has already refused.

use crate::ImeResult;
use crate::correction::score::CorrectionCandidate;
use crate::correction::touch::{StoredTouch, TouchLog};
use crate::model::Session;

/// How many candidates the engine hands the platform. Eight is more than any
/// suggestion bar shows and keeps the ranked set a fixed-size array.
pub(crate) const MAX_CANDIDATES: usize = 8;

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub(crate) struct CorrectionState {
    /// Where the fingers landed for the word being typed.
    pub(crate) touch: TouchLog,
    /// The touch for the key the host is about to send.
    pub(crate) next_touch: Option<StoredTouch>,
    /// Replay vehicle for candidate search — one session reused for every replay, so
    /// its string capacity is paid for once.
    pub(crate) scratch: Session,
    pub(crate) candidates: [CorrectionCandidate; MAX_CANDIDATES],
    pub(crate) len: usize,
    pub(crate) pending: Option<Pending>,
    pub(crate) last: Option<Applied>,
    /// Raw keys of a word the user undid: not corrected again next time it is typed.
    pub(crate) suppressed: Option<String>,
}

impl CorrectionState {
    pub(crate) fn candidates(&self) -> &[CorrectionCandidate] {
        &self.candidates[..self.len]
    }
}

/// A correction the word boundary parked, waiting for the platform's answer.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub(crate) struct Pending {
    /// The word as the app is displaying it — what a correction has to delete.
    pub(crate) shown: String,
    pub(crate) shown_chars: usize,
    pub(crate) keys: String,
    pub(crate) boundary: char,
    /// Whether the boundary would have English-restored this word.
    pub(crate) restore: bool,
}

/// A correction that landed, and can still be undone by one Backspace.
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct Applied {
    pub(crate) shown: String,
    pub(crate) keys: String,
    pub(crate) corrected_chars: usize,
    pub(crate) boundary: char,
}

/// Apply the candidate at `index`, or fall back to what the engine would have done.
pub(crate) fn apply(session: &mut Session, index: Option<usize>) -> ImeResult {
    let Some(state) = session.correction.as_mut() else {
        return ImeResult::none();
    };
    let Some(pending) = state.pending.take() else {
        return ImeResult::none();
    };
    let Some(candidate) = index.and_then(|i| state.candidates[..state.len].get(i)) else {
        return fallback(&pending);
    };
    let corrected_chars = candidate.text().chars().count();
    let mut output = String::with_capacity(candidate.text().len() + pending.boundary.len_utf8());
    output.push_str(candidate.text());
    output.push(pending.boundary);
    state.last = Some(Applied {
        shown: pending.shown,
        keys: pending.keys,
        corrected_chars,
        boundary: pending.boundary,
    });
    ImeResult::send(pending.shown_chars + 1, output)
}

/// What the word boundary would have returned had correction never looked at this
/// word. Mirrors `boundary::english_restore_result` with one more backspace: that
/// path swallows the boundary key, while this one runs after the platform echoed it.
fn fallback(pending: &Pending) -> ImeResult {
    if !pending.restore {
        return ImeResult::none();
    }
    let mut output = String::with_capacity(pending.keys.len() + pending.boundary.len_utf8());
    output.push_str(&pending.keys);
    output.push(pending.boundary);
    ImeResult::send(pending.shown_chars + 1, output)
}

/// Settle a correction the platform never answered, and disarm the one-tap undo.
/// Runs before every keystroke, so nothing correction parks can outlive one key.
pub(crate) fn flush_pending(session: &mut Session) -> Option<ImeResult> {
    let state = session.correction.as_mut()?;
    state.last = None;
    let deferred = fallback(&state.pending.take()?);
    (deferred.action == crate::Action::Send).then_some(deferred)
}

/// Undo the correction applied on the previous keystroke, if Backspace is still the
/// very next thing the user does.
pub(crate) fn take_undo(session: &mut Session) -> Option<ImeResult> {
    if !session.buffer.is_empty() {
        return None;
    }
    let state = session.correction.as_mut()?;
    let applied = state.last.take()?;
    state.suppressed = Some(applied.keys);
    let mut output = applied.shown;
    output.push(applied.boundary);
    Some(ImeResult::send(applied.corrected_chars + 1, output))
}

/// Drop a parked correction and the undo behind it — the host is doing something
/// other than typing the next key, and a stale edit would land in the wrong place.
pub(crate) fn discard(session: &mut Session) {
    if let Some(state) = session.correction.as_mut() {
        state.pending = None;
        state.last = None;
    }
}

/// Create or drop the state so it exists exactly while the setting is on.
pub(crate) fn sync(session: &mut Session) {
    match (session.config.typo_correction, session.correction.is_some()) {
        (true, false) => session.correction = Some(Box::default()),
        (false, true) => session.correction = None,
        _ => {}
    }
}

#[cfg(test)]
mod tests;
