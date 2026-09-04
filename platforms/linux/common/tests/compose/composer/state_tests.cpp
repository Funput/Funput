// The composer's non-typing surface: VI/EN and the two ways a composition can
// end without a key.

#include <doctest/doctest.h>

#include "support.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("flush commits the composing word without swallowing anything") {
    Composer composer = composerFor(Method::Telex);
    type(composer, "tieengs");

    const ComposePlan plan = composer.flush();
    CHECK(plan.effect == Effect::Commit);
    CHECK(plan.text == "tiếng");
    CHECK_FALSE(plan.consumed);

    CHECK(composer.flush().text.empty()); // the composition is over
}

TEST_CASE("flush with nothing composing still clears the preedit") {
    Composer composer = composerFor(Method::Telex);
    const ComposePlan plan = composer.flush();
    // Commit with no text is how a cancelled composition ends: drop the preedit,
    // type nothing.
    CHECK(plan.effect == Effect::Commit);
    CHECK(plan.text.empty());
}

TEST_CASE("discard drops the word instead of committing it") {
    Composer composer = composerFor(Method::Telex);
    type(composer, "tieengs");

    composer.discard();
    // Nothing is left to commit — on focus loss the framework already flushed the
    // preedit, and committing again would type the word twice.
    CHECK(composer.flush().text.empty());
}

TEST_CASE("the toggle commits, flips VI/EN, and mirrors into settings") {
    Composer composer = composerFor(Method::Telex);
    CHECK(composer.enabled());
    type(composer, "tieengs");

    const ComposePlan plan = composer.onKey(ctrl('`'));
    CHECK(plan.effect == Effect::Commit);
    CHECK(plan.text == "tiếng"); // the word was typed in the mode that is ending
    CHECK(plan.consumed);
    CHECK_FALSE(composer.enabled());
    CHECK_FALSE(composer.settings().enabled);
}

TEST_CASE("keys pass straight through while English is on") {
    Composer composer = composerFor(Method::Telex);
    composer.onKey(ctrl('`'));
    REQUIRE_FALSE(composer.enabled());

    CHECK(composer.onKey(ascii('t')).isNoop());
    CHECK(composer.onKey(ascii(' ')).isNoop());
    CHECK(composer.onKey(bare(keysym::BackSpace)).isNoop());

    // The toggle itself keeps working — it is the way back to Vietnamese.
    composer.onKey(ctrl('`'));
    CHECK(composer.enabled());
    CHECK(composer.onKey(ascii('t')).text == "t");
}

TEST_CASE("the flip hotkey is inert while English is on") {
    Settings settings;
    settings.flipHotkey = FlipHotkey::CtrlShiftZ;
    Composer composer(settings);
    composer.onKey(ctrl('`'));
    REQUIRE_FALSE(composer.enabled());

    CHECK(composer.onKey(ctrlShift('z')).isNoop());
}

TEST_CASE("applySettings pushes the method through to the engine") {
    Settings settings;
    settings.method = Method::Vni;
    Composer composer(settings);
    CHECK(type(composer, "a1").text == "á"); // VNI: digit is a tone

    composer.settings().method = Method::Telex;
    composer.applySettings();
    // Telex spells that tone with 's', so the digit is now literal.
    CHECK(type(composer, "as").text == "á");
}

TEST_CASE("applySettings resets the runtime VI/EN state to the stored one") {
    Composer composer = composerFor(Method::Telex);
    composer.onKey(ctrl('`'));
    REQUIRE_FALSE(composer.enabled());

    composer.settings().enabled = true;
    composer.applySettings();
    CHECK(composer.enabled());
}

TEST_CASE("gõ tắt expansions survive a settings push") {
    Settings settings;
    settings.method = Method::Telex;
    settings.shortcuts = {{"vn", "Việt Nam"}};
    Composer composer(settings);

    // The trigger expands when the word ends.
    const ComposePlan plan = type(composer, "vn ");
    CHECK(plan.effect == Effect::Commit);
    CHECK(plan.text == "Việt Nam ");
}

// The matching rules themselves belong to the engine and are tested there
// (crates/funput-engine/tests/words/shortcut.rs). What only this layer can prove is
// that applySettings() pushes each switch across the C ABI at all — before it did,
// neither field reached the engine however the user set it.
TEST_CASE("shortcutsEnabled off is pushed to the engine") {
    Settings settings;
    settings.method = Method::Telex;
    settings.shortcuts = {{"vn", "Việt Nam"}};
    settings.shortcutsEnabled = false;
    Composer composer(settings);

    CHECK(typeDocument(composer, "vn ") == "vn ");
}

TEST_CASE("shortcutSmartCase off is pushed to the engine") {
    Settings settings;
    settings.method = Method::Telex;
    settings.shortcuts = {{"vn", "Việt Nam"}};
    settings.shortcutSmartCase = false;
    Composer composer(settings);

    // The stored trigger still expands; a differently-cased one no longer does.
    CHECK(typeDocument(composer, "vn ") == "Việt Nam ");
    CHECK(typeDocument(composer, "Vn ") == "Vn ");
}
