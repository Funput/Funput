// Which prefix a reading equals when some pending edits may already be in the
// base (nonpreedit/document/match.h): the suffix a skip must leave on the base,
// the order among plausible matches, and when a match is contested.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/document/match.h"

using namespace funput;

TEST_CASE("edits applied in order leave a suffix the base must end with") {
    const std::vector<PredictedEdit> edits{{0, "m"}, {0, "n"}, {1, "ô"}, {1, ""}};
    CHECK(suffixLeftBy(edits, 0).empty());
    CHECK(suffixLeftBy(edits, 2) == "mn");
    CHECK(suffixLeftBy(edits, 3) == "mô"); // the ô replaced the n
    CHECK(suffixLeftBy(edits, 4) == "m");
    // A deletion reaching past what the edits wrote eats text the base never
    // showed; nothing is known about it, so nothing is required of it.
    CHECK(suffixLeftBy({{2, ""}, {0, "x"}}, 2) == "x");
    CHECK(endsWith("xin mô", "mô"));
    CHECK_FALSE(endsWith("", "m"));
}

TEST_CASE("a skip the base could not hold is not taken") {
    // `m`, `n`, `o` typed before a reading of `` cannot have been: the reading
    // `m` is the first edit landing, not the fourth after three in the base. The
    // review's `mnommm⌫mi gf` took the largest skip here and convicted the client.
    const std::vector<PredictedEdit> edits{{0, "m"}, {0, "n"}, {0, "o"}, {0, "m"}};
    const PrefixMatch match = bestMatch("", edits, 3, "m");
    REQUIRE(match.found);
    CHECK(match.skip == 0);
    CHECK(match.k == 1);
    CHECK_FALSE(match.contested);
}

TEST_CASE("a Backspace pending does not explain an arrow key") {
    // After `h`, ` `, `u`, `⌫` typed since the restart, a reading one character
    // shorter than the base is not the ⌫ landing with the three before it skipped:
    // the base would have to end with `h u`.
    const std::vector<PredictedEdit> edits{{0, "h"}, {0, " "}, {0, "u"}, {1, ""}};
    CHECK_FALSE(bestMatch("dwija etfmo", edits, 3, "dwija etfm").found);
}

TEST_CASE("among plausible matches the fewest edits settled wins") {
    // An on-time client answering after a gap: `a` and `b` are in the base, and
    // the reading is `c` landing. Skip 2 is plausible (the base ends with `ab`),
    // skip 0 is not a match, so the skip is taken.
    const std::vector<PredictedEdit> edits{{0, "a"}, {0, "b"}, {0, "c"}};
    PrefixMatch match = bestMatch("xab", edits, 2, "xabc");
    REQUIRE(match.found);
    CHECK(match.skip == 2);
    CHECK(match.k == 1);
    CHECK_FALSE(match.contested);
    // The same base read again: nothing settled at skip 0 beats two edits
    // settled at skip 2, and the two stories differ, so the match is contested.
    match = bestMatch("xab", edits, 2, "xab");
    REQUIRE(match.found);
    CHECK(match.skip == 0);
    CHECK(match.k == 0);
    CHECK(match.contested);
}

TEST_CASE("within one skip the oldest prefix wins, and that is never contested") {
    // `ab ` then ⌫ then ␣: the newest prefix equals the oldest. Edits that cancel
    // out can stay pending whichever state the client is in.
    const std::vector<PredictedEdit> edits{{1, ""}, {0, " "}};
    const PrefixMatch match = bestMatch("ab ", edits, 0, "ab ");
    REQUIRE(match.found);
    CHECK(match.k == 0);
    CHECK_FALSE(match.contested);
}

TEST_CASE("skips that settle different edits contest each other, alike as they predict") {
    // `x` then ⌫ cancel out, so skip 2 with nothing confirmed and skip 0 with
    // nothing confirmed predict the same document — but one keeps `x` and ⌫
    // pending and the other does not, and a later reading may need them.
    const std::vector<PredictedEdit> edits{{0, "x"}, {1, ""}, {0, "y"}};
    const PrefixMatch match = bestMatch("ab", edits, 2, "ab");
    REQUIRE(match.found);
    CHECK(match.skip == 0);
    CHECK(match.k == 0);
    CHECK(match.contested);
}
