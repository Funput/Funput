// Clients that apply every edit in order, answering on time, late, in bursts or
// only on change, across caret keys: the journal must never convict one, and no
// word it re-opens may send a repair across a space or past the document. The
// review's fuzz found both before the trust rule and the stale window in
// predicted.h; the same search runs here, bounded and seeded.
//
// BlindWrites is a different matter: a client a few keys late on an empty field
// answers `` twice while being written into, which is Cursor's shape, and the
// second `` stands the mode down (kLimit = 2, blind_tests.cpp). Those verdicts are
// told apart by their reading — the journal never convicts on a reading of ``.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/fuzz/models.h"

using namespace funput;
using namespace funput::test;

namespace {

struct Tally {
    int journalVerdicts = 0;
    int corrupt = 0;
    int runs = 0;
};

// Every lag from 0 to 7 under each answer model, one script.
void playHonest(const std::string &keys, Rng &rng, Tally &tally) {
    for (int lag = 0; lag <= 7; ++lag) {
        for (const FuzzSim::Answer answer :
             {FuzzSim::Answer::Honest, FuzzSim::Answer::Flaky, FuzzSim::Answer::Dedupe}) {
            FuzzSim sim(lag);
            sim.answer_ = answer;
            sim.silentFrom = 1 + rng.below(5);
            sim.silentFor = 1 + rng.below(8);
            ++tally.runs;
            for (char c : keys) {
                sim.play(c);
                if (sim.observation().verdict == Verdict::Unknown) continue;
                if (!sim.lastReading.empty()) ++tally.journalVerdicts;
                break; // a blind verdict ends the mode; the observation then stands
            }
            tally.corrupt += sim.corrupt;
        }
    }
}

Tally search(const std::string &alphabet, int trials, unsigned seed) {
    Rng rng{seed};
    Tally tally;
    for (int t = 0; t < trials; ++t) {
        const std::string keys = randomScript(rng, alphabet, 6 + rng.below(18));
        playHonest(keys, rng, tally);
    }
    return tally;
}

} // namespace

TEST_CASE("honest clients typing, deleting and spacing are never convicted by the journal") {
    const Tally tally = search("aeiouwsfrxjdtnghmpc \b", 1000, 20251103u);
    CHECK(tally.runs == 24000);
    CHECK(tally.journalVerdicts == 0);
    CHECK(tally.corrupt == 0);
}

TEST_CASE("nor across arrow keys, Enter, Shift and reported clicks") {
    // Left is where a stale reading looks like a deletion, and Enter where a
    // lagging client replays the line it left; both used to convict (the review's
    // `dwija etfmw⌫o←h u⌫ ⌫uf` at lag 4, `grhfip⏎psu⇧dj⌫ ←u⌫dd` at lag 7).
    const Tally tally = search("aeiouwsfrxjdtnghmpc \b\b<\n^#", 1000, 7u);
    CHECK(tally.runs == 24000);
    CHECK(tally.journalVerdicts == 0);
    CHECK(tally.corrupt == 0);
}

TEST_CASE("the review's scripts, replayed") {
    Rng rng{1u};
    for (const char *keys : {"dwija etfmw\bo<h u\b \buf", "pdnp<gxrx\brjrwhste", "wandwd\b\bwu\bjmnwheds",
                             "xmdmod\bmgac tsjsmuw c", "mnommm\bmi gf", "grhfip\npsu^dj\b <u\bdd",
                             "eego\bs<amdh\bpm\b\bdsujipc", "fe\b\b\buwjoe"}) {
        CAPTURE(keys);
        Tally tally;
        playHonest(keys, rng, tally);
        CHECK(tally.journalVerdicts == 0);
        CHECK(tally.corrupt == 0);
    }
}
