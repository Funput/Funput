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
    // Only with positive evidence that the document being read is current, which is
    // every journaled edit confirmed — not merely the client following a keystroke
    // behind. The selection flag alone is not enough: it comes from the same cache,
    // and in Chrome's address bar that cache had not yet reported a fresh mouse
    // selection — so the key was taken over, the delete was refused, and Backspace
    // appeared to do nothing until pressed a second time. A reading one keystroke
    // late is that same stale-selection shape, hence in sync and nothing less. When
    // the client says nothing this simply declines, which costs re-toning rather
    // than the far commoner select-and-delete.
    //
    // Note this repair cannot convict a client afterwards. It carries no text, so
    // "dropped" reads as the unchanged document, which is the silent-client signature.
    // A client that refuses it will simply appear to ignore Backspace.
    // Still recorded: seeing it land keeps `inSync`, so the next ⌫ stays here too.
    // Unrecorded, ⌫⌫ over `gox ` handed the `x` to the app, Chrome dropped the repair
    // after it, and re-toning `go` wrote `goõ`.
    if (!nonPreedit_.on || !nonPreedit_.inSync || nonPreedit_.selectionLive ||
        nonPreedit_.document.predicted().empty()) {
        // Handed to the app, but still journaled: the one-character deletion it is
        // about to make is as predictable as our own, and seeing it land is what
        // brings the client back on track. `appPerforms` itself skips the record
        // outside the mode or over a selection; a record made out of sync is safe
        // under the journal — see its comment. With nothing predicted before the
        // caret there is nothing for the app to delete and nothing to predict.
        return appPerforms(1, 0);
    }
    nonPreedit_.noteRepair(1, {});
    nonPreedit_.backspaceJournaled = true;
    return ComposePlan::replace(1, {});
}

bool Composer::adoptWordBeforeBackspace() {
    if (!nonPreedit_.on || !nonPreedit_.retoneAllowed || !effectiveEnabled_) return false;
    // The key deletes the selection, not the last character. A select-all over
    // `tiếng ` leaves the caret at the end, so the reading is unchanged and still
    // confirms the last repair; Backspace then takes everything, and adopting the
    // `tiếng` that popping one character offers would aim the next repair at text
    // that is gone.
    if (nonPreedit_.selectionLive) return false;
    // Only over a Backspace whose deletion is in the journal — so the predicted
    // document below already reads as the app is about to leave it — and only for a
    // client seen to follow what is written. Not the stronger `inSync`: a client that
    // answers a keystroke late never reaches that, and re-opening writes nothing.
    //
    // Why a lagging client is safe here. The word comes from the predicted document,
    // never from the reading: for a client that applies every edit in order, that is
    // its document whatever its lag, so the word is a suffix of what is really there
    // and the next repair deletes within it. A stale reading that happens to equal
    // some prefix — `␣␣`, `⌫⌫` — only ever makes the journal drop edits whose text
    // the reading already contains, and leaves the prediction the same string. A
    // client that dropped a delete holds *more* text than predicted, never less, so
    // the repair can land on the wrong characters but not on missing ones; that
    // repair is marked as following a re-open, and the reading after it catches the
    // drop as RefuseRetone. See nonpreedit/document/predicted.h for the one case the
    // journal cannot tell apart. Chrome, reporting `gox ` a keystroke late as `gox`,
    // used to be offered `go` from that reading and wrote `goõ` on the next `x`.
    if (!nonPreedit_.onTrack || !nonPreedit_.backspaceJournaled) return false;
    nonPreedit_.backspaceJournaled = false;
    std::vector<uint32_t> chars = decodeUtf8(nonPreedit_.document.predicted());

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
