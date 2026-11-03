// Edits the app performs itself — a space typed with nothing composing, a Backspace
// handed to the app, a Backspace inside a word — are journaled too, so the evidence
// that the document is being followed survives them: unrecorded, the second space of
// `phur␣␣⌫⌫s` broke the chain and the `s` was typed plain. What is never journaled,
// and what such a record must never claim: sync_tests.cpp.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

namespace {

ComposePlan backspace(ClientSim &sim) {
    return sim.press(bare(keysym::BackSpace));
}

// An on-time client that can be told to start dropping every delete.
struct Shell : ClientSim {
    bool dropsDeletes = false;
    void perform(const ComposePlan &plan, const KeyEvent &ev) override {
        if (!dropsDeletes || plan.effect != Effect::Replace) return ClientSim::perform(plan, ev);
        document_ += plan.text;
    }
};

} // namespace

TEST_CASE("two spaces and two Backspaces later the tone still lands") {
    ClientSim shell;
    REQUIRE(shell.type("phur  ") == "phủ  ");
    // The second space was journaled, so the reading that follows it confirms the
    // document and Funput takes the first Backspace over itself.
    const ComposePlan first = backspace(shell);
    CHECK(first.consumed);
    CHECK(first.deleteChars == 1);
    CHECK_FALSE(shell.adopted()); // the caret lands on the remaining space
    backspace(shell);
    CHECK(shell.adopted());
    CHECK(shell.type("s") == "phú");
}

TEST_CASE("punctuation stops the re-open where the word scan stops") {
    ClientSim shell;
    REQUIRE(shell.type("phur, ") == "phủ, ");
    backspace(shell);
    CHECK_FALSE(shell.adopted()); // the caret lands on the comma, not on a word
    CHECK(shell.type("s") == "phủ,s");
}

TEST_CASE("a Backspace handed to the app is still a predicted edit") {
    ClientSim shell;
    shell.seed("xin chào"); // typed before Funput was watching
    // Nothing confirms this reading yet, so the app deletes — and that is journaled.
    CHECK_FALSE(backspace(shell).consumed);
    REQUIRE(shell.document() == "xin chà");
    // The shortened document is the change predicted, so the next one is Funput's.
    const ComposePlan second = backspace(shell);
    CHECK(second.consumed);
    CHECK(second.deleteChars == 1);
    CHECK(shell.document() == "xin ch");
}

TEST_CASE("a Backspace inside the word keeps the record") {
    ClientSim shell;
    REQUIRE(shell.type("xin tieengs") == "xin tiếng");
    // The app deletes and the engine follows, all the way out of the word.
    for (int i = 0; i < 5; ++i) CHECK_FALSE(backspace(shell).consumed);
    REQUIRE(shell.document() == "xin ");
    REQUIRE_FALSE(shell.composer().isComposing());
    // Each of those deletions was journaled and seen to land, so the chain holds:
    // the Backspace that now eats the space is Funput's, and `xin` is re-opened.
    CHECK(backspace(shell).consumed);
    CHECK(shell.adopted());
    CHECK(shell.type("h") == "xinh");
}

TEST_CASE("nothing is journaled under a live selection") {
    ClientSim shell;
    shell.type("tieengs ");
    shell.selectionLive = true; // the user highlighted the trailing space
    CHECK(backspace(shell).isNoop());
    REQUIRE(shell.document() == "tiếng"); // the app deleted the selection
    shell.selectionLive = false;
    // Had that been journaled as a one-character deletion, this reading would have
    // confirmed it. It was not, so there is no proof and the app deletes again.
    CHECK_FALSE(backspace(shell).consumed);
    CHECK_FALSE(shell.adopted());
}

TEST_CASE("adoption is refused while a selection is live") {
    ClientSim shell;
    REQUIRE(shell.type("tieengs ") == "tiếng ");
    // Select-all leaves the caret at the end: the reading is unchanged and confirms
    // the last repair, so only the selection flag stands between Backspace and the
    // `tiếng` that popping one character offers — a word the key is about to delete.
    shell.selectionLive = true;
    CHECK(backspace(shell).isNoop()); // the app deletes the selection
    CHECK_FALSE(shell.adopted());
    CHECK_FALSE(shell.composer().isComposing());
}

TEST_CASE("app-performed edits never convict a client") {
    // `tiếng`, whose truncation `tiến` is a syllable the engine would take.
    Shell shell;
    REQUIRE(shell.type("tieengs ") == "tiếng ");
    // From here the client drops every delete, but is only ever asked for one of the
    // records that cannot produce a verdict: a space that deletes nothing, and
    // deletions that write nothing. The mode stays, and so does re-toning.
    shell.dropsDeletes = true;
    shell.type(" ");
    backspace(shell); // Funput's, dropped: no verdict, out of sync
    CHECK(shell.document() == "tiếng  ");
    backspace(shell); // the app's, journaled, and seen to land
    CHECK(shell.document() == "tiếng ");
    // That reading cannot tell "the first deletion dropped" from "the second still
    // pending" (nonpreedit/document/predicted.h), so this Backspace is the app's as
    // well — a takeover needs every edit confirmed — and no word is offered from a
    // prediction one deletion ahead: `tiến` while the document reads `tiếng`. Still
    // no verdict, and still no RefuseRetone; what happens at the next write is
    // sim/dropped_tests.cpp.
    CHECK_FALSE(backspace(shell).consumed);
    CHECK_FALSE(shell.adopted());
    CHECK_FALSE(shell.composer().isComposing());
    CHECK(shell.document() == "tiếng");
    CHECK(shell.composer().nonPreedit());
    CHECK(shell.observation().verdict == Verdict::Unknown);
}
