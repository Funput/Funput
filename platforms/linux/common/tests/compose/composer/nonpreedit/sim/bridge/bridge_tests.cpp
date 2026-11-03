// Direct typing through the gnome-shell bridge (sim/bridge/bridge.h), with and
// without the guard of transport/guard.h. Without it the model has to reproduce
// #640, or it is not the bridge; with it the first word in an empty field, and the
// first after a click, is composed in a preedit, and the word after goes straight
// again. Every guarded case here is one the unguarded sim gets wrong — the mode, or
// the text — so a guard that did nothing could not pass it.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/bridge/bridge.h"

using namespace funput;
using namespace funput::test;

namespace {

constexpr BridgeOptions kUnguarded{Transport::Direct, Method::Telex};
constexpr BridgeOptions kUnguardedAdvanced{Transport::Direct, Method::TelexAdvanced};
constexpr BridgeOptions kAdvanced{Transport::GnomeShell, Method::TelexAdvanced};

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
    BridgeSim sim(1, kUnguardedAdvanced);
    CHECK(sim.type("[[") == "ơ[");
    CHECK(sim.composer().nonPreedit()); // nothing stood the mode down; it is simply wrong

    // The same after a click: the bridge has nothing until the client speaks again,
    // and the engine was not told. (A client on time refills it with the first
    // commit; the late one is the one in #640.)
    BridgeSim clicked(1, kUnguardedAdvanced);
    clicked.type("abc ");
    clicked.click();
    CHECK(clicked.type("[[") == "abc ơ[");

    // A plain word survives a client one keystroke late — its first repair comes at
    // the third key, when the bridge holds `s` — and not a client two late. (#640's
    // `sôổ` is a lag this model does not produce; it duplicates differently.)
    BridgeSim late(1, kUnguarded);
    CHECK(late.type("soor") == "sổ");
    BridgeSim later(2, kUnguarded);
    CHECK(later.type("soor") == "soổ");
}

TEST_CASE("with the guard the first word in an empty field is composed in a preedit") {
    // The two unguarded failures above, guarded: the mode is off for that word, and
    // the text is right once the boundary commits it.
    BridgeSim brackets(1, kAdvanced);
    CHECK_FALSE(startsWordDirect(brackets, '['));
    CHECK(brackets.type("[ ") == "[ ");

    BridgeSim later(2);
    CHECK_FALSE(startsWordDirect(later, 's'));
    CHECK(later.type("oor ") == "sổ ");

    // On time and a keystroke late the text was right anyway; the mode still is not.
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
    for (const Shell shell : {Shell::Fcitx5, Shell::IBus}) {
        CAPTURE(static_cast<int>(shell));
        BridgeSim sim(0, {Transport::GnomeShell, Method::Telex, shell});
        sim.type("tieengs soor ");
        REQUIRE(sim.document() == "tiếng sổ ");
        sim.click();
        CHECK_FALSE(sim.spokeSinceReset());
        // The click changed nothing: the only answer was GTK's, before the reset. The
        // shell has heard nothing since, and the word is composed in a preedit.
        CHECK_FALSE(startsWordDirect(sim, 's'));
        CHECK(sim.type("oor ") == "tiếng sổ sổ ");
        // That commit changed the document; the answer arrives, and the bridge has a
        // copy again. On either path: behind ibus-daemon too, since it differs.
        CHECK(startsWordDirect(sim, 'x'));
        CHECK(sim.spokeSinceReset());
    }
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

TEST_CASE("the sim converts on one snapshot, so its round trip is an identity: RFC §4.5") {
    // `ế` is one character and three bytes; the tone on `tiếng` deletes three
    // characters, one of them two bytes wide. The sim hands mutter and GTK the same
    // copy, so the byte count mutter produces is the character count GTK reads back,
    // whether that copy is current or a keystroke old. This pins that modelling
    // choice (§4.5 argues the real hops agree too). It does not model the race the
    // argument leaves open: a commit in flight between the two hops, so that GTK
    // reads the bytes on a copy mutter never saw.
    const std::string expected = preeditDocument(Method::Telex, "tieengs");
    REQUIRE(expected == "tiếng");
    for (const int lag : {0, 1}) {
        CAPTURE(lag);
        BridgeSim direct(lag, kUnguarded);
        CHECK(direct.type("tieengs") == expected);
        BridgeSim guarded(lag);
        guarded.seed("x ");
        CHECK(guarded.type("tieengs") == "x " + expected);
    }
}
