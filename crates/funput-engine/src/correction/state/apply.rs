//! Spending a parked correction: the platform's answer, and the Backspace that takes
//! an applied one back.

use crate::ImeResult;
use crate::correction::state::Applied;
use crate::model::Session;

/// Apply the candidate at `index`; `None`, or an index that names no candidate,
/// declines.
///
/// Declining is a no-op. The boundary keystroke already did everything it would have
/// done without correction, English restore included, so there is nothing left to
/// finish. Either way the parked correction is answered: it and its candidates are
/// gone afterwards.
pub(crate) fn apply(session: &mut Session, index: Option<usize>) -> ImeResult {
    let Some(state) = session.correction.as_mut() else {
        return ImeResult::none();
    };
    let Some(pending) = state.pending.take() else {
        return ImeResult::none();
    };
    let edit = index
        .and_then(|i| state.candidates().get(i))
        .map(|candidate| {
            let text = candidate.text();
            let mut output = String::with_capacity(text.len() + pending.boundary.len_utf8());
            output.push_str(text);
            output.push(pending.boundary);
            (output, text.chars().count())
        });
    // Answered either way: the ranking described a word that is now closed.
    state.len = 0;
    let Some((output, corrected_chars)) = edit else {
        return ImeResult::none();
    };
    state.metrics.applied += 1;
    state.last = Some(Applied {
        shown: pending.shown,
        keys: pending.keys,
        corrected_chars,
        boundary: pending.boundary,
    });
    ImeResult::send(pending.shown_chars + 1, output)
}

/// Undo the correction applied on the previous keystroke, if Backspace is still the
/// very next thing the user does.
pub(crate) fn take_undo(session: &mut Session) -> Option<ImeResult> {
    if !session.buffer.is_empty() {
        return None;
    }
    let state = session.correction.as_mut()?;
    let applied = state.last.take()?;
    state.metrics.reverted += 1;
    state.suppressed = Some(applied.keys);
    let mut output = applied.shown;
    output.push(applied.boundary);
    Some(ImeResult::send(applied.corrected_chars + 1, output))
}
