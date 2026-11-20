// Enter and Tab expanding gõ tắt (compose/composer/keys/trigger.cpp). The expansion
// rules are the engine's (crates/funput-engine tests, `expand_shortcut`); these cases
// pin the switches, which keys ask, and that the key still reaches the app in both
// the preedit and the direct-typing mode.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

namespace {

KeyEvent enter() { return control(keysym::Return, U'\r'); }
KeyEvent tab() { return control(keysym::Tab, U'\t'); }

KeyEvent withShift(KeyEvent ev) {
    ev.mods.shift = true;
    return ev;
}

// `ct` → `cơm tấm`, with both switches as given.
void withShortcut(Composer &composer, bool onEnter, bool onTab) {
    Settings &settings = composer.settings();
    settings.shortcuts = {{"ct", "cơm tấm"}};
    settings.shortcutOnEnter = onEnter;
    settings.shortcutOnTab = onTab;
    composer.applySettings();
}

} // namespace

TEST_CASE("with the switches off, Enter and Tab commit the trigger as typed") {
    for (const KeyEvent &key : {enter(), tab()}) {
        Composer composer = composerFor(Method::Telex);
        withShortcut(composer, false, false);
        type(composer, "ct");
        const ComposePlan plan = composer.onKey(key);
        CHECK(plan.effect == Effect::Commit);
        CHECK(plan.text == "ct");
        CHECK_FALSE(plan.consumed);
    }
}

TEST_CASE("over a preedit, the expansion replaces it and the key goes on") {
    for (const KeyEvent &key : {enter(), withShift(enter()), tab()}) {
        Composer composer = composerFor(Method::Telex);
        withShortcut(composer, true, true);
        type(composer, "ct");
        const ComposePlan plan = composer.onKey(key);
        CHECK(plan.effect == Effect::Commit);
        CHECK(plan.text == "cơm tấm"); // no boundary character: the key is its own
        CHECK_FALSE(plan.consumed);
        CHECK_FALSE(composer.isComposing());
    }
}

TEST_CASE("typed straight, the expansion repairs the word and the key goes on") {
    ClientSim sim;
    withShortcut(sim.composer(), true, false);
    sim.type("ct");
    const ComposePlan plan = sim.press(enter());
    CHECK(plan.effect == Effect::Replace);
    CHECK(plan.deleteChars == 2);
    CHECK_FALSE(plan.consumed);
    CHECK(sim.document() == "cơm tấm\r"); // the app typed the newline itself
}

TEST_CASE("each key follows its own switch") {
    Composer composer = composerFor(Method::Telex);
    withShortcut(composer, false, true);
    type(composer, "ct");
    CHECK(composer.onKey(enter()).text == "ct");
    type(composer, "ct");
    CHECK(composer.onKey(tab()).text == "cơm tấm");
}

TEST_CASE("Shift+Tab and Ctrl+Enter never expand") {
    KeyEvent ctrlEnter = enter();
    ctrlEnter.mods.ctrl = true;
    for (const KeyEvent &key : {withShift(tab()), ctrlEnter}) {
        Composer composer = composerFor(Method::Telex);
        withShortcut(composer, true, true);
        type(composer, "ct");
        CHECK(composer.onKey(key).text == "ct");
    }
}

TEST_CASE("a word that is no trigger is committed as before") {
    Composer composer = composerFor(Method::Telex);
    withShortcut(composer, true, true);
    type(composer, "tieengs");
    const ComposePlan plan = composer.onKey(enter());
    CHECK(plan.text == "tiếng");
    CHECK_FALSE(plan.consumed);
}

TEST_CASE("English mode expands too when gõ tắt is allowed there") {
    Composer composer = composerFor(Method::Telex);
    composer.settings().enabled = false;
    withShortcut(composer, true, false);
    REQUIRE_FALSE(composer.enabled());
    type(composer, "ct");
    CHECK(composer.onKey(enter()).text == "cơm tấm");
}

TEST_CASE("an Enter that expanded still opens a sentence on the next line") {
    Composer composer = composerFor(Method::Telex);
    composer.settings().autoCapitalize = true;
    withShortcut(composer, true, false);
    type(composer, "ct");
    REQUIRE(composer.onKey(enter()).text == "cơm tấm");
    CHECK(type(composer, "a").text == "A");
}

TEST_CASE("Backspace after an Enter expansion deletes, it does not take it back") {
    ClientSim sim;
    withShortcut(sim.composer(), true, false);
    sim.type("ct");
    sim.press(enter());
    CHECK_FALSE(sim.pressBackspace()); // the app's own delete, of the newline
    CHECK(sim.document() == "cơm tấm");
}
