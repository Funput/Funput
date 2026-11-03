// `summarize()`: what a summary trace is allowed to say about a string, and what it
// must not let a reader recover.

#include <doctest/doctest.h>

#include "trace/summary/text.h"

using namespace funput;

namespace {
const SipKey kKey{0x0123456789abcdefULL, 0xfedcba9876543210ULL};
const SipKey kOther{0x0123456789abcdefULL, 0xfedcba9876543211ULL};
} // namespace

TEST_CASE("the same key hashes equal strings equal and different strings apart") {
    CHECK(summarize("phủ", kKey).hash == summarize("phủ", kKey).hash);
    CHECK(summarize("phủ", kKey).hash != summarize("phú", kKey).hash);
    CHECK(summarize("phủ", kKey).hash != summarize("phủ ", kKey).hash);
}

// The whole privacy argument: without the key, hashing a dictionary against the trace
// finds nothing, because another process's key gives another hash for the same word.
TEST_CASE("a different key gives a different hash for the same string") {
    CHECK(summarize("tiếng", kKey).hash != summarize("tiếng", kOther).hash);
    CHECK(summarize("", kKey).hash != summarize("", kOther).hash);
}

// Characters, the unit `deleteChars` counts in, so a reading's `chars` can be read
// against a plan directly. `tiếng` is five characters and seven bytes.
TEST_CASE("chars counts characters, not bytes") {
    CHECK(summarize("tiếng", kKey).chars == 5);
    CHECK(summarize("ế", kKey).chars == 1);
    CHECK(summarize("", kKey).chars == 0);
    CHECK(summarize("abc", kKey).chars == 3);
}
