// Which prefix of the journal a reading equals. The journal itself: predicted.h;
// how far to believe a match: trust.h; the verdict a reading that matches nothing
// earns: dropped.h.
//
// After a restart the first `provisional` pending edits may already be in the base:
// a client answering on time after a few keys of silence read a document those
// edits were in, while a client answering late read one from before them. So a
// reading is compared with the prefixes of base + edits[skip..] for every skip up
// to `provisional`, and a skip is taken only when it is *plausible*: edits applied
// in order just before the base leave a suffix the base must end with (`m`, `n`,
// `o` typed before a reading of `` cannot have been). The true explanation for a
// client that applies its edits in order is always plausible; what the check
// removes is the coincidence the review found convicting honest clients — a
// stale reading taken for a later state because a pending edit happened to spell
// the difference, after which the edits it erased came back as "dropped".
//
// Among plausible matches the fewest edits applied wins. Within one skip that is
// the oldest prefix, and it is always consistent: two prefixes of the same skip
// that read alike differ by edits that cancel out, so keeping them pending costs
// nothing a later reading cannot settle. Across skips it is not: a match at skip 0
// and one at skip 2 tell different stories about the base, and only later readings
// can say which. Such a match is reported `contested`, and the journal does not
// trust its base on it (trust.h).

#ifndef FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_MATCH_H
#define FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_MATCH_H

#include <cstddef>
#include <string>
#include <vector>

#include "compose/composer/nonpreedit/document/dropped.h"
#include "ffi/utf8.h"

namespace funput {

struct PrefixMatch {
    bool found = false;
    size_t skip = 0;  // edits applied before the base was read
    size_t k = 0;     // edits applied after it, confirmed by this reading
    bool contested = false; // a plausible match at another skip leaves other edits pending
    size_t next() const { return skip + k; } // how many edits the reading settles
};

// The suffix `edits[0..skip)` leave on any document they are applied to in order.
// A deletion that reaches past the suffix eats into text the base never showed,
// which is allowed: nothing is known about it.
inline std::string suffixLeftBy(const std::vector<PredictedEdit> &edits, size_t skip) {
    std::string suffix;
    for (size_t i = 0; i < skip; ++i) suffix = dropLast(suffix, edits[i].deleted) + edits[i].text;
    return suffix;
}

inline bool endsWith(const std::string &text, const std::string &suffix) {
    return text.size() >= suffix.size() &&
           text.compare(text.size() - suffix.size(), suffix.size(), suffix) == 0;
}

inline PrefixMatch bestMatch(const std::string &base, const std::vector<PredictedEdit> &edits,
                             size_t provisional, const std::string &reading) {
    PrefixMatch best;
    for (size_t skip = 0; skip <= provisional; ++skip) {
        if (!endsWith(base, suffixLeftBy(edits, skip))) continue;
        const std::vector<std::string> prefixes = prefixesOf(base, edits, skip);
        for (size_t k = 0; k < prefixes.size(); ++k) {
            if (prefixes[k] != reading) continue;
            const PrefixMatch match{true, skip, k};
            if (!best.found || match.next() < best.next()) {
                // Contested by the previous best unless both settle the same edits.
                const bool contested = best.found && best.next() != match.next();
                best = match;
                best.contested = contested;
            } else if (match.next() != best.next()) {
                best.contested = true;
            }
            break; // the oldest prefix of this skip
        }
    }
    return best;
}

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_DOCUMENT_MATCH_H
