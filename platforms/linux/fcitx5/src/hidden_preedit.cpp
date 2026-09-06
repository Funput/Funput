// Clients that advertise Preedit but paint nothing. The composing word is
// invisible until a commit. Non-preedit cannot fill that gap when surrounding
// text is missing or deleteSurroundingText is not honoured, so the remaining
// channel is the Fcitx5-drawn panel preedit. IBus has no equivalent that works;
// this file is Fcitx5-only.
//
// Adding an app: put its process basename in kHiddenPreedit below.
// updatePreedit() already draws the panel for anyone on the list. Do not match
// on capability flags or missing surrounding text — those lie both ways, and
// would pull in clients that already work.

#include "funput_engine.h"

#include <cctype>
#include <string_view>

namespace {

constexpr std::string_view kHiddenPreedit[] = {
    "wps",    // WPS Writer
    "wpp",    // WPS Presentation
    "et",     // WPS Spreadsheets
    "wpspdf", // WPS PDF
};

std::string_view basenameOf(std::string_view path) {
    const auto slash = path.find_last_of('/');
    return slash == std::string_view::npos ? path : path.substr(slash + 1);
}

bool equalsIgnoreCase(std::string_view left, std::string_view right) {
    if (left.size() != right.size()) return false;
    for (size_t i = 0; i < left.size(); ++i) {
        const auto a = static_cast<unsigned char>(left[i]);
        const auto b = static_cast<unsigned char>(right[i]);
        if (std::tolower(a) != std::tolower(b)) return false;
    }
    return true;
}

} // namespace

bool hidesClientPreedit(fcitx::InputContext *context) {
    const std::string_view name = basenameOf(context->program());
    for (const auto binary : kHiddenPreedit) {
        if (equalsIgnoreCase(name, binary)) return true;
    }
    return false;
}
