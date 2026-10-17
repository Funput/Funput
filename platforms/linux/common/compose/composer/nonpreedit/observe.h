// The three-string comparison that judges one repair. Lives beside the state
// struct rather than inside it so nonpreedit.h stays inside the 150-line budget.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_OBSERVE_H
#define FUNPUT_COMPOSE_NONPREEDIT_OBSERVE_H

#include "ffi/utf8.h"

namespace funput {

inline Verdict NonPreeditState::observe(const std::string &document) {
    Verdict verdict = Verdict::Unknown;
    if (!repairText.empty() || repairDeleted > 0) {
        const std::string dropped = lastDoc + repairText;
        const std::string applied = dropLast(lastDoc, repairDeleted) + repairText;
        inSync = document == applied;
        // A delete with nothing written — Funput taking a Backspace over — still
        // proves the document is current when it is seen to land, which is what lets
        // the *next* Backspace stay on Funput's channel too. It can never convict:
        // "dropped" is then the unchanged document, which is also what a client that
        // has not answered yet reads.
        if (!repairText.empty()) {
            if (document == dropped && dropped != applied) {
                verdict = repairAfterAdopt ? Verdict::RefuseRetone : Verdict::RefuseMode;
            }
            // The comparison above is blind to a document that is always empty, which
            // is a failure of its own rather than an absence of one.
            if (verdict == Verdict::Unknown && blind.observe(document, answered)) {
                verdict = Verdict::RefuseMode;
            }
        }
        repairText.clear();
        repairDeleted = 0;
        repairAfterAdopt = false;
    } else {
        // Nothing was written, so nothing was confirmed. A document that moved on
        // its own — the user clicking, or selecting with the mouse — lands here.
        inSync = false;
    }
    lastDoc = document;
    return verdict;
}

// The three strings above judge a *client*. This is about the *user*: the caret moved
// from under the word, which the engine still holds. The next tone key would then
// delete characters in front of the new caret that Funput never wrote.
//
// The client usually says nothing. Qt, Chromium and Firefox reset an input method on a
// click only while they hold a preedit, and this mode has none. So the shell passes the
// text after the caret, which our own typing cannot change — however late the client
// reports it — and a change means the caret went somewhere else.
//
// Deliberately narrow, because the cost of being wrong is asymmetric. A false alarm
// drops one word's tone and lets the key through as typed; a miss deletes the user's
// text. It still stays quiet whenever it cannot be sure: no reading (`after` unknown),
// a client that has not answered since the last key, or a selection — a Sheets or
// omnibox suffix is text after the caret that changes with every key and that our own
// repair removes, so the reading after one must start afresh.
inline bool NonPreeditState::movedOnItsOwn(const std::optional<std::string> &after) {
    if (!after) return false;
    if (selectionLive || selectionAfter > 0) {
        lastAfter.reset();
        return false;
    }
    const bool moved = answered && lastAfter.has_value() && *after != *lastAfter;
    lastAfter = after;
    return moved;
}

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_OBSERVE_H
