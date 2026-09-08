// The disobedient client both refusal test files drive the composer against.
//
// Shared rather than copied: verify_tests.cpp proves the verdict is *reached*, and
// clients_tests.cpp proves it is *remembered*, and the two must agree on what
// misbehaviour looks like or they would be testing different clients.

#ifndef FUNPUT_TESTS_NONPREEDIT_REFUSAL_H
#define FUNPUT_TESTS_NONPREEDIT_REFUSAL_H

#include <string>

#include "support.h"

namespace funput::test {

// A client that takes every commit and drops every delete — Chrome's address bar, and
// Cursor's editor. `phủ` comes back as `phủú`, which is the whole reason non-preedit
// checks its work afterwards.
inline void applyIgnoringDeletes(std::string &document, const ComposePlan &plan, char key) {
    if (plan.effect != Effect::Replace) {
        applyPlan(document, plan, key);
        return;
    }
    document += plan.text;
}

// Type until the mode or re-toning is refused, feeding the client each plan. Returns
// the document as that client left it.
inline std::string typeUntilRefused(Composer &composer, const std::string &keys,
                                    void (*client)(std::string &, const ComposePlan &, char)) {
    std::string document;
    for (char c : keys) {
        composer.observeDocument(document);
        if (!composer.nonPreedit()) break;
        client(document, composer.onKey(ascii(c)), c);
    }
    composer.observeDocument(document);
    return document;
}

} // namespace funput::test

#endif // FUNPUT_TESTS_NONPREEDIT_REFUSAL_H
