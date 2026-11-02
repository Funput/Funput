// Non-preedit mode. What it is and how a client is judged: nonpreedit.h.

#include "compose/composer/composer.h"

#include "ffi/utf8.h"

namespace funput {

// --- Composer ----------------------------------------------------------------

// On from the start, with no setting behind it: this setter is how a shell narrows
// the mode per client. It cannot revive a mode a client has already been caught
// breaking — see [NonPreeditState].
void Composer::setNonPreedit(bool on) {
    // A client already caught dropping a delete stays refused until the next context,
    // whatever the shell asks for. IBus asks on every keystroke.
    nonPreedit_.on = on && !nonPreedit_.refused;
}

// Clears what was learned about the last client, which is the only thing that lets a
// stood-down mode be tried again. `client` is how the shell names the focus — Fcitx5's program name, else the input
// context. A client caught dropping a delete keeps that verdict (Fcitx5 also
// re-activates on capability churn). Empty is always a fresh question (IBus has no
// name). See nonpreedit/clients.h.
void Composer::onFocusChanged(std::string_view client) {
    nonPreedit_.reset(client);
}

// Checks that the previous repair landed, and stands down as narrowly as the failure
// allows. `answered` is whether the client sent surrounding text since the last
// keystroke: without it an empty answer and no answer are the same string, and
// standing the mode down on silence would take it from the clients it was built for.
Observation Composer::observeDocument(const std::string &textBeforeCaret, bool selectionLive,
                                      bool answered, uint32_t selectedAfter) {
    nonPreedit_.selectionLive = selectionLive;
    nonPreedit_.selectionAfter = selectedAfter;
    nonPreedit_.answered = answered;
    // Taken before acting on the verdict, so the shell sees what this reading said
    // rather than the state the verdict left behind.
    const PredictedDocument::Ack ack = nonPreedit_.observe(textBeforeCaret);
    const Observation seen{ack.verdict, nonPreedit_.inSync, ack.acked, ack.pending};
    switch (seen.verdict) {
    case Verdict::Unknown:
        return seen;
    case Verdict::RefuseRetone:
        // Ordinary repairs still land here; only the one issued straight after the app
        // handled its own Backspace was dropped. Standing the whole mode down would
        // throw away something that works, so only re-toning goes.
        nonPreedit_.retoneAllowed = false;
        break;
    case Verdict::RefuseMode:
        // Remember *who* did it, so re-activating this same client — which Fcitx5 does
        // on every capability change — does not hand it the mode back and cost another
        // word. See nonpreedit/clients.h.
        nonPreedit_.clients.remember();
        nonPreedit_.refused = true;
        nonPreedit_.on = false;
        break;
    }
    // Either way the engine is holding a word the document does not have, and the next
    // delete would aim at text Funput never wrote.
    discard();
    return seen;
}

ComposePlan Composer::planFromResult(const FunputResult &result, char32_t typed) {
    // ACTION_NONE means the engine produced nothing to show: the key was not part of a
    // composition, so the app types `typed` itself. That is still a change to the
    // document, and a predictable one — recorded as "delete nothing, append this" so
    // the next reading can confirm the document is being followed. Without it, the
    // space that ends a word would leave us out of sync exactly when the Backspace
    // after it wants to know. A repair that deletes nothing can never produce a
    // verdict through the comparison; only `BlindWrites` counts it, as an answer.
    if (result.action == ACTION_NONE) {
        std::string text;
        if (typed != 0) appendUtf8(text, static_cast<uint32_t>(typed));
        nonPreedit_.noteRepair(0, text);
        return ComposePlan::passThrough();
    }
    // A live selection that is not a suffix after the caret is the user's own
    // highlight. Deleting into it would take their text, so the key goes through
    // and the composition is dropped.
    if (nonPreedit_.selectionLive && nonPreedit_.selectionAfter == 0 &&
        result.backspace > 0) {
        discard();
        return ComposePlan::passThrough();
    }
    std::string text = Handle::output(result);
    nonPreedit_.noteRepair(result.backspace, text);
    ComposePlan plan = ComposePlan::replace(result.backspace, std::move(text));
    plan.deleteAfterChars = nonPreedit_.selectionAfter;
    return plan;
}

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

// Backspace outside a word, and re-opening a finished one: nonpreedit/backspace.cpp.

ComposePlan Composer::endComposition(bool consumed) {
    // Nothing to commit. The word reached the document one keystroke at a time, so
    // committing it here would type the whole word a second time.
    handle_.clear();
    return consumed ? ComposePlan::swallow() : ComposePlan::passThrough();
}

} // namespace funput
