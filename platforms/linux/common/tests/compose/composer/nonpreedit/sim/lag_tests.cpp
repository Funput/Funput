// A client that answers a keystroke late — Firefox's content cache (RFC §4.4). Its
// reading never equals "the last repair applied", so with one record the document
// never counted as confirmed: `phur␣⌫s` gave `phủs`. These pin that the journal
// reads it as following, that a Backspace is still never taken over on its say-so,
// and that the word re-opened comes from the prediction rather than the stale reading.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

namespace {

// `\b` is a Backspace, everything else a plain key.
std::string run(ClientSim &sim, const std::string &keys) {
    for (char c : keys) {
        if (c == '\b') {
            sim.pressBackspace();
        } else {
            sim.press(ascii(c));
        }
    }
    return sim.document();
}

// A client that takes every commit and drops every delete, from the first key.
struct DropsDeletes : ClientSim {
    using ClientSim::ClientSim;
    void perform(const ComposePlan &plan, const KeyEvent &ev) override {
        if (plan.effect != Effect::Replace) return ClientSim::perform(plan, ev);
        document_ += plan.text;
    }
};

// Chrome's address bar: ordinary repairs land, the one after a re-open is dropped.
struct DropsAfterReopen : ClientSim {
    using ClientSim::ClientSim;
    void perform(const ComposePlan &plan, const KeyEvent &ev) override {
        if (!adopted() || plan.effect != Effect::Replace) return ClientSim::perform(plan, ev);
        document_ += plan.text;
    }
};

} // namespace

TEST_CASE("the tone lands after Backspace whether the client is on time or a key late") {
    for (const int lag : {0, 1}) {
        for (const std::string keys : {"phur \bs", "phur  \b\bs"}) {
            CAPTURE(lag);
            CAPTURE(keys);
            ClientSim sim(lag);
            CHECK(run(sim, keys) == "phú");
        }
    }
}

TEST_CASE("a Backspace is taken over only when every edit is confirmed") {
    // Lag one: the reading always trails by an edit, so the proof never arrives and
    // the app deletes — the stale-selection shape of Chrome's address bar (RFC §6.1,
    // invariant 6). On time, Funput deletes on its own channel.
    ClientSim late(1);
    late.type("phur  ");
    CHECK_FALSE(late.pressBackspace());
    CHECK_FALSE(late.pressBackspace());
    CHECK(late.adopted()); // and the word is still re-opened
    CHECK(late.observation().acked == 1);
    CHECK(late.observation().pending == 2);

    ClientSim onTime(0);
    onTime.type("phur  ");
    CHECK(onTime.pressBackspace());
    CHECK(onTime.pressBackspace());
    CHECK(onTime.adopted());
}

TEST_CASE("a silent client never re-opens a word") {
    // Every reading is the field as typing found it: a prefix the journal already
    // holds, so nothing is ever confirmed. No evidence, no re-open — and no verdict.
    ClientSim sim(ClientSim::kSilent);
    sim.seed("câu trước đó ");
    sim.type("phur ");
    CHECK_FALSE(sim.pressBackspace());
    CHECK_FALSE(sim.adopted());
    CHECK(sim.composer().nonPreedit());
}

// With one record, `⌫⌫` made a stale reading look confirmed: the third Backspace was
// taken over on a reading a keystroke old, and `go` adopted while the document
// was already `g`. A multi-edit journal matches that reading as prefix n−1, not n.
TEST_CASE("identical consecutive edits cannot confirm a stale reading") {
    ClientSim late(1);
    late.type("goxx ");
    REQUIRE(late.document() == "gox ");
    CHECK_FALSE(late.pressBackspace());
    CHECK_FALSE(late.adopted()); // `gox` is not a syllable
    CHECK_FALSE(late.pressBackspace());
    CHECK(late.adopted()); // `go`, which is exactly what the document holds
    CHECK_FALSE(late.pressBackspace()); // inside the re-opened word: the app deletes
    REQUIRE(late.document() == "g");

    ClientSim onTime(0);
    run(onTime, "goxx \b\b\bx");
    CHECK(run(late, "x") == onTime.document());
}

TEST_CASE("the re-opened word is the one the document really ends with") {
    ClientSim late(1);
    CHECK(run(late, "goxx \b\bx") == "gõ");
    // `tiếng␣⌫⌫⌫` used to offer `tiến` while the document read `tiế`, and the next
    // letter's repair deleted against text that was gone.
    ClientSim tieng(1);
    tieng.type("tieengs ");
    CHECK_FALSE(tieng.pressBackspace());
    CHECK(tieng.adopted());
    CHECK(tieng.document() == "tiếng");
    run(tieng, "\b\b");
    REQUIRE(tieng.document() == "tiế");
    ClientSim onTime(0);
    run(onTime, "tieengs \b\b\bf");
    CHECK(run(tieng, "f") == onTime.document());
}

TEST_CASE("a dropped delete two edits back still stands the mode down") {
    // With one record the reading a key late never lined up with the record it was
    // compared to, so a client dropping every delete was never caught there.
    DropsDeletes sim(1);
    sim.type("tieengs");
    CHECK_FALSE(sim.composer().nonPreedit());
    CHECK(sim.observation().verdict == Verdict::RefuseMode);
}

TEST_CASE("a repair dropped after a re-open still costs re-toning only, a key late") {
    DropsAfterReopen sim(1);
    sim.type("tieengs ");
    CHECK_FALSE(sim.pressBackspace());
    REQUIRE(sim.adopted());
    // The repair for `f` is dropped. The reading at Space is still the one before
    // it; the reading at `t` shows the drop, two edits back.
    sim.type("f t");
    CHECK(sim.observation().verdict == Verdict::RefuseRetone);
    CHECK(sim.composer().nonPreedit());
    sim.type("ieengs ");
    sim.pressBackspace();
    CHECK_FALSE(sim.adopted());
}
