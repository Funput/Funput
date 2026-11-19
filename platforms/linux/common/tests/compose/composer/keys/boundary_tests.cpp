// A boundary key commits what the engine wrote in full (compose/composer/keys/
// boundary.cpp): a gõ tắt expansion can be longer than the CHARS_CAP codepoints the
// inline `FunputResult.chars` holds, and was once cut off there on both paths.

#include <doctest/doctest.h>

#include "compose/composer/nonpreedit/sim/client.h"

using namespace funput;
using namespace funput::test;

namespace {

// 92 characters. The trigger avoids every Telex modifier key, so it composes nothing.
const std::string kLong = "Cộng hoà Xã hội Chủ nghĩa Việt Nam, Độc lập, Tự do, Hạnh phúc. "
                          "Số 1 Đường Hùng Vương, Hà Nội";

Composer composerWithLong(bool nonPreedit) {
    Settings settings;
    settings.shortcuts = {{"cvhn", kLong}};
    Composer composer(settings);
    composer.setNonPreedit(nonPreedit);
    return composer;
}

} // namespace

TEST_CASE("an expansion longer than the inline result is committed whole") {
    REQUIRE(decodeUtf8(kLong).size() > CHARS_CAP);
    for (bool nonPreedit : {false, true}) {
        CAPTURE(nonPreedit);
        for (char boundary : {' ', '.', ','}) {
            CAPTURE(boundary);
            Composer composer = composerWithLong(nonPreedit);
            CHECK(typeDocument(composer, std::string("cvhn") + boundary) == kLong + boundary);
        }
    }
}

TEST_CASE("a long expansion is taken back whole") {
    ClientSim sim;
    sim.composer().settings().shortcuts = {{"cvhn", kLong}};
    sim.composer().applySettings();
    REQUIRE(sim.type("cvhn ") == kLong + " ");
    sim.pressBackspace();
    CHECK(sim.document() == "cvhn ");
}
