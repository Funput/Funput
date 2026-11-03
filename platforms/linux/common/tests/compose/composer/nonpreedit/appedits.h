// The shell the app-edit tests drive the composer through. Shared by
// appedits_tests.cpp (what journaling the app's own edits buys) and
// appedits_sync_tests.cpp (what such a record must never claim), which have to agree
// on how a shell and its client behave or they would be testing different setups.

#ifndef FUNPUT_TESTS_NONPREEDIT_APPEDITS_H
#define FUNPUT_TESTS_NONPREEDIT_APPEDITS_H

#include <string>

#include "support.h"

namespace funput::test {

// Drives the composer in the order fcitx5/src/funput_input.cpp does: read the
// document, decide whether this Backspace may re-open a word, perform the plan, then
// offer the word. `client` performs a plan; the app always deletes its own character
// when a Backspace is handed to it. `lag` reports the document one keystroke late,
// as Chrome does ("a stale reading" in backspace_tests.cpp).
struct Shell {
    Composer composer = composerFor(Method::Telex);
    std::string document;
    std::string reading;
    bool lag = false;
    bool selectionLive = false;
    uint32_t selectedAfter = 0;
    bool adopted = false;
    void (*client)(std::string &, const ComposePlan &, char) = applyPlan;

    Shell() { composer.setNonPreedit(true); }

    ComposePlan press(const KeyEvent &ev, char ch) {
        const std::string before = lag ? reading : document;
        composer.observeDocument(before, selectionLive, true, selectedAfter);
        reading = document;
        const bool reopen = composer.nonPreedit() && !composer.isComposing() &&
                            classify(ev, composer.settings()) == KeyKind::Backspace;
        const ComposePlan plan = composer.onKey(ev);
        if (ev.keysym == keysym::BackSpace && !plan.consumed) {
            popChars(document, 1);
        } else {
            client(document, plan, ch);
        }
        adopted = reopen && composer.adoptWordBeforeBackspace(before);
        return plan;
    }
    ComposePlan backspace() { return press(bare(keysym::BackSpace), '\b'); }
    void type(const std::string &keys) {
        for (char c : keys) press(ascii(c), c);
    }
};

} // namespace funput::test

#endif // FUNPUT_TESTS_NONPREEDIT_APPEDITS_H
