// "Phụ âm đầu mở rộng" reaching the real engine through the composer: the letters
// in Settings are pushed by `applySettings()`, the one path a shell's startup, its
// file watcher and its focus-in check all take.

#include <doctest/doctest.h>

#include "support.h"

using namespace funput;
using namespace funput::test;

namespace {

Composer composerWith(Method method, const char *letters) {
    Settings settings;
    settings.method = method;
    settings.nonPreedit = false;
    settings.extraOnsets = ExtraOnsetLetters::fromId(letters);
    return Composer(settings);
}

// The word committed by typing `keys` and then a space.
std::string commit(Composer &composer, const std::string &keys) {
    type(composer, keys);
    return composer.onKey(ascii(' ')).text;
}

} // namespace

TEST_CASE("without extra onsets a z-word stays English") {
    Composer composer = composerWith(Method::Telex, "");
    CHECK(commit(composer, "zoo") == "zoo ");
}

TEST_CASE("an admitted letter opens a syllable like a native onset") {
    Composer composer = composerWith(Method::Telex, "z");
    CHECK(commit(composer, "zoo") == "zô ");
    // Only the chosen letters: f is still not an onset.
    CHECK(commit(composer, "fair") == "fair ");
}

TEST_CASE("every letter, and a rhyme Vietnamese lacks, behave as on other platforms") {
    Composer composer = composerWith(Method::Telex, "zfwj");
    CHECK(commit(composer, "jowf") == "jờ ");
    CHECK(commit(composer, "was") == "wá ");
    CHECK(commit(composer, "fair") == "fải ");
    CHECK(commit(composer, "food") == "food ");
}

TEST_CASE("VNI and Telex nâng cao read the letters too") {
    Composer vni = composerWith(Method::Vni, "zj");
    CHECK(commit(vni, "zo6") == "zô ");
    CHECK(commit(vni, "jo72") == "jờ ");

    // Telex nâng cao keeps w → ư; a doubled w is the consonant.
    Composer advanced = composerWith(Method::TelexAdvanced, "w");
    CHECK(commit(advanced, "wa") == "ưa ");
    CHECK(commit(advanced, "wwas") == "wá ");
}

TEST_CASE("a settings change applies without a new composer") {
    Composer composer = composerWith(Method::Telex, "");
    composer.settings().extraOnsets = ExtraOnsetLetters::fromId("z");
    composer.applySettings();
    CHECK(commit(composer, "zoo") == "zô ");

    composer.settings().extraOnsets = ExtraOnsetLetters();
    composer.applySettings();
    CHECK(commit(composer, "zoo") == "zoo ");
}
