// How far the journal believes its base (PredictedDocument's `trusted`), and the
// two counts that decide it: how many states the client has not reported, and
// how many stale readings are still to come after a caret key.
//
// A match (match.h) is a string equality, and a stale reading can equal a later
// prefix: an arrow key takes one character off the reading exactly as a pending ⌫
// would, and a client several keys late replays its own earlier keystrokes as
// readings, which the review's fuzz saw coincide with the keys typed after the
// arrow. So trust is earned only by a reading that confirms an edit writing text,
// at skip 0 and uncontested; a delete-only confirmation keeps it but cannot earn
// it; a restart or a contested match loses it. Two more rules come from what the
// journal knows about its readings. A journal reset by a focus change trusts its
// first reading, which reports the field as focus found it. After a caret key,
// every state the client was behind, plus the one the key produced, is still to
// be read — a lagging client answers that many stale readings first — so that
// many readings only restart the journal, whatever they equal. The stale count is
// what was measured before the caret key: a lag that grows after it, and a client
// that moves the caret without the reset GTK and Qt send, are outside what this
// can promise (platforms/linux/README.md names both).

#ifndef FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_TRUST_H
#define FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_TRUST_H

#include <algorithm>
#include <cstddef>

namespace funput {

struct Belief {
    // Why the journal starts over, which decides what the next readings are worth.
    enum class Reset { Focus, CaretKey, Overflow };

    bool trusted = false;    // the base is believed exact
    size_t unshown = 0;      // states the client has not reported yet, as last measured
    size_t stale = 0;        // readings still to come from before the last caret key
    bool trustFirst = false; // the next restart is from a focus-time reading

    // `pending` is how many edits the journal held when it was reset.
    void reset(Reset why, size_t pending) {
        stale = why == Reset::CaretKey ? std::max(unshown, pending) + 1 : 0;
        trustFirst = why == Reset::Focus;
        trusted = false;
        unshown = 0;
    }

    // A reading known to be stale: true while one remains to be consumed.
    bool consumeStale() {
        if (stale == 0) return false;
        --stale;
        return true;
    }

    // The journal restarted from a reading with `pending` edits kept provisional.
    void restarted(size_t pending) {
        unshown = std::max(unshown, pending);
        trusted = trustFirst && pending == 0;
        trustFirst = false;
    }

    // A reading matched: `wrote` is whether the edits it confirmed wrote text,
    // `pending` how many the journal still holds after it (`left` of them with the
    // skipped ones gone, the client's distance behind on a trusted base).
    void matched(bool fresh, bool contested, bool wrote, size_t left, size_t pending) {
        if (contested) trusted = false;
        else if (fresh && wrote) trusted = true;
        unshown = trusted ? left : std::max(unshown, pending);
    }
};

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_TRUST_H
