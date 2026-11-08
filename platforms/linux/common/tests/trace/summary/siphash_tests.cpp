// `siphash24()`: against the reference vectors, and against the attack that broke the
// hash it replaced.

#include <doctest/doctest.h>

#include <cstdint>
#include <set>
#include <string>
#include <vector>

#include "trace/summary/siphash.h"

using namespace funput;

namespace {

// The paper's setup (Appendix A of Aumasson & Bernstein, and `vectors.h` in the
// reference implementation): key 00 01 … 0f, message 00 01 … (len-1), output as the
// little-endian 64-bit word.
const SipKey kPaperKey{0x0706050403020100ULL, 0x0f0e0d0c0b0a0908ULL};

std::string message(size_t len) {
    std::string out;
    for (size_t i = 0; i < len; ++i) out += static_cast<char>(i);
    return out;
}

} // namespace

TEST_CASE("siphash24 matches the SipHash-2-4 reference vectors") {
    const std::vector<uint64_t> expected{
        0x726fdb47dd0e0e31ULL, 0x74f839c593dc67fdULL, 0x0d6c8009d9a94f5aULL,
        0x85676696d7fb7e2dULL, 0xcf2794e0277187b7ULL, 0x18765564cd99a68dULL,
        0xcbc9466e58fee3ceULL, 0xab0200f58b01d137ULL, 0x93f5f5799a932462ULL,
        0x9e0082df0ba9e4b0ULL, 0x7a5dbbc594ddb9f3ULL, 0xf4b32f46226bada7ULL,
        0x751e8fbc860ee5fbULL, 0x14ea5627c0843d90ULL, 0xf723ca908e7af2eeULL,
        0xa129ca6149be45e5ULL, // the worked example in the paper's body
    };
    for (size_t len = 0; len < expected.size(); ++len) {
        CAPTURE(len);
        CHECK(siphash24(message(len), kPaperKey) == expected[len]);
    }
}

TEST_CASE("the block boundary and a long input") {
    // Lengths across one and two full 8-byte blocks, no two alike, each stable.
    std::set<uint64_t> seen;
    for (size_t len = 7; len <= 17; ++len) {
        const uint64_t hash = siphash24(message(len), kPaperKey);
        CHECK(hash == siphash24(message(len), kPaperKey));
        seen.insert(hash);
    }
    CHECK(seen.size() == 11);
    CHECK(siphash24(message(1000), kPaperKey) != siphash24(message(1001), kPaperKey));
}

TEST_CASE("a different key gives a different hash, including for the empty string") {
    const SipKey other{0x0706050403020100ULL, 0x0f0e0d0c0b0a0909ULL};
    CHECK(siphash24("", kPaperKey) != siphash24("", other));
    CHECK(siphash24("tiếng", kPaperKey) != siphash24("tiếng", other));
}

TEST_CASE("randomSipKey draws 128 bits that differ from draw to draw") {
    const SipKey a = randomSipKey();
    const SipKey b = randomSipKey();
    CHECK((a.k0 != b.k0 || a.k1 != b.k1));
    CHECK((a.k0 >> 32) != 0); // the high words are filled, not left zero
}

// The attack the review ran against the previous hash. FNV-1a's state was one word
// that a known (text, hash) pair gave back by undoing the last steps, after which a
// dictionary pass found every other input. SipHash has no such step to undo: the
// state is four words the output folds together, so the only route to a match is
// knowing the key. The dictionary probe below is what the reviewer's probe did,
// rerun without the key: nothing matches, and the key is not derivable from any
// number of known pairs — only from the process that drew it.
TEST_CASE("without the key, a dictionary finds nothing and a known pair gives no key") {
    const SipKey secret = randomSipKey();
    const uint64_t target = siphash24("hunter2", secret);
    const std::vector<std::string> dictionary{"xin", "chào", "mật", "khẩu", "tiếng", "việt",
                                              "hunter2", "password", "phủ", "phú", "", "ế"};
    // Every key the reader could try is as good as any other; try a few families.
    int matches = 0;
    for (uint64_t guess = 0; guess < 4096; ++guess) {
        const SipKey candidates[]{{guess, 0}, {0, guess}, {guess, guess}, {~guess, guess}};
        for (const SipKey &key : candidates) {
            for (const auto &word : dictionary) matches += siphash24(word, key) == target;
        }
    }
    CHECK(matches == 0);
    // A known pair, as the reviewer had (`ế` after a Backspace): with FNV it pinned
    // the state. Here the hash of the next string is unrelated to it.
    const uint64_t known = siphash24("ế", secret);
    CHECK(known != target);
    CHECK(siphash24("", secret) != known);
}
