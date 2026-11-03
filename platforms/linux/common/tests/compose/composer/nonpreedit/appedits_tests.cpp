// Edits the app performs itself — a space typed with nothing composing, a Backspace
// inside a word — are journaled too, so the evidence that the document is current
// survives them: unrecorded, the second space of `phur␣␣⌫⌫s` broke the chain and the
// `s` was typed plain. What such a record must never claim: appedits_sync_tests.cpp.

#include <doctest/doctest.h>

#include "appedits.h"
#include "compose/composer/nonpreedit/refusal.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("two spaces and two Backspaces later the tone still lands") {
    Shell shell;
    shell.type("phur  ");
    REQUIRE(shell.document == "phủ  ");
    // The second space was journaled, so the reading after it confirms the document
    // and Funput takes the first Backspace over itself.
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
    shell.backspace();
    CHECK_FALSE(shell.adopted); // the caret lands on the comma, not on a word
    shell.type("s");
    CHECK(shell.document == "phủ,s");
}

TEST_CASE("a Backspace inside the word keeps the record") {
    Shell shell;
    shell.type("xin tieengs");
    REQUIRE(shell.document == "xin tiếng");
    // The app deletes and the engine follows, all the way out of the word. Each
    // deletion was journaled and seen to land, so the chain holds: the Backspace that
    // then eats the space is Funput's, and `xin` is re-opened.
    for (int i = 0; i < 5; ++i) CHECK_FALSE(shell.backspace().consumed);
    REQUIRE(shell.document == "xin ");
    CHECK(shell.backspace().consumed);
    CHECK(shell.adopted);
    shell.type("h");
    CHECK(shell.document == "xinh");
}

TEST_CASE("a boundary that is not a space passes through unrecorded") {
    Shell shell;
    shell.type("xin (");
    // The editor paired it: `(` before the caret, `)` after. A record of the `(`
    // would have been confirmed, and the next ⌫ taken over, stranding the `)`.
    CHECK_FALSE(shell.backspace().consumed); // the app removes the pair
    CHECK(shell.document == "xin ");
}

TEST_CASE("a space typed over a selection is not journaled") {
    Shell shell;
    shell.type("xin ");
    // A suffix the app filled in after the caret and selected, with nothing
    // composing. The space replaces it, so the text before the caret then reads
    // exactly what a journaled space would predict: the one selection the guard in
    // `appPerforms` decides, since a highlight before the caret moves the reading.
    shell.selectionLive = true;
    shell.selectedAfter = 1;
    shell.type(" ");
    shell.selectionLive = false;
    shell.selectedAfter = 0;
    REQUIRE(shell.document == "xin  ");
    CHECK_FALSE(shell.backspace().consumed); // no prediction, no proof: the app's
}

TEST_CASE("app-performed edits never convict a client") {
    Shell shell;
    shell.type("phur ");
    // From here the client drops every delete, but is only ever asked for records
    // that cannot produce a verdict: a space that deletes nothing, and deletions that
    // write nothing. The mode stays — out of sync, which is what a dropped delete
    // costs, so the Backspaces that follow are the app's and nothing is adopted.
    shell.client = applyIgnoringDeletes;
    shell.type(" ");
    shell.backspace(); // Funput's, dropped: no verdict
    REQUIRE(shell.document == "phủ  ");
    CHECK_FALSE(shell.backspace().consumed);
    CHECK_FALSE(shell.backspace().consumed);
    CHECK_FALSE(shell.adopted);
    CHECK(shell.document == "phủ");
    CHECK(shell.composer.nonPreedit());
}
