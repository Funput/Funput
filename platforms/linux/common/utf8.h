// UTF-8 <-> Unicode scalar marshalling for the Linux input-method shells.
//
// The `funput-ffi` C ABI carries text as UTF-32 codepoint arrays, while Fcitx5/IBus
// speak UTF-8; these are the single decode/encode pair behind every buffer read,
// committed word, and gõ tắt string. Mirrors `funput-ffi`'s `abi::codec`.

#ifndef FUNPUT_UTF8_H
#define FUNPUT_UTF8_H

#include <cstdint>
#include <string>
#include <vector>

namespace funput {

// Decode a UTF-8 string into Unicode scalars (the inverse of appendUtf8). Invalid
// bytes are skipped. Used to marshal gõ tắt strings to the FFI's uint32_t arrays.
inline std::vector<uint32_t> decodeUtf8(const std::string &s) {
    std::vector<uint32_t> out;
    size_t i = 0;
    const size_t n = s.size();
    while (i < n) {
        const auto b0 = static_cast<unsigned char>(s[i]);
        uint32_t cp = 0;
        size_t extra = 0;
        if (b0 < 0x80) {
            cp = b0;
        } else if ((b0 & 0xE0) == 0xC0) {
            cp = b0 & 0x1F;
            extra = 1;
        } else if ((b0 & 0xF0) == 0xE0) {
            cp = b0 & 0x0F;
            extra = 2;
        } else if ((b0 & 0xF8) == 0xF0) {
            cp = b0 & 0x07;
            extra = 3;
        } else {
            ++i; // invalid lead byte — skip
            continue;
        }
        if (i + extra >= n) break; // truncated sequence
        bool ok = true;
        for (size_t k = 1; k <= extra; ++k) {
            const auto bk = static_cast<unsigned char>(s[i + k]);
            if ((bk & 0xC0) != 0x80) {
                ok = false;
                break;
            }
            cp = (cp << 6) | (bk & 0x3F);
        }
        if (ok) {
            out.push_back(cp);
            i += extra + 1;
        } else {
            ++i; // malformed continuation — skip the lead byte
        }
    }
    return out;
}

// Append one Unicode scalar to a UTF-8 string.
inline void appendUtf8(std::string &out, uint32_t cp) {
    if (cp < 0x80) {
        out.push_back(static_cast<char>(cp));
    } else if (cp < 0x800) {
        out.push_back(static_cast<char>(0xC0 | (cp >> 6)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    } else if (cp < 0x10000) {
        out.push_back(static_cast<char>(0xE0 | (cp >> 12)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    } else {
        out.push_back(static_cast<char>(0xF0 | (cp >> 18)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
        out.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    }
}
} // namespace funput

#endif // FUNPUT_UTF8_H
