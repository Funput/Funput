// How the journal starts over — at the first reading, after a reading that matches
// no prefix, past its capacity, and after a focus change or a caret key — and what
// it takes to trust the base again. The edits pending at a restart stay on as
// provisional, since a lagging client may still be about to apply them.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/document/predicted.h"

using namespace funput;
using Reset = PredictedDocument::Reset;

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
    const PredictedDocument::Ack ack = doc.observe("xa");
    CHECK_FALSE(ack.matched);
    CHECK(ack.acked == 0);
    CHECK(ack.verdict == Verdict::Unknown);
    CHECK(doc.pending() == 1);
    doc.predict(0, "b", false);
    SUBCASE("a lagging client: the old edit was still in flight") {
        const PredictedDocument::Ack next = doc.observe("xaa");
        CHECK(next.acked == 1);
        CHECK(next.pending == 2);
        CHECK(doc.predicted() == "xaab");
    }
    SUBCASE("an on-time client answering after a gap: the old edit is in the base") {
        // Plausible, because the base ends with what the skipped edit wrote.
        const PredictedDocument::Ack next = doc.observe("xab");
        CHECK(next.acked == 1);
        CHECK(next.pending == 1);
        CHECK(doc.predicted() == "xab");
    }
}

TEST_CASE("an edit is skipped only where the base could hold it") {
    // `elsewhere` cannot be a document `a` was just typed into, so a reading of
    // `elsewhereb` is not the skip-one story but another restart — the shape of a
    // client that moved the caret without telling (predicted.h names it).
    PredictedDocument doc;
    doc.observe("");
    doc.predict(0, "a", false);
    doc.observe("elsewhere");
    doc.predict(0, "b", false);
    const PredictedDocument::Ack ack = doc.observe("elsewhereb");
    CHECK_FALSE(ack.matched);
    CHECK(ack.verdict == Verdict::Unknown);
    CHECK(doc.pending() == 2);
}

TEST_CASE("trust is earned by text confirmed at skip 0, kept by a deletion, lost by a restart") {
    PredictedDocument doc;
    CHECK_FALSE(doc.observe("ab").trusted); // a seeded base is anyone's guess
    doc.predict(1, "", false);
    CHECK_FALSE(doc.observe("a").trusted); // an arrow key reads exactly like this
    doc.predict(0, "c", false);
    CHECK(doc.observe("ac").trusted);
    doc.predict(1, "", false);
    CHECK(doc.observe("a").trusted);
    doc.predict(0, "d", false);
    CHECK_FALSE(doc.observe("moved").trusted);
}

TEST_CASE("a focus change trusts the first reading, a caret key does not") {
    PredictedDocument doc;
    doc.reset(Reset::Focus);
    CHECK(doc.observe("field").trusted);
    doc.predict(0, "x", false);
    CHECK(doc.observe("fieldx").acked == 1);
    doc.reset(Reset::CaretKey);
    CHECK_FALSE(doc.observe("field").trusted);
}

TEST_CASE("after a caret key, as many readings as the client was behind are stale") {
    // A client two keys late had two edits unshown when the arrow was pressed; its
    // next two readings are from before it and the third is where the caret landed.
    // All three restart the journal — the second would otherwise read as `b` landing
    // on `xa` — and the fourth confirms the first edit typed after the arrow.
    PredictedDocument doc;
    doc.reset(Reset::Focus);
    doc.observe("x");
    doc.predict(0, "a", false);
    doc.predict(0, "b", false);
    doc.observe("x"); // k == 0: two edits unshown, measured on a trusted base
    doc.reset(Reset::CaretKey);
    doc.predict(0, "b", false);
    CHECK_FALSE(doc.observe("xa").matched);
    CHECK_FALSE(doc.observe("xab").matched);
    doc.predict(0, "c", false);
    CHECK_FALSE(doc.observe("xa").matched); // the caret landed here
    const PredictedDocument::Ack ack = doc.observe("xab");
    CHECK(ack.matched);
    CHECK(ack.acked == 1);
    CHECK(ack.trusted);
    CHECK(doc.predicted() == "xabc");
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
