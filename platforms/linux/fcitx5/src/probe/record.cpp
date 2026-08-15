// What each observed event contributes to the five questions in probe.h.

#include "probe/internal.h"
#include "probe/probe.h"

namespace funput::probe {

using detail::lastCommit;
using detail::snapshot;
using detail::write;

namespace {

// The capabilities that decide whether non-preedit is possible at all. Named rather
// than dumped as a bitmask so the log stays readable years from now.
struct NamedFlag {
    const char *name;
    fcitx::CapabilityFlag flag;
};
constexpr NamedFlag kFlags[] = {
    {"Preedit", fcitx::CapabilityFlag::Preedit},
    {"SurroundingText", fcitx::CapabilityFlag::SurroundingText},
    {"ClientUnfocusCommit", fcitx::CapabilityFlag::ClientUnfocusCommit},
    {"KeyEventOrderFix", fcitx::CapabilityFlag::KeyEventOrderFix},
    {"Password", fcitx::CapabilityFlag::Password},
    {"Sensitive", fcitx::CapabilityFlag::Sensitive},
    {"Terminal", fcitx::CapabilityFlag::Terminal},
    {"Disable", fcitx::CapabilityFlag::Disable},
};

// Byte offset of the `chars`-th codepoint, or npos past the end. Fcitx5 reports the
// cursor in characters while the text is UTF-8, so comparing "the text before the
// caret" needs this conversion — without it every field with trailing content
// reports a false mismatch.
size_t byteOffsetOfChar(const std::string &text, size_t chars) {
    size_t offset = 0;
    for (size_t seen = 0; seen < chars; ++seen) {
        if (offset >= text.size()) return std::string::npos;
        do {
            ++offset;
        } while (offset < text.size() &&
                 (static_cast<unsigned char>(text[offset]) & 0xC0) == 0x80);
    }
    return offset;
}

// Does the text in front of the caret now end with what we committed? Anything else
// means the client's surrounding text cannot be trusted as a verification signal —
// which decides whether non-preedit can self-check or has to run blind.
bool endsWithCommit(fcitx::InputContext *ic, const std::string &committed) {
    const auto &text = ic->surroundingText();
    if (!text.isValid()) return false;
    const size_t caret = byteOffsetOfChar(text.text(), text.cursor());
    if (caret == std::string::npos || caret < committed.size()) return false;
    return text.text().compare(caret - committed.size(), committed.size(), committed) == 0;
}

} // namespace

void noteFocus(fcitx::InputContext *ic) {
    if (!enabled() || ic == nullptr) return;
    nlohmann::json caps = nlohmann::json::array();
    for (const auto &[name, flag] : kFlags) {
        if (ic->capabilityFlags().test(flag)) caps.push_back(name);
    }
    lastCommit().pending = false;
    write({
        {"ev", "focus"},
        // Empty under Wayland text-input-v3, which carries no app id — question 4.
        {"app", ic->program()},
        // `frontend()` rather than the newer `frontendName()`: the latter does not
        // exist on older Fcitx5 (Debian bookworm), and this says the same thing —
        // x11 / wayland / dbus / xim, which is what question 4 turns on.
        {"frontend", ic->frontend() != nullptr ? ic->frontend() : "?"},
        {"caps", caps},
        {"surrounding", snapshot(ic)},
    });
}

void noteCommit(fcitx::InputContext *ic, const std::string &text) {
    if (!enabled() || ic == nullptr || text.empty()) return;
    lastCommit() = {text, detail::Clock::now(), true};
    write({
        {"ev", "commit"},
        {"app", ic->program()},
        {"text", text},
        {"before", snapshot(ic)},
    });
}

void noteSurroundingUpdate(fcitx::InputContext *ic) {
    if (!enabled() || ic == nullptr) return;
    nlohmann::json record{{"ev", "surrounding"}, {"app", ic->program()}, {"after", snapshot(ic)}};
    if (detail::LastCommit &last = lastCommit(); last.pending) {
        record["dtMs"] = std::chrono::duration_cast<std::chrono::milliseconds>(
                             detail::Clock::now() - last.at)
                             .count();
        record["matchesTail"] = endsWithCommit(ic, last.text);
        last.pending = false;
    }
    write(std::move(record));
}

} // namespace funput::probe
