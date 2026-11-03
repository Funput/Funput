// A client that drops Funput's delete-only Backspace but honours the app's own.
// Every reading after that still matches a prefix, one deletion behind the
// prediction: `tieengs␣␣⌫⌫⌫` used to re-open `tiến` while the document read
// `tiếng`, and the `f` that followed wrote `tiếngền` with no verdict. Now the
// ambiguous acknowledgement refuses the re-open, and the dropped-delete chain
// (nonpreedit/document/dropped.h) convicts the client at its next write.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

namespace {

// On time; takes every commit and, once told to, drops every delete.
struct DropsDeletes : ClientSim {
    bool drops = false;
    void perform(const ComposePlan &plan, const KeyEvent &ev) override {
        if (!drops || plan.effect != Effect::Replace) return ClientSim::perform(plan, ev);
        document_ += plan.text;
    }
};

} // namespace

TEST_CASE("a prediction one deletion ahead re-opens nothing and is caught at the next write") {
    DropsDeletes sim;
    REQUIRE(sim.type("tieengs  ") == "tiếng  ");
    sim.drops = true;
    CHECK(sim.pressBackspace()); // Funput's, dropped
    REQUIRE(sim.document() == "tiếng  ");
    CHECK_FALSE(sim.pressBackspace()); // the app's: lands
    REQUIRE(sim.document() == "tiếng ");
    // This reading reads the dropped delete as applied and the app's as pending; the
    // prediction is `tiến` while the document is `tiếng`, so no word is offered.
    CHECK_FALSE(sim.pressBackspace());
    CHECK_FALSE(sim.adopted());
    CHECK_FALSE(sim.composer().isComposing());
    REQUIRE(sim.document() == "tiếng");
    CHECK(sim.observation().verdict == Verdict::Unknown);
    // The next write shows the base with every text appended and no delete applied.
    CHECK(sim.type("f") == "tiếngf");
    sim.type(" ");
    CHECK(sim.observation().verdict == Verdict::RefuseMode);
    CHECK_FALSE(sim.composer().nonPreedit());
}

TEST_CASE("the ambiguity clears once a write is confirmed") {
    // The same shape on an honest on-time client: both Backspaces are its own and
    // are confirmed one reading apart, so nothing is ambiguous and the word is
    // re-opened as before.
    ClientSim sim;
    REQUIRE(sim.type("tieengs  ") == "tiếng  ");
    CHECK(sim.pressBackspace());
    CHECK(sim.pressBackspace());
    CHECK(sim.adopted());
    CHECK(sim.type("f") == "tiềng");
}
