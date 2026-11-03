// The trace's switches and its file: when a file appears at all, the one-object-per-
// line format, and the bound that keeps it from growing for a whole session.

#include <doctest/doctest.h>

#include <filesystem>
#include <string>

#include "trace/sandbox.h"

using namespace funput;
using namespace funput::test;

TEST_CASE("a disabled trace creates no file") {
    const std::string path = tracePath("disabled.jsonl");
    Trace trace(path);
    CHECK_FALSE(trace.enabled());
    trace.start("test");
    trace.note("nothing");
    trace.configure(Settings{}); // the defaults: both keys off
    trace.note("still nothing");
    CHECK_FALSE(std::filesystem::exists(path));
}

TEST_CASE("an enabled trace writes one JSON object per line with t from the start line") {
    const std::string path = tracePath("lines.jsonl");
    Trace trace(path);
    trace.start("test");
    trace.configure(tracing());
    REQUIRE(trace.enabled());
    CHECK_FALSE(trace.raw());
    trace.note("one");
    trace.note("two");

    const auto lines = readLines(path);
    REQUIRE(lines.size() == 3); // start, then the two notes
    CHECK(lines[0]["ev"] == "start");
    CHECK(lines[0]["shell"] == "test");
    CHECK(lines[0]["raw"] == false);
    CHECK(lines[0].contains("wall"));
    CHECK(lines[2]["what"] == "two");
    CHECK(lines[0]["t"] == 0);
    long long last = -1;
    for (const auto &line : lines) {
        REQUIRE(line["t"].is_number_integer());
        const long long t = line["t"].get<long long>();
        CHECK(t >= last);
        last = t;
    }
}

TEST_CASE("exceeding maxBytes truncates the file and keeps the latest ring lines") {
    const std::string path = tracePath("ring.jsonl");
    // A note line is ~50 bytes: the ring overflows the bound many times over.
    const size_t ringLines = 5;
    const size_t maxBytes = 600;
    Trace trace(path, ringLines, maxBytes);
    trace.start("test");
    trace.configure(tracing());
    for (int i = 0; i < 100; ++i) trace.note("note " + std::to_string(i));

    CHECK(std::filesystem::file_size(path) <= maxBytes);
    const auto lines = readLines(path);
    // Never fewer than the ring (they all fit in the bound), ending on the newest line,
    // and contiguous: the file is the tail of the session, not samples of it.
    REQUIRE(lines.size() >= ringLines);
    CHECK(lines.back()["what"] == "note 99");
    int expected = 99 - static_cast<int>(lines.size()) + 1;
    for (const auto &line : lines) {
        CHECK(line["what"] == "note " + std::to_string(expected));
        ++expected;
    }
}

TEST_CASE("configure() turning the trace off stops the lines") {
    const std::string path = tracePath("off.jsonl");
    Trace trace(path);
    trace.start("test");
    trace.configure(tracing());
    trace.note("before");
    trace.configure(Settings{});
    CHECK_FALSE(trace.enabled());
    trace.note("after");

    const auto lines = readLines(path);
    REQUIRE(lines.size() == 2);
    CHECK(lines[1]["what"] == "before");
}

// User: raw on → reproduce → raw off → delete the file to scrub it → keep tracing in
// summary mode. The ring must not bring the raw lines back on the next rewrite.
TEST_CASE("switching the trace or raw off empties the ring") {
    const std::string path = tracePath("scrub.jsonl");
    Trace trace(path, 50, 1500);
    trace.start("test");
    trace.configure(tracing(/*raw=*/true));
    trace.reading("mật khẩu bí mật", true, false, 0, {});
    trace.configure(tracing()); // raw off, still on
    trace.configure(Settings{}); // off
    std::filesystem::remove(path);
    trace.configure(tracing()); // summary mode, fresh file
    for (int i = 0; i < 60; ++i) trace.note("n" + std::to_string(i)); // past the bound
    CHECK(readFile(path).find("mật khẩu") == std::string::npos);
}

TEST_CASE("switching raw on while enabled announces itself") {
    const std::string path = tracePath("raw-switch.jsonl");
    Trace trace(path);
    trace.start("test");
    trace.configure(tracing());
    trace.configure(tracing(/*raw=*/true));
    CHECK(trace.raw());

    const auto lines = readLines(path);
    REQUIRE(lines.size() == 2);
    CHECK(lines[0]["raw"] == false);
    CHECK(lines[1]["ev"] == "start");
    CHECK(lines[1]["raw"] == true);
}

// A trace that cannot be written must switch itself off rather than throw: the
// shells call it from inside a key handler.
TEST_CASE("a path that cannot be created disables the trace quietly") {
    Trace trace("/proc/funput-cannot-exist/trace.jsonl");
    trace.configure(tracing());
    trace.note("lost");
    CHECK_FALSE(trace.enabled());
    trace.note("also lost"); // and stays a no-op
}

TEST_CASE("a trace without a path can never be enabled") {
    Trace trace("");
    trace.configure(tracing(true));
    CHECK_FALSE(trace.enabled());
    CHECK_FALSE(trace.raw());
}
