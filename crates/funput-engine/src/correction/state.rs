//! The state correction keeps beside the live word, and the moves that spend it.
//!
//! All of it hangs off `Session::correction`, which exists only while the setting is
//! on. Three pieces outlive a word on purpose: the correction parked at a boundary
//! (the platform answers it on the *next* call), the undo armed behind an applied
//! correction, and the word a user has already refused. A fourth — what a preview
//! found — lives exactly as long as the word it looked at.

mod apply;
mod metrics;

pub use metrics::CorrectionMetrics;

pub(crate) use apply::{apply, take_undo};

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
    /// How many of `candidates` describe the word the search last looked at. Zero
    /// again once the platform has answered, or the word has been left behind: a
    /// ranking must never outlive the word it was made for.
    pub(crate) len: usize,
    /// Whether the word exactly as typed stands as a rival to `candidates` — it does
    /// when every key carried touch evidence, so a finger square on its keys can
    /// speak for it. Lives beside `candidates` rather than in `Pending` because it is
    /// set by the search and read by the ranking, neither of which needs a boundary.
    pub(crate) typed_competes: bool,
    /// The live word `candidates` describe, when a host asked ahead of the boundary.
    pub(crate) preview: Option<Preview>,
    pub(crate) pending: Option<Pending>,
    pub(crate) last: Option<Applied>,
    /// Raw keys of a word the user undid: not corrected again next time it is typed.
    pub(crate) suppressed: Option<String>,
    pub(crate) metrics: CorrectionMetrics,
}

impl CorrectionState {
    pub(crate) fn candidates(&self) -> &[CorrectionCandidate] {
        &self.candidates[..self.len]
    }
}

/// What a preview searched, kept until the next letter key so the boundary can take
/// the candidates as they stand instead of searching the same word twice.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub(crate) struct Preview {
    pub(crate) keys: String,
}

/// A correction the word boundary parked, waiting for the platform's answer.
#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub(crate) struct Pending {
    /// The word as the app is displaying it once the boundary keystroke has been
    /// handled — the raw keys when English restore ran, the composition otherwise.
    /// It is what a correction has to delete.
    pub(crate) shown: String,
    pub(crate) shown_chars: usize,
    pub(crate) keys: String,
    pub(crate) boundary: char,
}

/// A correction that landed, and can still be undone by one Backspace.
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct Applied {
    pub(crate) shown: String,
    pub(crate) keys: String,
    pub(crate) corrected_chars: usize,
    pub(crate) boundary: char,
}

/// Snapshot what the platform will need, before the boundary clears the session.
///
/// `shown` is the word as the app displays it *after* this keystroke: English restore
/// runs in the same keystroke and puts the raw keys back on screen, so a correction
/// that follows it has to delete those, not the composition they replaced.
pub(crate) fn park(state: &mut CorrectionState, session: &Session, boundary: char, restore: bool) {
    let shown = if restore {
        &session.keys
    } else {
        &session.buffer
    };
    let pending = state.pending.get_or_insert_with(Pending::default);
    pending.shown.clear();
    pending.shown.push_str(shown);
    pending.shown_chars = shown.chars().count();
    pending.keys.clear();
    pending.keys.push_str(&session.keys);
    pending.boundary = boundary;
}

/// Let a correction the platform never answered lapse, and disarm the one-tap undo.
/// Runs before every keystroke, so nothing correction parks can outlive one key.
///
/// Nothing is replayed. The boundary that parked the correction already did
/// everything it would have done without the feature — English restore included —
/// so an unanswered correction leaves the document exactly as it stands.
pub(crate) fn flush_pending(session: &mut Session) {
    let Some(state) = session.correction.as_mut() else {
        return;
    };
    state.last = None;
    if state.pending.take().is_some() {
        state.len = 0;
    }
}

/// Drop a parked correction, the undo behind it and any preview — the host is doing
/// something other than typing the next key, and a stale edit would land in the
/// wrong place.
pub(crate) fn discard(session: &mut Session) {
    if let Some(state) = session.correction.as_mut() {
        state.preview = None;
        state.pending = None;
        state.last = None;
        state.len = 0;
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
