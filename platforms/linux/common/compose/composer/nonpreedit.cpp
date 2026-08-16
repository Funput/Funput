// Non-preedit mode. What it is and how a client is judged: nonpreedit.h.

#include "compose/composer/composer.h"

#include <vector>

#include "compose/key/boundary.h"
#include "ffi/utf8.h"

namespace funput {

namespace {

// Drop the last `count` characters. Characters, not bytes — the same unit
// `deleteChars` is in, and the same reason.
std::string dropLast(const std::string &text, uint32_t count) {
    std::vector<uint32_t> chars = decodeUtf8(text);
    const size_t keep = count < chars.size() ? chars.size() - count : 0;
    std::string out;
    for (size_t i = 0; i < keep; ++i) appendUtf8(out, chars[i]);
    return out;
}

} // namespace

// --- NonPreeditState ---------------------------------------------------------

void NonPreeditState::reset() {
    refused = false;
    retoneAllowed = true;
    lastDoc.clear();
    repairText.clear();
    repairDeleted = 0;
    repairAfterAdopt = false;
    justAdopted = false;
}

void NonPreeditState::noteRepair(uint32_t deleted, const std::string &text) {
    repairDeleted = deleted;
    repairText = text;
    repairAfterAdopt = justAdopted;
    justAdopted = false;
}

Verdict NonPreeditState::observe(const std::string &document) {
    Verdict verdict = Verdict::Unknown;
    if (!repairText.empty()) {
        const std::string dropped = lastDoc + repairText;
        const std::string applied = dropLast(lastDoc, repairDeleted) + repairText;
        if (document == dropped && dropped != applied) {
            verdict = repairAfterAdopt ? Verdict::RefuseRetone : Verdict::RefuseMode;
        }
        repairText.clear();
        repairDeleted = 0;
        repairAfterAdopt = false;
    }
    lastDoc = document;
    return verdict;
}

// --- Composer ----------------------------------------------------------------

void Composer::setNonPreedit(bool on) {
    // A client already caught dropping a delete stays refused until the next context,
    // whatever the shell asks for. IBus asks on every keystroke.
    nonPreedit_.on = on && !nonPreedit_.refused;
}

void Composer::onFocusChanged() {
    nonPreedit_.reset();
}

void Composer::observeDocument(const std::string &textBeforeCaret) {
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

ComposePlan Composer::planFromResult(const FunputResult &result) {
    // ACTION_NONE means the engine produced nothing to show: the key was not part of a
    // composition, so the app types it itself. The app's own edit is not something this
    // can predict, so any pending expectation is void.
    if (result.action == ACTION_NONE) {
        nonPreedit_.noteRepair(0, {});
        return ComposePlan::passThrough();
    }
    nonPreedit_.noteRepair(result.backspace, Handle::output(result));
    return ComposePlan::replace(nonPreedit_.repairDeleted, nonPreedit_.repairText);
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
