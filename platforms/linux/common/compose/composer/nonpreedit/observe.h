// The three-string comparison that judges one repair. Lives beside the state
// struct rather than inside it so nonpreedit.h stays inside the 150-line budget.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_OBSERVE_H
#define FUNPUT_COMPOSE_NONPREEDIT_OBSERVE_H

#include "ffi/utf8.h"

namespace funput {

inline Verdict NonPreeditState::observe(const std::string &document) {
    Verdict verdict = Verdict::Unknown;
    if (!repairText.empty()) {
        const std::string dropped = lastDoc + repairText;
        const std::string applied = dropLast(lastDoc, repairDeleted) + repairText;
        inSync = document == applied;
        if (document == dropped && dropped != applied) {
            verdict = repairAfterAdopt ? Verdict::RefuseRetone : Verdict::RefuseMode;
        }
        // The comparison above is blind to a document that is always empty, which
        // is a failure of its own rather than an absence of one.
        if (verdict == Verdict::Unknown && blind.observe(document, answered)) {
            verdict = Verdict::RefuseMode;
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

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_OBSERVE_H
