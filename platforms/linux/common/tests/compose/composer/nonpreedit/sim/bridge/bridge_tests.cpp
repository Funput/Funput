// Direct typing through the gnome-shell bridge (sim/bridge/bridge.h), with and without the
// guard of transport/guard.h. Without it the model has to reproduce #640, or it is
// not the bridge; with it the first word in an empty field, and the first after a
// click, is composed in a preedit, and the word after goes straight again.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/bridge/bridge.h"

using namespace funput;
using namespace funput::test;

namespace {

// Press the first key of a word and report the mode that word is typed in.
bool startsWordDirect(BridgeSim &sim, char key) {
    sim.press(ascii(key));
    return sim.composer().nonPreedit();
}

} // namespace

TEST_CASE("without the guard the bridge drops the first repair: #640, symptom 4") {
    // `[` is ơ in Telex advanced and a second `[` takes it back: "delete 1, write `[`".
    // The client is a keystroke late, so the bridge still holds the empty field when
    // that delete arrives, and only the `[` lands.
    BridgeSim sim(1, Transport::Direct, Method::TelexAdvanced);
    CHECK(sim.type("[[") == "ơ[");
    CHECK(sim.composer().nonPreedit()); // nothing stood the mode down; it is simply wrong

    // The same after a click: the bridge has nothing until the client speaks again,
    // and the engine was not told. (A client on time refills it with the first
    // commit; the late one is the one in #640.)
    BridgeSim clicked(1, Transport::Direct, Method::TelexAdvanced);
    clicked.type("abc ");
    clicked.click();
    CHECK(clicked.type("[[") == "abc ơ[");
}

TEST_CASE("with the guard the first word in an empty field is composed in a preedit") {
    BridgeSim brackets(1, Transport::GnomeShell, Method::TelexAdvanced);
    CHECK_FALSE(startsWordDirect(brackets, '['));
    CHECK(brackets.type("[ ") == "[ "); // the boundary commits the finished word

    for (const int lag : {0, 1}) {
        CAPTURE(lag);
        BridgeSim sim(lag);
        CHECK_FALSE(startsWordDirect(sim, 's'));
        CHECK(sim.type("oor ") == "sổ ");
    }
}

TEST_CASE("the next word goes straight once the client has answered") {
    BridgeSim sim(0);
    sim.type("tieengs ");
    REQUIRE(sim.document() == "tiếng ");
    // The commit of the first word is the answer: a prefix the bridge can convert on.
    CHECK(startsWordDirect(sim, 's'));
    CHECK(sim.type("oor ") == "tiếng sổ ");
}

TEST_CASE("after a click the next word waits for the client, and only that one") {
    BridgeSim sim(0);
    sim.type("tieengs soor ");
    REQUIRE(sim.document() == "tiếng sổ ");
    sim.click();
    CHECK_FALSE(sim.spokeSinceReset());
    // Nothing has changed in the document, so the daemon forwards nothing and the
    // shell has heard nothing since the reset: the word is composed in a preedit.
    CHECK_FALSE(startsWordDirect(sim, 's'));
    CHECK(sim.type("oor ") == "tiếng sổ sổ ");
    // That commit changed the document; the answer arrives, and the bridge has a copy
    // again.
    CHECK(startsWordDirect(sim, 'x'));
    CHECK(sim.spokeSinceReset());
}

TEST_CASE("a non-empty prefix is not guarded") {
    // Text already in the field is a copy the bridge can walk back on: every repair
    // deletes within the word being typed, which is shorter than the prefix.
    BridgeSim sim(1);
    sim.seed("abc ");
    CHECK(startsWordDirect(sim, 's'));
    CHECK(sim.type("oor") == "abc sổ");
    CHECK(sim.composer().nonPreedit());
}

TEST_CASE("characters to bytes and back on one snapshot is an identity: RFC §4.5") {
    // `ế` is one character and three bytes; the tone on `tiếng` deletes three
    // characters, one of them two bytes wide. Through the bridge, whether the copy is
    // current or a keystroke old, the app deletes the same three characters, and the
    // document equals the preedit path's.
    const std::string expected = preeditDocument(Method::Telex, "tieengs");
    REQUIRE(expected == "tiếng");
    for (const int lag : {0, 1}) {
        CAPTURE(lag);
        BridgeSim direct(lag, Transport::Direct);
        CHECK(direct.type("tieengs") == expected);
        BridgeSim guarded(lag);
        guarded.seed("x ");
        CHECK(guarded.type("tieengs") == "x " + expected);
    }
}
