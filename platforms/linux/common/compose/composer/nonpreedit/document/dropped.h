// The dropped-delete check: the one comparison that convicts a client, run over every
// unconfirmed edit and over every chain of them. The journal it runs on: predicted.h.
//
// With one edit "delete N, write T" on document `D`, a reading equal to "`D` then
// `T`" says the delete was dropped. With several pending the same holds for each
// edit j against the prefix before it: every earlier edit applied, j's delete
// dropped. A client can also drop deletes in a run — a ⌫ of Funput's dropped, the
// app's ⌫⌫ honoured — and then every reading still matches a prefix, one deletion
// behind the prediction, until the next write lands: that reading is the base with
// every text appended and no delete applied. So each j is also compared with that
// chain, base + T_0 + … + T_j. Both are `RefuseMode`, or `RefuseRetone` when a
// repair that followed a re-opened word is among the edits blamed — the one repair
// Chrome's address bar drops (Verdict::RefuseRetone).
//
// Neither comparison can convict a client that applies every edit in order, however
// late it answers. Its readings are snapshots of its document, and every snapshot
// after the one the base was taken from is some prefix p(k), the base with the first
// k edits applied — PredictedDocument runs this check only when the reading equals
// no prefix. For the reading to equal a chain c(j) all the same, c(j) would have to
// differ from every p(k), in particular from p(j+1), which is c(j) with the deletes
// applied: so one of those deletes removed something, and the client cannot both
// show that text and have applied the delete that removed it. A delete that removes
// nothing — nothing before the caret — makes c(j) equal to p(j+1), which the prefix
// loop matched first. What the check cannot see is a caret the client moved and has
// not reported yet, which is why a caret key resets the journal
// (NonPreeditState::forget), and why a click remains the one residual.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_DROPPED_H
#define FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_DROPPED_H

#include <cstddef>
#include <cstdint>
#include <string>
#include <vector>

#include "compose/composer/nonpreedit/verdict.h"
#include "ffi/utf8.h"

namespace funput {

// One "delete N characters, then write this", Funput's own or the app's.
// `afterAdopt` marks the repair that follows a re-opened word.
struct PredictedEdit {
    uint32_t deleted = 0;
    std::string text;
    bool afterAdopt = false;
};

// Two deletions that write nothing and are the same size: the ambiguous pair.
inline bool sameDeleteOnly(const PredictedEdit &a, const PredictedEdit &b) {
    return a.text.empty() && b.text.empty() && a.deleted == b.deleted;
}

// The document after each of `edits[from..]` in turn, starting from `base`:
// p(0) = base, p(i+1) = p(i) less the edit's deleted characters, then its text.
inline std::vector<std::string> prefixesOf(const std::string &base,
                                           const std::vector<PredictedEdit> &edits, size_t from) {
    std::vector<std::string> prefixes{base};
    for (size_t i = from; i < edits.size(); ++i) {
        prefixes.push_back(dropLast(prefixes.back(), edits[i].deleted) + edits[i].text);
    }
    return prefixes;
}

// The prefix a reading equals, if any: `k` of the edits after the first `skip`
// applied, the skipped ones having gone before the base (PredictedDocument's
// provisional edits). Within a skip the *oldest* equal prefix wins. A stale reading
// can look newer than it is when edits cancel out — `dru` then ⌫ reads `dr`, the
// document two edits back — and taking it as the newer state drops edits a lagging
// client has yet to report, after which nothing it reports matches. Keeping them
// pending is consistent whichever state the client is in, since every later state
// is a prefix of the edits that remain. Across skips the largest wins: a skip is
// the on-time explanation of the last restart, and a client that reads the base
// with the provisional edits taken as new ones has made them already.
struct PrefixMatch {
    bool found = false;
    size_t skip = 0;
    size_t k = 0;
};

inline PrefixMatch bestMatch(const std::string &base, const std::vector<PredictedEdit> &edits,
                             size_t provisional, const std::string &reading) {
    for (size_t skip = provisional + 1; skip-- > 0;) {
        const std::vector<std::string> prefixes = prefixesOf(base, edits, skip);
        for (size_t k = 0; k < prefixes.size(); ++k) {
            if (prefixes[k] == reading) return {true, skip, k};
        }
    }
    return {};
}

// `prefixes` is `prefixesOf(base, edits, from)`. Only meaningful for a `reading`
// that equals none of them: see the header.
inline Verdict droppedDelete(const std::vector<std::string> &prefixes,
                             const std::vector<PredictedEdit> &edits, size_t from,
                             const std::string &reading) {
    std::string chain = prefixes.front();
    bool afterAdopt = false;
    for (size_t j = from; j < edits.size(); ++j) {
        chain += edits[j].text;
        afterAdopt = afterAdopt || edits[j].afterAdopt;
        // This delete alone dropped. Not for an edit that writes nothing: "dropped" is
        // then the unchanged prefix, which is also what a client that has not
        // answered yet reads.
        const std::string &before = prefixes[j - from];
        if (!edits[j].text.empty() && reading == before + edits[j].text &&
            reading != prefixes[j - from + 1]) {
            return edits[j].afterAdopt ? Verdict::RefuseRetone : Verdict::RefuseMode;
        }
        // Every delete up to this one dropped.
        if (reading == chain) return afterAdopt ? Verdict::RefuseRetone : Verdict::RefuseMode;
    }
    return Verdict::Unknown;
}

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_DROPPED_H
