// Which word a Backspace offers for re-opening: the scan from the caret back to a
// separator, over the document as the composer predicts it. The rule is shared with
// `CommittedTail` on Windows (crates/funput-desktop/src/retone.rs), so a change to
// it is a change everywhere.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("only a Vietnamese syllable is re-opened") {
    // English words stay literal — the engine refuses them.
    ClientSim english;
    english.type("hello ");
    english.pressBackspace();
    CHECK_FALSE(english.adopted());
    CHECK_FALSE(english.composer().isComposing());

    ClientSim vietnamese;
    vietnamese.type("phur ");
    vietnamese.pressBackspace();
    CHECK(vietnamese.adopted());
    CHECK(vietnamese.composer().isComposing());
}

TEST_CASE("the caret landing on a separator re-opens nothing") {
    ClientSim sim;
    sim.type("phur, ");
    REQUIRE(sim.document() == "phủ, ");
    sim.pressBackspace();
    CHECK_FALSE(sim.adopted());
}

TEST_CASE("nothing in front of the caret re-opens nothing") {
    ClientSim sim;
    sim.pressBackspace();
    CHECK_FALSE(sim.adopted());
    // Nor does the Backspace that empties the field.
    sim.type("a");
    sim.pressBackspace();
    CHECK_FALSE(sim.adopted());
    CHECK(sim.document().empty());
}

TEST_CASE("the word scan splits on punctuation, as the hook shells do") {
    // `github.com` is not one word here: the scan stops at the dot and offers `com`,
    // which really is a Vietnamese syllable, so it is re-opened. `CommittedTail`'s
    // `is_separator` splits the same way on Windows. Pinned because it is a decision
    // shared with the other platforms, not an accident of this one.
    ClientSim sim;
    sim.type("github.com ");
    REQUIRE(sim.document() == "github.com ");
    sim.pressBackspace();
    CHECK(sim.adopted());
}

TEST_CASE("the scan reads the prediction, not the reading") {
    // The client reports the document a keystroke late, so at Backspace it still
    // shows `gox ` as `gox`. Popping a character off *that* offered `go`, a word the
    // document no longer ended with, and the next `x` wrote `goõ`. The prediction
    // knows about the space: the word offered is `gox`, which the engine refuses,
    // and the `x` is typed plain.
    ClientSim late(1);
    late.type("goxx ");
    REQUIRE(late.document() == "gox ");
    late.pressBackspace();
    CHECK_FALSE(late.adopted());
    CHECK(late.type("x") == "goxx");
}

TEST_CASE("re-opening is a non-preedit affair only") {
    Composer composer = composerFor(Method::Telex);
    // A preedit shell has no committed word to re-open; Backspace shortens the
    // composition instead.
    composer.onKey(bare(keysym::BackSpace));
    CHECK_FALSE(composer.adoptWordBeforeBackspace());
}
