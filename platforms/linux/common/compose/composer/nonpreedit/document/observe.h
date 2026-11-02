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
    // Whether any pending edit wrote text is asked *before* the journal moves on:
    // the reading is judged against the writes it may have swallowed.
    const bool wrote = document.pendingWrites();
    PredictedDocument::Ack ack = document.observe(reading);
    // Every edit confirmed. With nothing pending nothing was written, so nothing
    // was confirmed either — a document that moved on its own (the user clicking,
    // or selecting with the mouse) lands there, and so does the reading after a
    // confirmed one.
    inSync = ack.pending > 0 && ack.acked == ack.pending;
    // At least one edit confirmed. k == 0 is a reading equal to the base, which is
    // byte for byte what a client that has not answered, or has stopped
    // answering, reads: no evidence that this client follows anything.
    onTrack = ack.matched && ack.acked >= 1;
    // The prefix comparison is blind to a document that is always empty, which is a
    // failure of its own rather than an absence of one — see BlindWrites.
    if (wrote && ack.verdict == Verdict::Unknown && blind.observe(reading, answered)) {
        ack.verdict = Verdict::RefuseMode;
    }
    return ack;
}

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_OBSERVE_H
