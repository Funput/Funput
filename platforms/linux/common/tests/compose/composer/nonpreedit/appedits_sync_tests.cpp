// What a record of an edit the app performs must never claim, and which app edits
// are never recorded. A record journaled out of sync vouches for nothing on its own:
// a client one keystroke late reads the document as it stood before the previous
// key, and two identical edits in a row make that stale reading equal what the
// newest record predicts. With one record that marked the document current one
// edit early and the word adopted from it was one character too long; the journal
// reads the same reading as prefix n−1 of n, so the app still deletes and the word,
// when there is one, is the one the document really ends with.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("a Backspace handed to the app proves nothing to a late client") {
    ClientSim shell(1);
    shell.seed("xin chào"); // typed before Funput was watching
    // Both readings are the field as typing found it: the app deletes twice, and
    // nothing is re-opened, since no record was seen to land. The next word earns
    // the evidence back, and the Backspace after it — still the app's, a key late —
    // re-opens `chao` from the prediction.
    CHECK_FALSE(shell.pressBackspace());
    CHECK_FALSE(shell.adopted());
    CHECK_FALSE(shell.pressBackspace());
    CHECK_FALSE(shell.adopted());
    REQUIRE(shell.document() == "xin ch");
    shell.type("ao ");
    CHECK_FALSE(shell.pressBackspace());
    CHECK(shell.adopted());
    CHECK(shell.type("f") == "xin chào");
}

TEST_CASE("the app's own edits never catch a late client up") {
    ClientSim late(1);
    SUBCASE("a space after a space") {
        late.type("phur  ");
        CHECK_FALSE(late.pressBackspace()); // reads `phủ `: not current, the app's
        CHECK_FALSE(late.adopted()); // from a reading one character too long: `phủ`
        CHECK(late.type("s") == "phủ s");
    }
    SUBCASE("a Backspace after a Backspace") {
        late.type("phur ab");
        for (int i = 0; i < 4; ++i) CHECK_FALSE(late.pressBackspace());
        REQUIRE(late.document() == "ph");
        // The same keys on time, which is the document the user is looking at.
        ClientSim onTime(0);
        onTime.type("phur ab");
        for (int i = 0; i < 4; ++i) onTime.pressBackspace();
        REQUIRE(onTime.document() == "ph");
        CHECK(late.type("s") == onTime.type("s"));
        CHECK(late.document() != "pú"); // adopted from the stale reading: `phủ`
    }
}

TEST_CASE("a boundary that is not a space passes through unrecorded") {
    ClientSim shell;
    shell.type("xin (");
    // The editor paired it: `(` before the caret, `)` after. A record of the `(`
    // would have been confirmed, and the next ⌫ taken over, stranding the `)`.
    CHECK_FALSE(shell.pressBackspace()); // the app removes the pair
    CHECK(shell.document() == "xin ");
}

TEST_CASE("a space typed over a selection is not journaled") {
    ClientSim shell;
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
    REQUIRE(shell.document() == "xin  ");
    CHECK_FALSE(shell.pressBackspace()); // no prediction, no proof: the app's
}
