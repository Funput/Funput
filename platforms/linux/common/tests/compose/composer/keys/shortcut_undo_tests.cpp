// Backspace taking back a gõ tắt expansion (compose/composer/keys/shortcut_undo.cpp),
// driven the way a shell drives it: ClientSim reads the document, runs the key and
// offers the word to re-open, as fcitx5's keyEvent() does. What the undo restores and
// the refusal it leaves are the engine's rules (crates/funput-engine tests); these
// cases pin when Linux trusts the document with it, and that unsure is a plain ⌫.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

namespace {

// `vn` → `Việt Nam`, typed in Vietnamese unless `english`.
void withShortcut(ClientSim &sim, bool english = false) {
    Settings &settings = sim.composer().settings();
    settings.shortcuts = {{"vn", "Việt Nam"}};
    settings.enabled = !english;
    sim.composer().applySettings();
}

} // namespace

TEST_CASE("backspace right after an expansion writes the trigger and its boundary back") {
    ClientSim sim;
    withShortcut(sim);
    REQUIRE(sim.type("vn ") == "Việt Nam ");
    const ComposePlan plan = sim.press(bare(keysym::BackSpace));
    CHECK(plan.effect == Effect::Replace);
    CHECK(plan.consumed);
    CHECK(plan.deleteChars == 9);
    CHECK(sim.document() == "vn ");
    CHECK_FALSE(sim.adopted()); // an undo is not a deletion: nothing is re-opened
}

TEST_CASE("the trigger comes back as it was typed") {
    ClientSim sim;
    withShortcut(sim);
    REQUIRE(sim.type("Vn ") == "Việt Nam ");
    sim.pressBackspace();
    CHECK(sim.document() == "Vn ");
}

TEST_CASE("the trigger taken back stays as typed when typed again at once") {
    ClientSim sim;
    withShortcut(sim);
    sim.type("vn ");
    sim.pressBackspace();
    sim.pressBackspace(); // the space; `vn` is re-opened
    REQUIRE(sim.adopted());
    REQUIRE(sim.document() == "vn");
    CHECK(sim.type(" ") == "vn ");
    CHECK(sim.type("vn ") == "vn Việt Nam "); // once only
}

TEST_CASE("only the backspace straight after the expansion takes it back") {
    ClientSim sim;
    withShortcut(sim);
    sim.type("vn a");
    sim.pressBackspace();
    sim.pressBackspace();
    CHECK(sim.document() == "Việt Nam");
}

TEST_CASE("a click spends the undo") {
    ClientSim sim;
    withShortcut(sim);
    sim.type("vn ");
    sim.click("Việt Nam "); // back where it was, but the caret moved in between
    sim.pressBackspace();
    CHECK(sim.document() == "Việt Nam");
}

TEST_CASE("a client not known to follow the document gets a plain backspace") {
    for (int lag : {1, ClientSim::kSilent}) {
        CAPTURE(lag);
        ClientSim sim(lag);
        withShortcut(sim);
        sim.type("vn ");
        sim.pressBackspace();
        CHECK(sim.document() == "Việt Nam");
        sim.pressBackspace(); // the undo is spent, not merely skipped
        CHECK(sim.document() == "Việt Na");
    }
}

TEST_CASE("over a selection the key deletes the selection") {
    ClientSim sim;
    withShortcut(sim);
    sim.type("vn ");
    sim.selectionLive = true;
    const ComposePlan plan = sim.press(bare(keysym::BackSpace));
    CHECK(plan.effect == Effect::None);
    CHECK_FALSE(plan.consumed);
}

TEST_CASE("English mode takes an expansion back too") {
    ClientSim sim;
    withShortcut(sim, true);
    REQUIRE(sim.type("vn ") == "Việt Nam ");
    sim.pressBackspace();
    CHECK(sim.document() == "vn ");
}

TEST_CASE("a preedit client gets a plain backspace") {
    Settings settings;
    settings.shortcuts = {{"vn", "Việt Nam"}};
    Composer composer(settings);
    composer.setNonPreedit(false);
    std::string document;
    for (char c : std::string("vn ")) applyPlan(document, composer.onKey(ascii(c)), c);
    REQUIRE(document == "Việt Nam ");
    const ComposePlan plan = composer.onKey(bare(keysym::BackSpace));
    CHECK(plan.effect == Effect::None);
    CHECK_FALSE(plan.consumed);
}
