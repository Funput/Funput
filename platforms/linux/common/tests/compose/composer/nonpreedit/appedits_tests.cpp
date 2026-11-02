// Edits the app performs itself — a boundary typed with nothing composing, a
// Backspace handed to the app, a Backspace inside a word — are journaled too, so the
// evidence that the document is current survives them. Unrecorded, the second space
// of `phur␣␣⌫⌫s` broke the chain and the `s` was typed plain.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/refusal.h"
#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

// Drives the composer in the order fcitx5/src/funput_input.cpp does: read the
// document, decide whether this Backspace may re-open a word, perform the plan, then
// offer the word. `client` performs a plan; the app always deletes its own character
// when a Backspace is handed to it.
struct Shell {
    Composer composer = composerFor(Method::Telex);
    std::string document;
    bool selectionLive = false;
    bool adopted = false;
    void (*client)(std::string &, const ComposePlan &, char) = applyPlan;

    Shell() { composer.setNonPreedit(true); }

    ComposePlan press(const KeyEvent &ev, char ch) {
        composer.observeDocument(document, selectionLive);
        const bool reopen = composer.nonPreedit() && !composer.isComposing() &&
                            classify(ev, composer.settings()) == KeyKind::Backspace;
        const std::string before = document;
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

} // namespace

TEST_CASE("two spaces and two Backspaces later the tone still lands") {
    Shell shell;
    shell.type("phur  ");
    REQUIRE(shell.document == "phủ  ");
    // The second space was journaled, so the reading that follows it confirms the
    // document and Funput takes the first Backspace over itself.
    const ComposePlan first = shell.backspace();
    CHECK(first.consumed);
    CHECK(first.deleteChars == 1);
    CHECK_FALSE(shell.adopted); // the caret lands on the remaining space
    shell.backspace();
    CHECK(shell.adopted);
    shell.type("s");
    CHECK(shell.document == "phú");
}

TEST_CASE("punctuation stops the re-open where the word scan stops") {
    Shell shell;
    shell.type("phur, ");
    REQUIRE(shell.document == "phủ, ");
    shell.backspace();
    CHECK_FALSE(shell.adopted); // the caret lands on the comma, not on a word
    shell.type("s");
    CHECK(shell.document == "phủ,s");
}

TEST_CASE("a Backspace handed to the app is still a predicted edit") {
    Shell shell;
    shell.document = "xin chào"; // typed before Funput was watching
    shell.composer.onFocusChanged();
    // Nothing confirms this reading yet, so the app deletes — and that is journaled.
    CHECK_FALSE(shell.backspace().consumed);
    REQUIRE(shell.document == "xin chà");
    // The shortened document is the change predicted, so the next one is Funput's.
    const ComposePlan second = shell.backspace();
    CHECK(second.consumed);
    CHECK(second.deleteChars == 1);
    CHECK(shell.document == "xin ch");
}

TEST_CASE("a Backspace inside the word keeps the record") {
    Shell shell;
    shell.type("xin tieengs");
    REQUIRE(shell.document == "xin tiếng");
    // The app deletes and the engine follows, all the way out of the word.
    for (int i = 0; i < 5; ++i) CHECK_FALSE(shell.backspace().consumed);
    REQUIRE(shell.document == "xin ");
    REQUIRE_FALSE(shell.composer.isComposing());
    // Each of those deletions was journaled and seen to land, so the chain holds:
    // the Backspace that now eats the space is Funput's, and `xin` is re-opened.
    CHECK(shell.backspace().consumed);
    CHECK(shell.adopted);
    shell.type("h");
    CHECK(shell.document == "xinh");
}

TEST_CASE("nothing is journaled under a live selection") {
    Shell shell;
    shell.type("tieengs ");
    shell.selectionLive = true; // the user highlighted the trailing space
    CHECK(shell.backspace().isNoop());
    REQUIRE(shell.document == "tiếng"); // the app deleted the selection
    shell.selectionLive = false;
    // Had that been journaled as a one-character deletion, this reading would have
    // confirmed it. It was not, so there is no proof and the app deletes again.
    CHECK_FALSE(shell.backspace().consumed);
    CHECK_FALSE(shell.adopted);
}

TEST_CASE("adoption is refused while a selection is live") {
    Shell shell;
    shell.type("tieengs ");
    REQUIRE(shell.document == "tiếng ");
    shell.selectionLive = true;
    shell.backspace();
    // The word was selected and deleted whole; adopting `tiến` from the reading
    // would aim the next repair at text that is gone.
    CHECK_FALSE(shell.adopted);
    CHECK_FALSE(shell.composer.isComposing());
}

TEST_CASE("app-performed edits never convict a client") {
    Shell shell;
    shell.type("phur ");
    REQUIRE(shell.document == "phủ ");
    // From here the client drops every delete, but is only ever asked for one of the
    // records that cannot produce a verdict: a space that deletes nothing, and
    // deletions that write nothing. The mode stays, and so does re-toning.
    shell.client = applyIgnoringDeletes;
    shell.type(" ");
    shell.backspace(); // Funput's, dropped: no verdict, out of sync
    CHECK(shell.document == "phủ  ");
    shell.backspace(); // the app's, journaled, and seen to land
    CHECK(shell.document == "phủ ");
    shell.backspace(); // Funput's again, and the word is offered
    CHECK(shell.adopted);
    CHECK(shell.composer.nonPreedit());
}
