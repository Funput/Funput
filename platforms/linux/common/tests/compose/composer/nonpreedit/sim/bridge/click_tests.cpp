// A click into the focused field on the gnome-shell bridge. It reaches the shell as
// a reset, after mutter has already committed the preedit — which the guard now puts
// the first word of a field into — so the shell must not commit it again
// (Composer::onReset, state.cpp), and the commit is a change the client answers
// like any other. The one difference between the two shells behind the bridge is
// also here: whether an answer equal to the last one is heard (sim/bridge/bridge.h).

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/bridge/bridge.h"

using namespace funput;
using namespace funput::test;

namespace {

constexpr uint32_t kShiftL = 0xFFE1;

bool startsWordDirect(BridgeSim &sim, char key) {
    sim.press(ascii(key));
    return sim.composer().nonPreedit();
}

} // namespace

TEST_CASE("a click commits the guarded word once: mutter does, the shell does not") {
    BridgeSim sim(0);
    sim.type("tieengs");
    REQUIRE_FALSE(sim.composer().nonPreedit()); // the first word of the field: guarded
    REQUIRE(sim.document().empty());            // still in the preedit
    sim.click();
    CHECK(sim.document() == "tiếng");
    CHECK_FALSE(sim.composer().isComposing());
}

TEST_CASE("the word a click committed is answered like any other change") {
    // On time, the answer is there for the next word: the bridge has a copy, and the
    // word goes straight. A keystroke late, it is not there yet, and the next word
    // waits — the answer lands during it, and the word after goes straight.
    BridgeSim onTime(0);
    onTime.type("ti");
    onTime.click();
    REQUIRE(onTime.document() == "ti");
    CHECK(startsWordDirect(onTime, 's'));
    CHECK(onTime.type("oor ") == "tisổ ");

    BridgeSim late(1);
    late.type("ti");
    late.click();
    CHECK_FALSE(startsWordDirect(late, 's'));
    CHECK(late.type("oor ") == "tisổ ");
    CHECK(late.spokeSinceReset());
    CHECK(startsWordDirect(late, 'x'));
}

TEST_CASE("a click with nothing pending commits nothing and is answered by nobody") {
    BridgeSim sim(0);
    sim.type("tieengs ");
    sim.click();
    CHECK(sim.document() == "tiếng ");
    CHECK_FALSE(sim.spokeSinceReset());
    CHECK_FALSE(startsWordDirect(sim, 's'));
}

TEST_CASE("an answer equal to the last one reaches Fcitx5 and not the IBus engine") {
    // The sim's app answers every press, even one that changed nothing. Fcitx5's IBus
    // frontend hears that answer, since libibus sends every new IBus.Text object;
    // ibus-daemon drops it, since its text, cursor and anchor equal the last one it
    // forwarded. So after a click, a word on the IBus engine waits for the document
    // to change; on Fcitx5 it waits for the client to say anything at all.
    for (const Shell shell : {Shell::Fcitx5, Shell::IBus}) {
        CAPTURE(static_cast<int>(shell));
        BridgeSim sim(0, {Transport::GnomeShell, Method::Telex, shell});
        sim.type("tieengs ");
        sim.click();
        REQUIRE_FALSE(sim.spokeSinceReset());
        sim.press(bare(kShiftL)); // changes nothing; answered with the same text
        const bool heard = shell == Shell::Fcitx5;
        CHECK(startsWordDirect(sim, 's') == heard);
        CHECK(sim.spokeSinceReset() == heard);
    }
}
