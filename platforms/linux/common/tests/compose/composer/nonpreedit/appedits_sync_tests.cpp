// A record of an edit the app performs may keep the evidence that the document is
// current, never create it. Journaled while out of sync it would vouch for a stale
// reading, Funput would take a Backspace over against a document one edit behind the
// real one, and the word adopted from that reading re-toned the wrong text.

#include <doctest/doctest.h>

#include "appedits.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("a Backspace handed to the app proves nothing") {
    Shell shell;
    shell.document = "xin chào"; // typed before Funput was watching
    shell.composer.onFocusChanged();
    // No repair confirmed this reading, so the app deletes — and that is not
    // journaled, since a record made out of sync can only vouch for a stale reading.
    // The next Backspace is the app's too; sync is earned back by a repair Funput
    // writes, which the next word brings.
    CHECK_FALSE(shell.backspace().consumed);
    CHECK_FALSE(shell.backspace().consumed);
    CHECK_FALSE(shell.adopted);
    REQUIRE(shell.document == "xin ch");
    shell.type("ao ");
    CHECK(shell.backspace().consumed);
    CHECK(shell.adopted); // `chao`
    shell.type("f");
    CHECK(shell.document == "xin chào");
}

TEST_CASE("adoption is refused while a selection is live") {
    Shell shell;
    shell.type("tieengs ");
    REQUIRE(shell.document == "tiếng ");
    // Select-all leaves the caret at the end: the reading is unchanged and confirms
    // the last repair, so only the selection flag stands between Backspace and the
    // `tiếng` that popping one character offers — a word the key is about to delete.
    shell.selectionLive = true;
    CHECK(shell.backspace().isNoop()); // the app deletes the selection
    CHECK_FALSE(shell.adopted);
    CHECK_FALSE(shell.composer.isComposing());
}

// A client one keystroke late reads the document as it stood before the previous
// key. Two identical edits in a row make that stale reading equal what a record of
// the second would predict, so a record made out of sync would mark the document
// current one edit early — and the word adopted from it one character too long.
TEST_CASE("the app's own edits never catch a late client up") {
    Shell shell;
    shell.lag = true;
    SUBCASE("a space after a space") {
        shell.type("phur  ");
        CHECK_FALSE(shell.backspace().consumed); // reads `phủ `: not current, the app's
        CHECK_FALSE(shell.adopted); // journaled out of sync, `phủ` was: `phủú`
        shell.type("s");
        CHECK(shell.document == "phủ s");
    }
    SUBCASE("a Backspace after a Backspace") {
        shell.type("phur ab");
        for (int i = 0; i < 4; ++i) CHECK_FALSE(shell.backspace().consumed);
        CHECK_FALSE(shell.adopted); // journaled out of sync, `phủ` was: `pú`
        shell.type("s");
        CHECK(shell.document == "phs");
    }
}
