// What one reading of the document is worth to the mode. The journal it is judged
// against: predicted.h. Lives beside the state struct rather than inside it so
// nonpreedit.h stays inside the 150-line budget.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_OBSERVE_H
#define FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_OBSERVE_H

namespace funput {

inline PredictedDocument::Ack NonPreeditState::observe(const std::string &reading) {
    // A Backspace journaled for the previous key is spent: the next one must earn
    // its own record before a word may be re-opened over it.
    backspaceJournaled = false;
    // Not an answer: the client has said nothing since the previous key, and what
    // the shell hands over is the answer it cached then. Silence is not a verdict
    // (RFC §6.1, invariant 3), and it is not evidence either — equal to a prefix it
    // could only repeat what the previous key confirmed; equal to none it would
    // restart the journal from a document the client has since moved past, and the
    // next real answer could then read as a dropped delete. So the journal stands,
    // and this key is neither in sync nor on track.
    if (!answered) {
        inSync = onTrack = false;
        return {0, document.pending(), false, document.trusted(), Verdict::Unknown};
    }
    // Whether the pending edits wrote text, and whether they leave the document
    // predicted non-empty, is asked *before* the journal moves on: the reading is
    // judged against the writes it may have swallowed.
    const bool growing = document.pendingWrites() && !document.predicted().empty();
    const PredictedDocument::Ack ack = document.observe(reading);
    // Every edit confirmed, on a journal whose base is believed exact. With nothing
    // pending nothing was written, so nothing was confirmed either — a document
    // that moved on its own lands there, and so does the reading after a confirmed
    // one.
    inSync = ack.trusted && ack.pending > 0 && ack.acked == ack.pending;
    // At least one edit confirmed, on a trusted journal. k == 0 is a reading equal
    // to the base, which is byte for byte what a client that has not answered, or
    // has stopped answering, reads: no evidence that this client follows anything.
    // And a journal not yet trusted may have matched a stale reading by
    // coincidence, so its prediction is not one to re-open a word from.
    onTrack = ack.trusted && ack.matched && ack.acked >= 1;
    // The prefix comparison is blind to a document that is always empty, which is a
    // failure of its own rather than an absence of one — see BlindWrites. What is
    // counted is Cursor's shape: a reading of `` while the journal predicts text.
    // Not an empty answer the user earned by deleting everything: then the
    // prediction is empty too, or the answer confirms the deletions (a client a
    // few keys late says so a few keys late). Those pass as silence, which counts
    // nothing and still lets any text reset the count. An empty answer equal to the
    // base confirms nothing — a ⌫ pending on a base of `` used to explain `` as a
    // later prefix and let Cursor write on for a dozen keys.
    const bool wrote = growing && !(ack.matched && ack.acked >= 1);
    PredictedDocument::Ack seen = ack;
    if (seen.verdict == Verdict::Unknown && blind.observe(reading, wrote)) {
        seen.verdict = Verdict::RefuseMode;
    }
    return seen;
}

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_OBSERVE_H
