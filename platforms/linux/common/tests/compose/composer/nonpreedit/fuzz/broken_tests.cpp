// Clients that drop deletes, under random scripts: how reliably each is caught,
// and how soon. A drop at key d is first visible in the reading that shows the
// document after it, which a client `lag` keys late sends at key d + lag + 1, so
// a run counts as missed when a repair that deleted text was dropped, lag + 2
// keys passed, and no verdict came. The bounds are the rates the review measured
// on the journal before the trust rule, which the rule must not lower — it delays
// no verdict a trusted base could give, and the stale window only covers readings
// a lagging client had not shown.

#include <doctest/doctest.h>

#include <map>

#include "compose/composer/nonpreedit/fuzz/models.h"

using namespace funput;
using namespace funput::test;

namespace {

struct Model {
    const char *name;
    FuzzSim::Answer answer;
    FuzzSim::Perform perform;
    Verdict expected;
};

constexpr Model kModels[] = {
    {"DropsDeletes", FuzzSim::Answer::Honest, FuzzSim::Perform::DropsDeletes, Verdict::RefuseMode},
    {"DropsFrom", FuzzSim::Answer::Honest, FuzzSim::Perform::DropsFrom, Verdict::RefuseMode},
    {"DropsAfterReopen", FuzzSim::Answer::Honest, FuzzSim::Perform::DropsAfterReopen,
     Verdict::RefuseRetone},
    {"Dedupe+DropsDeletes", FuzzSim::Answer::Dedupe, FuzzSim::Perform::DropsDeletes, Verdict::RefuseMode},
    {"Cursor", FuzzSim::Answer::AlwaysEmpty, FuzzSim::Perform::DropsDeletes, Verdict::RefuseMode},
};

struct Rate {
    int runs = 0;   // runs in which a text-deleting repair was dropped
    int missed = 0; // of them, no verdict with lag + 2 keys to spare
    int wrong = 0;  // the mode lost by the journal where only re-toning should go
};

std::map<std::string, Rate> measure(const std::string &alphabet, int trials, unsigned seed) {
    Rng rng{seed};
    std::map<std::string, Rate> rates;
    for (int t = 0; t < trials; ++t) {
        const int len = 6 + rng.below(18);
        const std::string keys = randomScript(rng, alphabet, len);
        for (const Model &model : kModels) {
            for (int lag = 0; lag <= 3; ++lag) {
                FuzzSim sim(lag);
                sim.answer_ = model.answer;
                sim.perform_ = model.perform;
                sim.dropFrom = 1 + rng.below(len);
                int firstDropAt = -1;
                bool caught = false, wrong = false;
                for (char c : keys) {
                    sim.play(c);
                    if (firstDropAt < 0 && sim.droppedWithText > 0) firstDropAt = sim.keyNo;
                    const Verdict verdict = sim.observation().verdict;
                    if (verdict == Verdict::Unknown) continue;
                    // A client dropping every delete loses re-toning first when the
                    // first drop seen followed a re-open, and the mode at the next
                    // write; either is the drop caught. A verdict on `` is
                    // BlindWrites', not the journal's.
                    caught = true;
                    if (verdict == Verdict::RefuseMode && model.expected == Verdict::RefuseRetone &&
                        !sim.lastReading.empty()) {
                        wrong = true;
                    }
                    break;
                }
                Rate &rate = rates[std::string(model.name) + "/lag" + std::to_string(lag)];
                if (sim.droppedWithText == 0) continue;
                ++rate.runs;
                if (!caught && sim.keyNo - firstDropAt >= lag + 2) ++rate.missed;
                if (wrong) ++rate.wrong;
            }
        }
    }
    return rates;
}

} // namespace

TEST_CASE("clients that drop deletes are caught, with the verdict the drop deserves") {
    const std::map<std::string, Rate> rates = measure("aeiouwsfrxjdtnghmpc \b", 1000, 20251103u);
    // Caught as soon as the reading can show the drop, every run at every lag —
    // the review's rates before this round were the same — and the address bar
    // never loses more than re-toning.
    for (const auto &[name, rate] : rates) {
        CAPTURE(name);
        CHECK(rate.runs > 0);
        CHECK(rate.wrong == 0);
        CHECK(rate.missed == 0);
    }
}

TEST_CASE("Cursor is caught by the second empty answer that the prediction belies") {
    // Two letters write text, so the second answer of `` after the first key is the
    // verdict: key 3. A script that opens with a Backspace or a space, or empties
    // its prediction again (`n⌫d` reads `` on an honest client a key late),
    // is caught a key or two later, but never with more than one text-deleting
    // repair dropped. Before this round a ⌫ pending on an empty base excused ``
    // as a later prefix, and `⌫ tieengs vieejt nam` cost three words.
    Rng rng{7u};
    std::map<int, int> caughtAt;
    int worstDropped = 0;
    for (int t = 0; t < 1000; ++t) {
        const std::string keys = randomScript(rng, "aeiouwsfrxjdtnghmpc \b", 40);
        FuzzSim sim(0);
        sim.answer_ = FuzzSim::Answer::AlwaysEmpty;
        sim.perform_ = FuzzSim::Perform::DropsDeletes;
        int at = -1;
        for (char c : keys) {
            sim.play(c);
            if (!sim.composer().nonPreedit()) {
                at = sim.keyNo;
                break;
            }
        }
        ++caughtAt[at];
        if (sim.droppedWithText > worstDropped) worstDropped = sim.droppedWithText;
        const bool letters = std::string("\b ").find(keys[0]) == std::string::npos &&
                             std::string("\b ").find(keys[1]) == std::string::npos;
        if (letters) CHECK(at == 3);
    }
    CHECK(caughtAt.count(-1) == 0);
    CHECK(caughtAt.rbegin()->first <= 6);
    CHECK(worstDropped <= 1);
}
