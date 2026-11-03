// The dropped-delete check: one edit's delete dropped, or every delete in a run of
// edits dropped, and the reason neither can convict a client that applies its edits
// in order, whatever its lag (nonpreedit/document/dropped.h).

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/document/predicted.h"

using namespace funput;

namespace {

Verdict judge(const std::string &base, const std::vector<PredictedEdit> &edits,
              const std::string &reading) {
    return droppedDelete(prefixesOf(base, edits, 0), edits, 0, reading);
}

} // namespace

TEST_CASE("a run of dropped deletes is caught at the write that follows") {
    // Two deletions the client dropped, then a letter it took: the document is the
    // base with the letter appended, which no prefix describes.
    CHECK(judge("tiếng ", {{1, ""}, {1, ""}, {0, "f"}}, "tiếng f") == Verdict::RefuseMode);
    // The same with a repair that followed a re-opened word among them.
    CHECK(judge("tiếng ", {{1, "", true}, {0, "f"}}, "tiếng f") == Verdict::RefuseRetone);
    // Through the journal: the restart keeps the edits provisional, the verdict stands.
    PredictedDocument doc;
    doc.observe("tiếng ");
    doc.predict(1, "", false);
    doc.predict(0, "f", false);
    CHECK(doc.observe("tiếng f").verdict == Verdict::RefuseMode);
}

TEST_CASE("one delete dropped among applied edits is still caught") {
    CHECK(judge("ph", {{0, "u"}, {1, "ủ"}}, "phuủ") == Verdict::RefuseMode);
    CHECK(judge("tiếng", {{3, "ềng", true}}, "tiếngềng") == Verdict::RefuseRetone);
}

TEST_CASE("every prefix is a non-verdict, so an honest reading never convicts") {
    // An honest client's reading is some prefix p(k), and the journal only asks for
    // a verdict when the reading equals none: equal to a chain c(j), it would have
    // to differ from p(j+1), which is c(j) with the deletes applied — so a delete
    // removed text the reading still shows, which an honest client cannot have
    // applied. Checked here on the journal's own loop, prefix by prefix.
    const std::vector<PredictedEdit> edits{{0, "p"}, {1, "ph"}, {0, "u"}, {1, "ủ"},
                                           {0, " "}, {1, ""},   {1, ""},  {0, "s"}};
    const std::vector<std::string> prefixes = prefixesOf("xin ", edits, 0);
    for (size_t k = 0; k < prefixes.size(); ++k) {
        CAPTURE(k);
        PredictedDocument doc;
        doc.observe("xin ");
        for (const PredictedEdit &edit : edits) doc.predict(edit.deleted, edit.text, false);
        const PredictedDocument::Ack ack = doc.observe(prefixes[k]);
        CHECK(ack.matched);
        CHECK(prefixes[ack.acked] == prefixes[k]); // the oldest equal prefix wins
        CHECK(ack.acked <= k);
        CHECK(ack.verdict == Verdict::Unknown);
    }
}

TEST_CASE("a delete that removes nothing makes the chain a prefix, not a verdict") {
    // Deleting from an empty document is a no-op, so "dropped" and "applied" are the
    // same string; the prefix loop matches it first and nothing is judged.
    PredictedDocument doc;
    doc.observe("");
    doc.predict(1, "a", false);
    const PredictedDocument::Ack ack = doc.observe("a");
    CHECK(ack.matched);
    CHECK(ack.acked == 1);
    CHECK(ack.verdict == Verdict::Unknown);
}

TEST_CASE("a delete-only edit confirmed with an identical one pending is ambiguous") {
    PredictedDocument doc;
    doc.observe("ab  ");
    doc.predict(1, "", false);
    doc.predict(1, "", false);
    CHECK(doc.observe("ab ").acked == 1);
    CHECK(doc.ambiguous());
    // A write confirmed on top of it settles the question.
    doc.predict(0, "x", false);
    CHECK(doc.observe("abx").acked == 2);
    CHECK_FALSE(doc.ambiguous());
    // Not ambiguous when the confirmed edit wrote text, or the pending one differs.
    doc.predict(0, " ", false);
    doc.predict(1, "", false);
    CHECK(doc.observe("abx ").acked == 1);
    CHECK_FALSE(doc.ambiguous());
}
