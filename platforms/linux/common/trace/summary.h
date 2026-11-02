// What the trace says about a string when it must not say the string.
//
// Funput is a privacy-first input method, and a diagnostic trace of every keystroke
// is one setting away from a keylogger. So by default a string is reduced to its
// character count and a salted hash: enough to see that two readings are the same
// document, or that a repair deleted one character too many, without the text ever
// reaching the file. The salt is drawn once per process and never written, which is
// what keeps a short string — a word, a password — from being reversed by hashing a
// dictionary against the trace. Equal strings still hash equal within one trace.

#ifndef FUNPUT_TRACE_SUMMARY_H
#define FUNPUT_TRACE_SUMMARY_H

#include <cstdint>
#include <string>

#include "ffi/utf8.h"

namespace funput {

struct TextSummary {
    // Characters, not bytes — the unit every delete count in this codebase uses, so a
    // trace line can be read against a plan's `deleteChars` directly.
    uint32_t chars = 0;
    uint32_t hash = 0;
};

// FNV-1a over the salt's bytes and then the UTF-8 bytes. Nothing cryptographic is
// asked of it: with the salt unknown the trace reader cannot try candidates, and
// within one trace equal input gives equal output. 32 bits keeps a line short.
inline uint32_t saltedHash(const std::string &utf8, uint64_t salt) {
    uint32_t hash = 2166136261u;
    const auto absorb = [&hash](unsigned char byte) {
        hash ^= byte;
        hash *= 16777619u;
    };
    for (int shift = 0; shift < 64; shift += 8) absorb(static_cast<unsigned char>(salt >> shift));
    for (const char c : utf8) absorb(static_cast<unsigned char>(c));
    return hash;
}

inline TextSummary summarize(const std::string &utf8, uint64_t salt) {
    return {static_cast<uint32_t>(decodeUtf8(utf8).size()), saltedHash(utf8, salt)};
}

} // namespace funput

#endif // FUNPUT_TRACE_SUMMARY_H
