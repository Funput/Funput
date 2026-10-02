// The letter set behind "Phụ âm đầu mở rộng": its stored spelling, and the rules
// for reading one written by another platform or a newer build.

#include <doctest/doctest.h>

#include "settings/onsets/letters.h"

using namespace funput;

TEST_CASE("a new set admits no letter") {
    const ExtraOnsetLetters letters;
    CHECK(letters.empty());
    CHECK(letters.id().empty());
    CHECK(letters == ExtraOnsetLetters::none());
    for (OnsetLetter letter : kAllOnsetLetters) CHECK_FALSE(letters.contains(letter));
}

TEST_CASE("every letter round-trips through its stored spelling") {
    for (OnsetLetter letter : kAllOnsetLetters) {
        const ExtraOnsetLetters one = ExtraOnsetLetters().with(letter, true);
        CHECK(one.id() == std::string(1, onsetSymbol(letter)));
        CHECK(ExtraOnsetLetters::fromId(one.id()) == one);
    }
    CHECK(ExtraOnsetLetters::all().id() == "zfwj");
    CHECK(ExtraOnsetLetters::fromId("zfwj") == ExtraOnsetLetters::all());
}

TEST_CASE("the stored spelling is canonical whatever order it was read in") {
    // Same order as funput-config, so the file does not churn between writers.
    CHECK(ExtraOnsetLetters::fromId("jz").id() == "zj");
    CHECK(ExtraOnsetLetters::fromId("jwfz").id() == "zfwj");
}

TEST_CASE("reading ignores case, repeats and letters this build does not know") {
    CHECK(ExtraOnsetLetters::fromId("WJ").id() == "wj");
    CHECK(ExtraOnsetLetters::fromId("zz").id() == "z");
    CHECK(ExtraOnsetLetters::fromId("zxq").id() == "z");
    CHECK(ExtraOnsetLetters::fromId("đ-1").empty());
}

TEST_CASE("unticking the last letter leaves the set empty") {
    const ExtraOnsetLetters z = ExtraOnsetLetters().with(OnsetLetter::Z, true);
    CHECK(z.with(OnsetLetter::Z, true) == z); // idempotent
    CHECK(z.with(OnsetLetter::F, false) == z);
    CHECK(z.with(OnsetLetter::Z, false).empty());
}
