// Non-preedit mode. What it is and how a client is judged: nonpreedit.h.

#include "compose/composer/composer.h"

#include "ffi/utf8.h"

namespace funput {

// --- Composer ----------------------------------------------------------------

void Composer::setNonPreedit(bool on) {
    // A client already caught dropping a delete stays refused until the next context,
    // whatever the shell asks for. IBus asks on every keystroke.
    nonPreedit_.on = on && !nonPreedit_.refused;
}

void Composer::onFocusChanged(std::string_view client) {
    nonPreedit_.reset(client);
}

void Composer::observeDocument(const std::string &textBeforeCaret, bool selectionLive,
                               bool answered, uint32_t selectedAfter,
                               const std::optional<std::string> &textAfterCaret) {
    nonPreedit_.selectionLive = selectionLive;
    nonPreedit_.selectionAfter = selectedAfter;
    nonPreedit_.answered = answered;
    const bool moved = nonPreedit_.movedOnItsOwn(textAfterCaret);
    switch (nonPreedit_.observe(textBeforeCaret)) {
    case Verdict::Unknown:
        // The caret left the word without telling us. Forget the word, and let the
        // key through as typed rather than repair text that is no longer there.
        if (moved && isComposing()) discard();
        return;
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
}

ComposePlan Composer::planFromResult(const FunputResult &result, char32_t typed) {
    // ACTION_NONE means the engine produced nothing to show: the key was not part of a
    // composition, so the app types `typed` itself. That is still a change to the
    // document, and a predictable one — recorded as "delete nothing, append this" so
    // the next reading can confirm the document is being followed. Without it, the
    // space that ends a word would leave us out of sync exactly when the Backspace
    // after it wants to know. A repair that deletes nothing can never produce a
    // verdict, so this cannot accuse anyone.
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
    nonPreedit_.noteRepair(result.backspace, Handle::output(result));
    ComposePlan plan = ComposePlan::replace(nonPreedit_.repairDeleted, nonPreedit_.repairText);
    plan.deleteAfterChars = nonPreedit_.selectionAfter;
    return plan;
}

// Backspace outside a word, and re-opening a finished one: nonpreedit/backspace.cpp.

ComposePlan Composer::endComposition(bool consumed) {
    // Nothing to commit. The word reached the document one keystroke at a time, so
    // committing it here would type the whole word a second time.
    handle_.clear();
    return consumed ? ComposePlan::swallow() : ComposePlan::passThrough();
}

} // namespace funput
