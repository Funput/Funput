// What the trace says about a string when it must not say the string.
//
// Funput is a privacy-first input method, and a diagnostic trace of every keystroke
// is one setting away from a keylogger. So by default a string is reduced to its
// character count and a keyed hash: enough to see that two readings are the same
// document, or that a repair deleted one character too many, without the text ever
// reaching the file. The key (siphash.h) is drawn once per process and never
// written, so the file alone does not let anyone test a guess — a word, a password
// — against a hash. Equal strings still hash equal within one trace.

#ifndef FUNPUT_TRACE_SUMMARY_TEXT_H
#define FUNPUT_TRACE_SUMMARY_TEXT_H

#include <cstdint>
#include <string>

#include "ffi/utf8.h"
#include "trace/summary/siphash.h"

namespace funput {

struct TextSummary {
    // Characters, not bytes — the unit every delete count in this codebase uses, so a
    // trace line can be read against a plan's `deleteChars` directly.
    uint32_t chars = 0;
    uint64_t hash = 0;
};

inline TextSummary summarize(const std::string &utf8, const SipKey &key) {
    return {static_cast<uint32_t>(decodeUtf8(utf8).size()), siphash24(utf8, key)};
}

} // namespace funput

#endif // FUNPUT_TRACE_SUMMARY_TEXT_H
