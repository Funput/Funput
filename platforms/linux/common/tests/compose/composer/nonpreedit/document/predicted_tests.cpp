// The journal of predicted edits, judged one reading at a time: how many of the
// pending edits a reading confirms, and what the document is predicted to read once
// everything has landed. A reading matching none, and the journal's capacity:
// restart_tests.cpp. The verdicts themselves: dropped_tests.cpp.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/document/predicted.h"

using namespace funput;

TEST_CASE("a reading confirms a prefix of the pending edits") {
    PredictedDocument doc;
    doc.observe(""); // the empty field, answered at focus
    doc.predict(0, "a", false);
    doc.predict(0, "b", false);

    // One keystroke behind: the first edit has landed, the second is still pending.
    PredictedDocument::Ack ack = doc.observe("a");
    CHECK(ack.matched);
    CHECK(ack.acked == 1);
    CHECK(ack.pending == 2);
    CHECK(ack.verdict == Verdict::Unknown);
    CHECK(doc.pending() == 1);

    // Caught up: the one edit left is confirmed too.
    ack = doc.observe("ab");
    CHECK(ack.acked == 1);
    CHECK(ack.pending == 1);
    CHECK(doc.pending() == 0);
}

TEST_CASE("an on-time client confirms everything at once") {
    PredictedDocument doc;
    doc.observe("");
    doc.predict(0, "a", false);
    doc.predict(0, "b", false);
    const PredictedDocument::Ack ack = doc.observe("ab");
    CHECK(ack.acked == 2);
    CHECK(ack.pending == 2);
}

TEST_CASE("the oldest matching prefix wins") {
    // `ab ` then Backspace then Space: the newest prefix equals the oldest. Read as
    // nothing applied, not as caught up. A stale reading *can* look newer than it
    // is when edits cancel out, and a client a key late that was taken as caught up
    // here would report `ab` next, which the emptied journal could not place.
    PredictedDocument doc;
    doc.observe("ab ");
    doc.predict(1, "", false);
    doc.predict(0, " ", false);
    PredictedDocument::Ack ack = doc.observe("ab ");
    CHECK(ack.matched);
    CHECK(ack.acked == 0);
    CHECK(doc.pending() == 2);
    // Keeping both pending costs nothing a later reading cannot settle, whichever
    // state the client was in: `ab` is the first edit applied …
    ack = doc.observe("ab");
    CHECK(ack.acked == 1);
    CHECK(doc.pending() == 1);
    // … and `ab x`, after one more edit, both of them plus the new one.
    doc.predict(0, "x", false);
    CHECK(doc.observe("ab x").acked == 2);
}

TEST_CASE("a reading equal to the base confirms nothing and forgets nothing") {
    PredictedDocument doc;
    doc.observe("phu");
    doc.predict(1, "ủ", false);
    const PredictedDocument::Ack ack = doc.observe("phu");
    CHECK(ack.matched);
    CHECK(ack.acked == 0);
    CHECK(doc.pending() == 1);
}

TEST_CASE("a dropped delete convicts on the first pending edit") {
    PredictedDocument doc;
    doc.observe("phu");
    doc.predict(1, "ủ", false);
    CHECK(doc.observe("phuủ").verdict == Verdict::RefuseMode);
}

TEST_CASE("a dropped delete convicts on the second pending edit too") {
    // The first edit landed but was never reported on its own: the reading is the
    // document with it applied and the second's delete dropped.
    PredictedDocument doc;
    doc.observe("ph");
    doc.predict(0, "u", false);
    doc.predict(1, "ủ", false);
    const PredictedDocument::Ack ack = doc.observe("phuủ");
    CHECK(ack.verdict == Verdict::RefuseMode);
    CHECK(doc.observe("phuủ").matched); // the journal restarted from the reading
}

TEST_CASE("a dropped delete after a re-open costs re-toning only") {
    PredictedDocument doc;
    doc.observe("tiếng");
    doc.predict(3, "ềng", /*afterAdopt=*/true);
    CHECK(doc.observe("tiếngềng").verdict == Verdict::RefuseRetone);
}

TEST_CASE("a delete that writes nothing never convicts") {
    // "Dropped" is then the unchanged document, which is also what a client that has
    // not answered yet reads — and that is read as nothing confirmed, not as guilt.
    PredictedDocument doc;
    doc.observe("ab ");
    doc.predict(1, "", false);
    const PredictedDocument::Ack ack = doc.observe("ab ");
    CHECK(ack.matched);
    CHECK(ack.acked == 0);
    CHECK(ack.verdict == Verdict::Unknown);
}

TEST_CASE("predicted() applies every pending edit in order") {
    PredictedDocument doc;
    doc.observe("go");
    doc.predict(1, "õ", false);
    doc.predict(1, "ox", false);
    doc.predict(0, " ", false);
    CHECK(doc.predicted() == "gox ");
    CHECK(doc.pendingWrites());
}

TEST_CASE("deletions count characters, not bytes") {
    PredictedDocument doc;
    doc.observe("tiêng");
    doc.predict(3, "ếng", false); // `êng` is three characters and five bytes
    CHECK(doc.predicted() == "tiếng");
    CHECK(doc.observe("tiếng").acked == 1);
    doc.predict(1, "", false);
    CHECK(doc.predicted() == "tiến");
}
