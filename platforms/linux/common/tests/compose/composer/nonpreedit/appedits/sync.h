// A client that answers every keystroke on time — the honest counterpart of the one in
// refusal.h, for the tests that need the composer to trust what it reads.

#ifndef FUNPUT_TESTS_NONPREEDIT_SYNC_H
#define FUNPUT_TESTS_NONPREEDIT_SYNC_H

#include <string>

#include "support.h"

namespace funput::test {

// Type `keys` the way a shell drives the composer: read the document, check it, then
// press the key. Ends with a reading taken, so the next Backspace has the proof
// `adoptWordBeforeBackspace` asks for — that the document it reads is the one the
// last repair left.
inline std::string typeInSync(Composer &composer, const std::string &keys) {
    std::string document;
    for (char c : keys) {
        composer.observeDocument(document);
        applyPlan(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document);
    return document;
}

} // namespace funput::test

#endif // FUNPUT_TESTS_NONPREEDIT_SYNC_H
