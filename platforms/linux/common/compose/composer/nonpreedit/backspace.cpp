// Non-preedit mode, Backspace half: who deletes a character that is already in the
// document, and when a finished word is re-opened. What the mode is: nonpreedit.h.

#include "compose/composer/composer.h"

#include <vector>

#include "compose/key/boundary.h"
#include "ffi/utf8.h"

namespace funput {

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
    // Note this repair cannot convict a client afterwards. It carries no text, so
    // "dropped" reads as the unchanged document, which is the silent-client signature.
    // A client that refuses it will simply appear to ignore Backspace.
    // Still recorded: seeing it land keeps `inSync`, so the next ⌫ stays here too.
    // Unrecorded, ⌫⌫ over `gox ` handed the `x` to the app, Chrome dropped the repair
    // after it, and re-toning `go` wrote `goõ`.
    if (!nonPreedit_.on || !nonPreedit_.inSync || nonPreedit_.selectionLive ||
        nonPreedit_.lastDoc.empty()) {
        return ComposePlan::passThrough();
    }
    nonPreedit_.noteRepair(1, {});
    return ComposePlan::replace(1, {});
}

bool Composer::adoptWordBeforeBackspace(const std::string &textBeforeCaret) {
    if (!nonPreedit_.on || !nonPreedit_.retoneAllowed || !effectiveEnabled_) return false;
    // Only a document seen to be current: Chrome can report `gox ` a keystroke late as
    // `gox`, and adopting the `go` that leaves would make the next `x` write `goõ`.
    if (!nonPreedit_.inSync) return false;
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
