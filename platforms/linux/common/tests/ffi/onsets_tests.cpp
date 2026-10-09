// The letter set as the C ABI's `ONSET_*` mask: one wire bit per letter, each
// letter mapped by name rather than by the settings type's private bits.

#include <doctest/doctest.h>

#include "ffi/onsets.h"

using namespace funput;

TEST_CASE("each letter maps to its own ONSET_* bit") {
    const auto only = [](OnsetLetter letter) {
        return onsetMask(ExtraOnsetLetters().with(letter, true));
    };
    CHECK(only(OnsetLetter::Z) == ONSET_Z);
    CHECK(only(OnsetLetter::F) == ONSET_F);
    CHECK(only(OnsetLetter::W) == ONSET_W);
    CHECK(only(OnsetLetter::J) == ONSET_J);
}

TEST_CASE("the mask is empty for none and complete for all") {
    CHECK(onsetMask(ExtraOnsetLetters()) == 0);
    CHECK(onsetMask(ExtraOnsetLetters::all()) == (ONSET_F | ONSET_J | ONSET_W | ONSET_Z));
    CHECK(onsetMask(ExtraOnsetLetters::fromId("zj")) == (ONSET_Z | ONSET_J));
}
