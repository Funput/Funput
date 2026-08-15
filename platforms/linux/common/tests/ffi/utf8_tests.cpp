#include <doctest/doctest.h>

#include <string>
#include <vector>

#include "ffi/utf8.h"

using namespace funput;

namespace {

std::string encode(const std::vector<uint32_t> &scalars) {
    std::string out;
    for (uint32_t cp : scalars) appendUtf8(out, cp);
    return out;
}

} // namespace

TEST_CASE("appendUtf8 encodes each sequence length") {
    CHECK(encode({U'A'}) == "A");                    // 1 byte
    CHECK(encode({U'ế'}) == "ế");               // 3 bytes, Vietnamese NFC
    CHECK(encode({0x00E9}) == "é");             // 2 bytes
    CHECK(encode({0x1F600}) == "\U0001F600");        // 4 bytes, past the BMP
    CHECK(encode({U'a', U'ế', 0x1F600}).size() == 8); // 1 + 3 + 4
}

TEST_CASE("decodeUtf8 inverts appendUtf8") {
    const std::vector<uint32_t> scalars = {U'T', U'i', U'ế', U'n', U'g', 0x1F600};
    CHECK(decodeUtf8(encode(scalars)) == scalars);
}

TEST_CASE("decodeUtf8 handles the empty string") {
    CHECK(decodeUtf8("").empty());
}

TEST_CASE("decodeUtf8 skips an invalid lead byte and keeps going") {
    // 0xFF is not a legal lead byte; the surrounding ASCII must survive.
    const std::string input = std::string("a") + '\xFF' + "b";
    CHECK(decodeUtf8(input) == std::vector<uint32_t>{U'a', U'b'});
}

TEST_CASE("decodeUtf8 stops at a truncated sequence") {
    // A 3-byte lead with only one continuation byte available.
    const std::string input = std::string("a") + '\xE1' + '\xBA';
    CHECK(decodeUtf8(input) == std::vector<uint32_t>{U'a'});
}

TEST_CASE("decodeUtf8 skips a lead byte with a malformed continuation") {
    // 0xC3 promises one continuation byte, but 'a' is not one — the lead byte is
    // dropped and decoding resumes at the character after it.
    const std::string input = std::string("\xC3") + "ab";
    CHECK(decodeUtf8(input) == std::vector<uint32_t>{U'a', U'b'});
}

TEST_CASE("decodeUtf8 round-trips a gõ tắt expansion") {
    // The real caller: marshalling shortcut strings to the FFI's uint32_t arrays.
    const std::string expansion = "Việt Nam";
    CHECK(encode(decodeUtf8(expansion)) == expansion);
}
