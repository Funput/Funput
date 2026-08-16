// Non-preedit mode. What it is and how a client is judged: nonpreedit.h.

#include "compose/composer/composer.h"

#include <vector>

#include "compose/key/boundary.h"
#include "ffi/utf8.h"

namespace funput {

// --- Composer ----------------------------------------------------------------

void Composer::setNonPreedit(bool on) {
    // A client already caught dropping a delete stays refused until the next context,
    // whatever the shell asks for. IBus asks on every keystroke.
    nonPreedit_.on = on && !nonPreedit_.refused;
}

void Composer::onFocusChanged() {
    nonPreedit_.reset();
}

void Composer::observeDocument(const std::string &textBeforeCaret, bool selectionLive) {
    nonPreedit_.selectionLive = selectionLive;
    switch (nonPreedit_.observe(textBeforeCaret)) {
    case Verdict::Unknown:
        return;
    case Verdict::RefuseRetone:
        // Ordinary repairs still land here; only the one issued straight after the app
        // handled its own Backspace was dropped. Standing the whole mode down would
        // throw away something that works, so only re-toning goes.
        nonPreedit_.retoneAllowed = false;
        break;
    case Verdict::RefuseMode:
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
    nonPreedit_.noteRepair(result.backspace, Handle::output(result));
    return ComposePlan::replace(nonPreedit_.repairDeleted, nonPreedit_.repairText);
}

ComposePlan Composer::backspaceOutsideWord() {
    // In non-preedit, do the deleting rather than letting the key through.
    //
    // Letting the app do it is what breaks re-toning on Chrome's address bar: the app
    // edits behind our back, and the repair issued on the next keystroke is discarded.
    // Ordinary repairs are honoured there, so keeping the deletion on that same channel
    // keeps the whole word on a path the client has already shown it will follow. It
    // also mirrors Windows, which deletes with synthetic Backspace presses rather than
    // asking the app — one ordered channel, no request anyone can decline.
    //
    // Only with positive evidence that the document being read is current. The
    // selection flag alone is not enough: it comes from the same cache, and in
    // Chrome's address bar that cache had not yet reported a fresh mouse selection —
    // so the key was taken over, the delete was refused, and Backspace appeared to do
    // nothing until pressed a second time. A confirmed repair is the proof; when the
    // client says nothing this simply declines, which costs re-toning rather than the
    // far commoner select-and-delete.
    //
    // Note this repair cannot be verified afterwards. It carries no text, so "applied"
    // and "dropped" read as the same document, which is the silent-client signature. A
    // client that refuses it will simply appear to ignore Backspace.
    if (!nonPreedit_.on || !nonPreedit_.inSync || nonPreedit_.selectionLive ||
        nonPreedit_.lastDoc.empty()) {
        return ComposePlan::passThrough();
    }
    return ComposePlan::replace(1, {});
}

ComposePlan Composer::endComposition(bool consumed) {
    // Nothing to commit. The word reached the document one keystroke at a time, so
    // committing it here would type the whole word a second time.
    handle_.clear();
    return consumed ? ComposePlan::swallow() : ComposePlan::passThrough();
}

bool Composer::adoptWordBeforeBackspace(const std::string &textBeforeCaret) {
    if (!nonPreedit_.on || !nonPreedit_.retoneAllowed || !effectiveEnabled_) return false;
    std::vector<uint32_t> chars = decodeUtf8(textBeforeCaret);
    if (chars.empty()) return false;
    // The app has not deleted it yet, so drop it here to see where the caret lands.
    chars.pop_back();

    size_t start = chars.size();
    while (start > 0 && !isBoundary(static_cast<char32_t>(chars[start - 1]), settings_.method)) {
        --start;
    }
    // The caret landed on a separator, not on a word — nothing to re-open. Mirrors
    // `CommittedTail::backspace` on Windows, which is the same rule sourced from a
    // shadow copy because a hook shell has no document to read.
    if (start == chars.size()) return false;

    std::string word;
    for (size_t i = start; i < chars.size(); ++i) appendUtf8(word, chars[i]);
    // The engine refuses anything that is not a Vietnamese syllable, which is what
    // keeps English words and URLs literal.
    if (!handle_.adopt(word)) return false;
    // The repair this arms is the one a client like Chrome's address bar drops.
    nonPreedit_.justAdopted = true;
    return true;
}

} // namespace funput
