// What each observed event contributes to the five questions in probe.h.

#include "probe/support/internal.h"
#include "probe/probe.h"

namespace funput::probe {

using detail::advanceSelfTest;
using detail::cancelSelfTest;
using detail::lastCommit;
using detail::snapshot;
using detail::textBeforeCursor;
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

// Does the text in front of the caret now end with what we committed? Anything else
// means the client's surrounding text cannot be trusted as a verification signal —
// which decides whether non-preedit can self-check or has to run blind.
bool endsWithCommit(fcitx::InputContext *ic, const std::string &committed) {
    const std::string before = textBeforeCursor(ic);
    if (before.size() < committed.size()) return false;
    return before.compare(before.size() - committed.size(), committed.size(), committed) == 0;
}

} // namespace

void noteFocus(fcitx::InputContext *ic) {
    if (!enabled() || ic == nullptr) return;
    nlohmann::json caps = nlohmann::json::array();
    for (const auto &[name, flag] : kFlags) {
        if (ic->capabilityFlags().test(flag)) caps.push_back(name);
    }
    lastCommit().pending = false;
    // The field changed under any running self-test; its baseline is meaningless now.
    cancelSelfTest();
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
    // The self-test reads this update before we log it, so its own steps appear in
    // the log in the order they happened.
    advanceSelfTest(ic);
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
