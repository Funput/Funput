// The trace's file as a file: who can read it, what a restart does to the previous
// session, and a line too large for the bound. The ring and the switches: ring_tests.

#include <doctest/doctest.h>

#include <sys/stat.h>

#include <filesystem>
#include <string>

#include "trace/sandbox.h"

using namespace funput;
using namespace funput::test;

namespace {

mode_t modeOf(const std::string &path) {
    struct stat status {};
    REQUIRE(::stat(path.c_str(), &status) == 0);
    return status.st_mode & 0777;
}

} // namespace

// Raw mode writes what the user types: whatever the umask says, nobody else reads
// it. Both the directory and the file are private from the moment they exist.
TEST_CASE("the directory is created 0700 and the file 0600, whatever the umask") {
    const mode_t old = ::umask(0);
    const std::string path = tracePath("perm/funput/trace.jsonl");
    std::filesystem::remove_all(std::filesystem::path(path).parent_path());
    Trace trace(path);
    trace.start("test");
    trace.configure(tracing());
    trace.note("x");
    ::umask(old);
    CHECK(modeOf(std::filesystem::path(path).parent_path().string()) == 0700);
    CHECK(modeOf(path) == 0600);
}

TEST_CASE("a file left readable by an older build is tightened on open") {
    const std::string path = tracePath("loose.jsonl");
    writeFile(path, "{\"ev\":\"note\"}\n");
    ::chmod(path.c_str(), 0644);
    Trace trace(path);
    trace.start("test");
    trace.configure(tracing());
    trace.note("x");
    CHECK(modeOf(path) == 0600);
}

// "A trace spans engine restarts": the next process appends. But its ring holds
// nothing of the session before, so a rewrite would wipe it — and the session before
// is the one that crashed. Past half the bound, the file is rotated instead.
TEST_CASE("a restart with the file past half the bound keeps it as trace.jsonl.1") {
    const std::string path = tracePath("restart.jsonl");
    const std::string previous = tracePath("restart.jsonl.1");
    const size_t maxBytes = 600;
    {
        Trace crashed(path, 100, maxBytes);
        crashed.start("test");
        crashed.configure(tracing());
        while (std::filesystem::file_size(path) < maxBytes - 50) crashed.note("crash session");
    }
    const auto before = readLines(path);
    Trace trace(path, 100, maxBytes);
    trace.start("test");
    trace.configure(tracing());
    trace.note("first line after restart");

    CHECK(readLines(previous) == before);
    CHECK(modeOf(previous) == 0600);
    const auto lines = readLines(path);
    REQUIRE(lines.size() == 2);
    CHECK(lines[0]["ev"] == "start");
    CHECK(lines[1]["what"] == "first line after restart");
}

TEST_CASE("a restart with a short file appends to it") {
    const std::string path = tracePath("short.jsonl");
    {
        Trace earlier(path, 100, 600);
        earlier.start("test");
        earlier.configure(tracing());
        earlier.note("earlier");
    }
    Trace trace(path, 100, 600);
    trace.start("test");
    trace.configure(tracing());
    trace.note("later");
    const auto lines = readLines(path);
    REQUIRE(lines.size() == 4); // start, earlier, start, later
    CHECK(lines[1]["what"] == "earlier");
    CHECK(lines[3]["what"] == "later");
    CHECK_FALSE(std::filesystem::exists(path + ".1"));
}

// A raw reading of a very long document can exceed the whole bound. Rewriting from
// a ring that cannot fit it would leave the file empty; what stays is a note.
TEST_CASE("a line larger than the bound becomes a note saying it was dropped") {
    const std::string path = tracePath("big-line.jsonl");
    Trace trace(path, 100, 300);
    trace.start("test");
    trace.configure(tracing(/*raw=*/true));
    trace.reading(std::string(400, 'x'), true, false, 0, {});
    trace.note("after");

    CHECK(std::filesystem::file_size(path) <= 300);
    CHECK(readFile(path).find("xxxx") == std::string::npos);
    const auto lines = readLines(path);
    REQUIRE(lines.size() == 3);
    CHECK(lines[1]["ev"] == "note");
    CHECK(lines[1]["what"].get<std::string>().rfind("dropped reading line of ", 0) == 0);
    CHECK(lines[2]["what"] == "after");
}
