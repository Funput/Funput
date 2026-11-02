// `summarize()`: what a summary trace is allowed to say about a string, and what it
// must not let a reader recover.

#include <doctest/doctest.h>

#include "trace/summary.h"

using namespace funput;

TEST_CASE("the same salt hashes equal strings equal and different strings apart") {
    const uint64_t salt = 0x0123456789abcdefULL;
    CHECK(summarize("phủ", salt).hash == summarize("phủ", salt).hash);
    CHECK(summarize("phủ", salt).hash != summarize("phú", salt).hash);
    CHECK(summarize("phủ", salt).hash != summarize("phủ ", salt).hash);
}

// The whole privacy argument: without the salt, hashing a dictionary against the trace
// finds nothing, because another process's salt gives another hash for the same word.
TEST_CASE("a different salt gives a different hash for the same string") {
    CHECK(summarize("tiếng", 1).hash != summarize("tiếng", 2).hash);
    CHECK(summarize("", 1).hash != summarize("", 2).hash);
}

// Characters, the unit `deleteChars` counts in, so a reading's `chars` can be read
// against a plan directly. `tiếng` is five characters and seven bytes.
TEST_CASE("chars counts characters, not bytes") {
    CHECK(summarize("tiếng", 7).chars == 5);
    CHECK(summarize("ế", 7).chars == 1);
    CHECK(summarize("", 7).chars == 0);
    CHECK(summarize("abc", 7).chars == 3);
}
