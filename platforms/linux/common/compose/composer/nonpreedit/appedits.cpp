// Non-preedit mode, the app's own edits: a space typed with nothing composing, a
// Backspace handed to the app, a Backspace shortening a word that stands in the
// document. What the mode is: nonpreedit.h.

#include "compose/composer/composer.h"

#include "ffi/utf8.h"

namespace funput {

ComposePlan Composer::appPerforms(uint32_t deleted, char32_t typed) {
    // An edit the app makes itself is still a change to the document, and a
    // predictable one: a space typed with nothing composing, a Backspace handed to
    // the app, a Backspace shortening a word that stands in the document. Journaled,
    // it lets the next reading confirm the document is still being followed — the
    // evidence `backspaceOutsideWord()` and `adoptWordBeforeBackspace()` require.
    // Unrecorded, `phủ ␣ ␣ ⌫ ⌫ s` lost `inSync` at the second space and typed a plain
    // `s`. The ACTION_NONE branch above is the same idea for a key the engine saw.
    //
    // Journaled whether or not the document is in sync. With one record that was
    // unsafe: a client one keystroke late reads the document as it stood before the
    // previous key, and two identical edits in a row — ␣␣, ⌫⌫ — made that stale
    // reading equal what the record predicted, so Funput took the next ⌫ over and
    // re-opened a word one character too long (`phủ ␣ ␣ ⌫ s` became `phủú`). The
    // journal keeps every unconfirmed edit, so that reading matches prefix n−1 of
    // n: on track, never in sync (nonpreedit/document/predicted.h). And a record
    // made out of sync is what lets a lagging client re-tone: `phur ␣ ⌫ s` is `phú`
    // there only because the ⌫ handed to the app is in the journal.
    //
    // These records never convict through the three-string comparison: with nothing
    // deleted, "applied" and "dropped" are the same string, and with nothing written
    // `observe()` keeps the record out of the verdict. A record that writes text is
    // still an answer `BlindWrites` counts, as the letters the app types in the
    // ACTION_NONE branch always have been. Windows keeps the same journal in
    // `CommittedTail` (retone.rs).
    //
    // A live selection is excluded: what the key does to it is the app's business —
    // replace it, or accept a Sheets suffix and append — so there is nothing to
    // predict. Only the boundary path can get here with one: `onKey` passes a
    // Backspace over a selection straight through, ahead of both Backspace paths.
    if (nonPreedit_.on && !nonPreedit_.selectionLive) {
        std::string text;
        if (typed != 0) appendUtf8(text, static_cast<uint32_t>(typed));
        // The repair a re-opened word arms is the next one *Funput* writes; the app
        // shortening the word in between must not spend it, or a client like the
        // address bar dropping that repair would cost the mode instead of re-toning.
        const bool adopted = nonPreedit_.justAdopted;
        nonPreedit_.noteRepair(deleted, text);
        nonPreedit_.justAdopted = adopted;
        // A deletion, so the word before it may be re-opened — see backspace.cpp.
        if (deleted > 0) nonPreedit_.backspaceJournaled = true;
    }
    return ComposePlan::passThrough();
}

} // namespace funput
