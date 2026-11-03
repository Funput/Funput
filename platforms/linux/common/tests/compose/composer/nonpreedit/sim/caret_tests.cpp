// Keys that move the caret — an arrow, Enter, a shortcut — and the VI/EN toggle
// leave the journal describing a document the caret is no longer at the end of. It
// used to carry on regardless: `phur␣ ← ⌫ s` on a client a key late re-opened
// `phủ` from a prediction the caret had left and wrote `pú␣`. Every such key now
// forgets the journal (NonPreeditState::forget); answered readings start it again.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/client.h"
#include "compose/key/chord.h"

using namespace funput;
using namespace funput::test;

namespace {

constexpr uint32_t kLeft = 0xFF51; // XK_Left, a NonText key

// A client with text after the caret: Left moves one character across it, Enter
// starts a new line, and a click puts the caret somewhere else entirely.
struct CaretSim : ClientSim {
    using ClientSim::ClientSim;
    std::string after;
    void perform(const ComposePlan &plan, const KeyEvent &ev) override {
        if (ev.keysym == kLeft) {
            std::vector<uint32_t> chars = decodeUtf8(document_);
            if (chars.empty()) return;
            std::string last;
            appendUtf8(last, chars.back());
            document_.resize(document_.size() - last.size());
            after = last + after;
        } else if (ev.keysym == keysym::Return) {
            document_.clear();
        } else {
            ClientSim::perform(plan, ev);
        }
    }
    std::string full() const { return document_ + after; }
    std::string run(const std::string &keys) {
        for (char c : keys) (c == '\b') ? void(pressBackspace()) : void(press(ascii(c)));
        return full();
    }
};

} // namespace

TEST_CASE("Backspace after a caret key is the app's and re-opens nothing, at any lag") {
    for (const int lag : {0, 1, 2, 3}) {
        CAPTURE(lag);
        CaretSim sim(lag);
        REQUIRE(sim.run("phur ") == "phủ ");
        sim.press(bare(kLeft)); // the caret sits between `ủ` and the space
        CHECK_FALSE(sim.pressBackspace());
        CHECK_FALSE(sim.adopted());
        CHECK(sim.run("s") == "phs ");
        CHECK(sim.observation().verdict == Verdict::Unknown);
    }
}

TEST_CASE("Enter, a shortcut, the toggle and a flush forget the journal too") {
    CaretSim sim;
    REQUIRE(sim.run("phur ") == "phủ ");
    SUBCASE("nothing in between: the Backspace is Funput's") {
        CHECK(sim.pressBackspace());
        CHECK(sim.adopted());
    }
    SUBCASE("Enter") {
        sim.press(control(keysym::Return, U'\r'));
        CHECK_FALSE(sim.pressBackspace());
        CHECK(sim.observation().pending == 0);
    }
    SUBCASE("a shortcut") {
        sim.press(ctrl('b'));
        CHECK_FALSE(sim.pressBackspace());
        CHECK_FALSE(sim.adopted());
        CHECK(sim.observation().pending == 0);
    }
    SUBCASE("the VI/EN toggle, there and back") {
        sim.press(ctrl('`'));
        sim.press(ctrl('`'));
        REQUIRE(sim.composer().enabled());
        CHECK_FALSE(sim.pressBackspace());
        CHECK_FALSE(sim.adopted());
    }
    SUBCASE("a flush") {
        sim.composer().flush();
        CHECK_FALSE(sim.pressBackspace());
        CHECK_FALSE(sim.adopted());
    }
}

TEST_CASE("a modifier press forgets nothing") {
    // Shift goes down before every capital letter. A client a key late re-opens
    // `phủ` over the Backspace after it only because the journal survived the press
    // — and since the press changed nothing, its late reading even catches up.
    CaretSim sim(1);
    sim.run("phur ");
    sim.press(bare(keysym::ShiftL));
    CHECK(sim.pressBackspace());
    CHECK(sim.adopted());
    CHECK(sim.run("s") == "phú");
}

TEST_CASE("a lagging client catches up again after Enter") {
    // Its first readings after the caret key still show the line it left, so the
    // journal restarts from each in turn, keeping the edits in flight as provisional
    // until a reading lines up with them.
    for (const int lag : {0, 1, 2, 3}) {
        CAPTURE(lag);
        CaretSim sim(lag);
        sim.run("xin ");
        sim.press(control(keysym::Return, U'\r'));
        CHECK(sim.run("phur \bs") == "phú");
        CHECK(sim.observation().verdict == Verdict::Unknown);
        CHECK(sim.composer().nonPreedit());
    }
}

TEST_CASE("a click resets the input method, which forgets the journal like a caret key") {
    // GTK and Qt reset the input method on a click; Fcitx5 answers with a flush.
    // The journal restarts from the new line and earns its trust back from the
    // first text confirmed there.
    for (const int lag : {0, 1, 2}) {
        CAPTURE(lag);
        CaretSim sim(lag);
        sim.run("phur ");
        sim.click("xin"); // the caret is now at the end of another line
        CHECK(sim.run(" chaof ") == "xin chào ");
        CHECK(sim.pressBackspace() == (lag == 0));
        CHECK(sim.adopted());
        CHECK(sim.run("s") == "xin cháo");
        CHECK(sim.observation().verdict == Verdict::Unknown);
    }
}

TEST_CASE("a client that moves the caret without a reset is outside the guarantee") {
    // The one residual predicted.h names. On time it costs re-toning, not the
    // mode: the space's record cannot be placed on the new line (`xin` does not
    // end with it), so the journal restarts on every reading and trusts nothing,
    // until its capacity resets it or a later key happens to spell the record.
    CaretSim sim;
    sim.run("phur ");
    sim.click("xin", /*reset=*/false);
    CHECK(sim.run("chaof,") == "xinchào,");
    CHECK_FALSE(sim.pressBackspace());
    CHECK_FALSE(sim.adopted());
    CHECK(sim.run("s") == "xinchàos");
    CHECK(sim.observation().verdict == Verdict::Unknown);
    CHECK(sim.composer().nonPreedit());
}
