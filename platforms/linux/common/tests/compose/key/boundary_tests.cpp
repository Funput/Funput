#include <doctest/doctest.h>

#include "compose/key/boundary.h"

using namespace funput;

TEST_CASE("whitespace ends a word") {
    CHECK(isBoundary(U' ', Method::Telex));
    CHECK(isBoundary(U'\t', Method::Telex));
    CHECK(isBoundary(U'\n', Method::Telex));
    CHECK(isBoundary(U'\r', Method::Telex));
}

TEST_CASE("digits are never boundaries") {
    // VNI spells tones with digits, so a digit has to keep reaching the engine.
    for (char32_t c = U'0'; c <= U'9'; ++c) {
        CHECK_FALSE(isBoundary(c, Method::Vni));
        CHECK_FALSE(isBoundary(c, Method::Telex));
    }
}

TEST_CASE("letters are never boundaries") {
    for (char32_t c = U'a'; c <= U'z'; ++c) CHECK_FALSE(isBoundary(c, Method::Telex));
    for (char32_t c = U'A'; c <= U'Z'; ++c) CHECK_FALSE(isBoundary(c, Method::Telex));
}

TEST_CASE("the four ASCII punctuation ranges are boundaries") {
    CHECK(isBoundary(U'!', Method::Telex)); // 0x21, start of the first range
    CHECK(isBoundary(U'/', Method::Telex)); // 0x2F, end of the first
    CHECK(isBoundary(U':', Method::Telex)); // 0x3A
    CHECK(isBoundary(U'@', Method::Telex)); // 0x40
    CHECK(isBoundary(U'[', Method::Telex)); // 0x5B
    CHECK(isBoundary(U'`', Method::Telex)); // 0x60
    CHECK(isBoundary(U'{', Method::Telex)); // 0x7B
    CHECK(isBoundary(U'~', Method::Telex)); // 0x7E
}

TEST_CASE("brackets stay composable under Telex Advanced") {
    // Advanced Telex spells ư/ơ with [ and ], so those two must reach the engine
    // instead of ending the word — but only in that method.
    CHECK_FALSE(isBoundary(U'[', Method::TelexAdvanced));
    CHECK_FALSE(isBoundary(U']', Method::TelexAdvanced));
    CHECK(isBoundary(U'[', Method::Telex));
    CHECK(isBoundary(U']', Method::Vni));
    // Its neighbours in the same range are unaffected.
    CHECK(isBoundary(U'\\', Method::TelexAdvanced));
}

TEST_CASE("non-ASCII is never a boundary") {
    CHECK_FALSE(isBoundary(U'ế', Method::Telex));
    CHECK_FALSE(isBoundary(static_cast<char32_t>(0x0080), Method::Telex)); // first past ASCII
    CHECK_FALSE(isBoundary(static_cast<char32_t>(0x00A0), Method::Telex)); // no-break space
}

TEST_CASE("isNumpadDigitKeysym matches KP_0..KP_9 and nothing else") {
    CHECK(isNumpadDigitKeysym(0xffb0));       // KP_0
    CHECK(isNumpadDigitKeysym(0xffb9));       // KP_9
    CHECK_FALSE(isNumpadDigitKeysym(0xffaf)); // one below the range
    CHECK_FALSE(isNumpadDigitKeysym(0xffba)); // one above
    CHECK_FALSE(isNumpadDigitKeysym(0x0031)); // the top-row '1', a VNI modifier
}
