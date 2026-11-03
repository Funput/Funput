// What each event writes — and above all what a summary trace leaves out. The
// assertions on absence search the whole file: a string that appears anywhere, under
// any field, is a leak.

#include <doctest/doctest.h>

#include <string>

#include "support.h"
#include "trace/sandbox.h"

using namespace funput;
using namespace funput::test;

namespace {

Observation inSyncReading() {
    return {Verdict::Unknown, true};
}

} // namespace

TEST_CASE("a summary trace carries lengths and hashes, never the text") {
    const std::string path = tracePath("summary.jsonl");
    Trace trace(path);
    trace.configure(tracing());
    trace.reading("tiếng ", true, false, 0, inSyncReading());
    trace.plan(ComposePlan::replace(1, "ế"));
    trace.plan(ComposePlan::commit("tiếng"));
    trace.plan(ComposePlan::replace(1, ""));

    const std::string file = readFile(path);
    CHECK(file.find("tiếng") == std::string::npos);
    CHECK(file.find("ế") == std::string::npos);
    const auto lines = readLines(path);
    const auto reading = lastEvent(lines, "reading");
    CHECK(reading["before"]["chars"] == 6);
    CHECK(reading["before"]["hash"].get<std::string>().size() == 16);
    CHECK_FALSE(reading["before"].contains("text"));
    // The empty string's hash would be the one value every trace prints for a known
    // input, so a deletion carries its count alone.
    const auto deletion = lastEvent(lines, "plan");
    CHECK(deletion["text"]["chars"] == 0);
    CHECK_FALSE(deletion["text"].contains("hash"));
}

TEST_CASE("a raw trace carries the text as well") {
    const std::string path = tracePath("raw.jsonl");
    Trace trace(path);
    trace.configure(tracing(/*raw=*/true));
    trace.reading("tiếng ", true, false, 0, inSyncReading());
    trace.plan(ComposePlan::replace(1, "ế"));

    const auto lines = readLines(path);
    CHECK(lastEvent(lines, "reading")["before"]["text"] == "tiếng ");
    CHECK(lastEvent(lines, "plan")["text"]["text"] == "ế");
    CHECK(lastEvent(lines, "plan")["text"]["chars"] == 1); // the summary stays
}

TEST_CASE("a letter is content: no keysym, character or modifier unless raw") {
    const std::string path = tracePath("keys.jsonl");
    Trace trace(path);
    trace.start("test");
    trace.configure(tracing());
    KeyEvent capital = ascii('A');
    capital.mods.shift = true;
    trace.key(KeyKind::Compose, capital);
    trace.key(KeyKind::Boundary, ascii(' '));
    trace.key(KeyKind::Backspace, bare(keysym::BackSpace));
    trace.key(KeyKind::Shortcut, ctrlShift('z'));

    auto lines = readLines(path);
    REQUIRE(lines.size() == 5);
    CHECK(lines[1]["kind"] == "Compose");
    CHECK_FALSE(lines[1].contains("keysym"));
    CHECK_FALSE(lines[1].contains("ch"));
    CHECK_FALSE(lines[1].contains("mods")); // Shift on a letter is its case: content
    CHECK_FALSE(lines[2].contains("keysym"));
    CHECK(lines[3]["kind"] == "Backspace");
    CHECK(lines[3]["keysym"] == "ff08");
    CHECK(lines[4]["mods"] == "ctrl+shift");
    CHECK(lines[4]["keysym"] == "7a"); // which shortcut is behaviour, not content

    trace.configure(tracing(/*raw=*/true));
    trace.key(KeyKind::Compose, capital);
    lines = readLines(path);
    CHECK(lines.back()["keysym"] == "41");
    CHECK(lines.back()["ch"] == "A");
    CHECK(lines.back()["mods"] == "shift");
}

TEST_CASE("a Replace plan carries both delete counts and a text summary") {
    const std::string path = tracePath("plan.jsonl");
    Trace trace(path);
    trace.start("test");
    trace.configure(tracing());
    ComposePlan plan = ComposePlan::replace(2, "ổ");
    plan.deleteAfterChars = 1;
    trace.plan(plan);
    trace.plan(ComposePlan::passThrough());

    const auto lines = readLines(path);
    const auto replace = lines[1];
    CHECK(replace["effect"] == "Replace");
    CHECK(replace["consumed"] == true);
    CHECK(replace["deleteChars"] == 2);
    CHECK(replace["deleteAfterChars"] == 1);
    CHECK(replace["text"]["chars"] == 1);
    const auto none = lines[2];
    CHECK(none["effect"] == "None");
    CHECK_FALSE(none.contains("deleteChars"));
    CHECK_FALSE(none.contains("text"));
}

TEST_CASE("a reading carries the composer's observation and the shell's bits") {
    const std::string path = tracePath("observation.jsonl");
    Trace trace(path);
    trace.configure(tracing());
    trace.reading("", false, true, 1, {Verdict::RefuseMode, false});

    const auto reading = lastEvent(readLines(path), "reading");
    CHECK(reading["verdict"] == "RefuseMode");
    CHECK(reading["inSync"] == false);
    CHECK(reading["answered"] == false);
    CHECK(reading["selection"] == true);
    CHECK(reading["selectedAfter"] == 1);
}

TEST_CASE("focus, mode, adopt and note lines name what they are about") {
    const std::string path = tracePath("misc.jsonl");
    Trace trace(path);
    trace.configure(tracing());
    trace.focus("gnome-shell");
    trace.mode(false, "silent");
    trace.adopt(true);
    trace.note("reset");

    const auto lines = readLines(path);
    CHECK(lastEvent(lines, "focus")["client"] == "gnome-shell");
    CHECK(lastEvent(lines, "mode")["on"] == false);
    CHECK(lastEvent(lines, "mode")["reason"] == "silent");
    CHECK(lastEvent(lines, "adopt")["taken"] == true);
    CHECK(lastEvent(lines, "note")["what"] == "reset");
}
