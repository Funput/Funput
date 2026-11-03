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
// stood-down mode be tried again. `client` is how the shell names the focus —
// Fcitx5's program name, else the input context. A client caught dropping a delete
// keeps that verdict (Fcitx5 also re-activates on capability churn). Empty is always
// a fresh question (IBus has no name). See nonpreedit/clients.h.
void Composer::onFocusChanged(std::string_view client) {
    nonPreedit_.reset(client);
}

// Checks that the previous repair landed, and stands down as narrowly as the failure
// allows. `answered` is whether the client sent surrounding text since the last
// keystroke: without it an empty answer and no answer are the same string, and
// standing the mode down on silence would take it from the clients it was built for.
// A reading not answered is the shell's cache, and leaves the journal untouched.
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

// An edit the app makes itself: nonpreedit/appedits.cpp. Backspace outside a word,
// and re-opening a finished one: nonpreedit/backspace.cpp.

ComposePlan Composer::endComposition(bool consumed) {
    // Nothing to commit. The word reached the document one keystroke at a time, so
    // committing it here would type the whole word a second time.
    handle_.clear();
    return consumed ? ComposePlan::swallow() : ComposePlan::passThrough();
}

} // namespace funput
