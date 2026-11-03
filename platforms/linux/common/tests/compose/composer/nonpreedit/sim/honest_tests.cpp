// Clients that apply every edit in order but answer late, in bursts, or only when
// something changed. None of them may ever be convicted, and silence may never
// move the journal: a reading the shell only cached is the previous answer again
// (RFC §6.1, invariant 3). The review's brute force found an honest client with a
// few keys of silence convicted 14 times in 20000 runs under the old capacity fold;
// the same search runs here, bounded.

#include <doctest/doctest.h>

#include <functional>

#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

namespace {

// On time, but answering only on the keys `answers` says; otherwise the shell hands
// back the last answer it cached, as Fcitx5 and IBus do.
struct Flaky : ClientSim {
    std::function<bool(int)> answers;
    int key = 0;
    std::string cached;
    std::string answer(bool &answered) override {
        answered = answers(++key);
        if (answered) cached = document_;
        return cached;
    }
};

// `lag` keys late, and answering only when the text it would report changed — what
// the IBus daemon makes of any client, since it drops an update equal to the last.
struct Dedupe : ClientSim {
    explicit Dedupe(int lag) : ClientSim(lag) {}
    std::string last;
    bool first = true;
    std::string answer(bool &answered) override {
        bool fresh = false;
        const std::string reading = ClientSim::answer(fresh);
        answered = fresh && (first || reading != last);
        first = false;
        if (fresh) last = reading;
        return reading;
    }
};

template <class Sim>
bool convicted(Sim &sim, const std::string &keys) {
    for (char c : keys) {
        if (c == '\b') sim.pressBackspace(); else sim.press(ascii(c));
        if (sim.observation().verdict != Verdict::Unknown) return true;
    }
    return false;
}

} // namespace

TEST_CASE("a cached reading confirms nothing and moves nothing") {
    Flaky sim;
    sim.answers = [](int key) { return key < 3 || key >= 8; };
    // `xddwfwos`, silent from the third key to the seventh: the one the review's
    // search printed. The stale `x` neither restarts the journal nor matches it.
    CHECK_FALSE(convicted(sim, "xddwfwos"));
    CHECK(sim.composer().nonPreedit());
    Flaky quiet;
    quiet.answers = [](int key) { return key != 3; };
    quiet.type("ph");
    quiet.press(ascii('u')); // not answered: the reading is `p` again
    CHECK_FALSE(quiet.observation().inSync);
    CHECK(quiet.observation().acked == 0);
    CHECK(quiet.observation().pending == 1); // `h` is still there, and `u` joins it
    quiet.press(ascii('r'));
    CHECK(quiet.observation().acked == 2); // both confirmed by the next real answer
    CHECK(quiet.type(" ") == "phủ ");
}

TEST_CASE("an honest client with gaps in its answers is never convicted") {
    const char *alphabet = "aeiouwsfrxjdt \b";
    unsigned seed = 12345;
    auto rnd = [&]() {
        seed = seed * 1103515245u + 12345u;
        return (seed >> 16) & 0x7fff;
    };
    int convictions = 0;
    for (int trial = 0; trial < 2000; ++trial) {
        std::string keys;
        const int len = 6 + static_cast<int>(rnd() % 10);
        for (int i = 0; i < len; ++i) keys += alphabet[rnd() % 15];
        const int from = 1 + static_cast<int>(rnd() % 4);
        const int gap = 4 + static_cast<int>(rnd() % 5);
        Flaky sim;
        sim.answers = [=](int key) { return key < from || key >= from + gap; };
        if (convicted(sim, keys)) ++convictions;
    }
    CHECK(convictions == 0);
}

TEST_CASE("honest clients at every lag leave the same document, with no verdict") {
    const std::string keys = "xin chaof phur \bs tieengs vieejt  \b\bs ddaay ";
    ClientSim onTime(0);
    CHECK_FALSE(convicted(onTime, keys));
    for (int lag = 1; lag <= 7; ++lag) {
        CAPTURE(lag);
        ClientSim late(lag);
        CHECK_FALSE(convicted(late, keys));
        CHECK(late.document() == onTime.document());
        Dedupe deduped(lag);
        CHECK_FALSE(convicted(deduped, keys));
        CHECK(deduped.document() == onTime.document());
    }
}

TEST_CASE("the tone lands after Backspace at any lag the journal has heard from") {
    // `phur␣⌫s` is six keys: a client more than four keys late has shown nothing
    // past the empty field by the Backspace, so there is no evidence to re-open on
    // and the `s` is typed plain — no corruption, no verdict. After a word or two
    // every lag up to the journal's reach re-tones.
    for (int lag = 0; lag <= 7; ++lag) {
        CAPTURE(lag);
        ClientSim sim(lag);
        sim.type("phur ");
        sim.pressBackspace();
        CHECK(sim.type("s") == (lag <= 4 ? "phú" : "phủs"));
        ClientSim later(lag);
        later.type("xin chaof phur ");
        later.pressBackspace();
        CHECK(later.type("s") == "xin chào phú");
        CHECK(later.observation().verdict == Verdict::Unknown);
    }
}

TEST_CASE("a lagging client typing into an empty field is not blind") {
    // Its first readings are the empty field it found, but they are the shell's
    // cache, not answers, so BlindWrites does not count them.
    for (int lag = 2; lag <= 7; ++lag) {
        CAPTURE(lag);
        ClientSim sim(lag);
        sim.type("xin chaof ");
        CHECK(sim.composer().nonPreedit());
    }
}
