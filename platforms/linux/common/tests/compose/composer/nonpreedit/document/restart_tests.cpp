// How the journal starts over: at the first reading, after a reading that matches
// no prefix, and past its capacity. The edits pending at a restart stay on as
// provisional, since a lagging client may still be about to apply them.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/document/predicted.h"

using namespace funput;

TEST_CASE("the first reading is the base, whatever was predicted before it") {
    // Nothing to compare with until the client has answered once: the reading is
    // taken as the document and the edits since stay on, provisionally.
    PredictedDocument doc;
    doc.predict(0, "a", false);
    const PredictedDocument::Ack ack = doc.observe("xin ");
    CHECK_FALSE(ack.matched);
    CHECK(ack.verdict == Verdict::Unknown);
    CHECK(doc.observe("xin a").acked == 1); // in flight after all: a client a key late
}

TEST_CASE("a reading matching no prefix restarts the journal and keeps the edits provisional") {
    PredictedDocument doc;
    doc.observe("");
    doc.predict(0, "a", false);
    const PredictedDocument::Ack ack = doc.observe("elsewhere");
    CHECK_FALSE(ack.matched);
    CHECK(ack.acked == 0);
    CHECK(ack.verdict == Verdict::Unknown);
    CHECK(doc.pending() == 1);
    doc.predict(0, "b", false);
    SUBCASE("a lagging client: the old edit was still in flight") {
        const PredictedDocument::Ack next = doc.observe("elsewherea");
        CHECK(next.acked == 1);
        CHECK(next.pending == 2);
        CHECK(doc.predicted() == "elsewhereab");
    }
    SUBCASE("a click reported on time: the old edit went to the old place") {
        const PredictedDocument::Ack next = doc.observe("elsewhereb");
        CHECK(next.acked == 1);
        CHECK(next.pending == 1);
        CHECK(doc.predicted() == "elsewhereb");
    }
}

TEST_CASE("past capacity the journal is lost and the next reading restarts it") {
    PredictedDocument doc;
    doc.observe("");
    for (size_t i = 0; i < PredictedDocument::kCapacity; ++i) doc.predict(0, "a", false);
    REQUIRE(doc.pending() == PredictedDocument::kCapacity);
    doc.predict(0, "b", false);
    CHECK(doc.pending() == 1); // the overflow, alone
    // Folded, `a` would have become the base and a client that never confirmed it
    // could be convicted for it; lost, the reading is simply where things stand.
    const PredictedDocument::Ack ack = doc.observe("aaaab");
    CHECK_FALSE(ack.matched);
    CHECK(ack.verdict == Verdict::Unknown);
    CHECK(doc.observe("aaaab").matched);
}
