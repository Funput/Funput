// How the write self-test gets started, and the evidence for why it is started this
// way. See the self-test section of probe.h.

#include "probe/probe.h"
#include "probe/support/internal.h"

#include <string>

#include <fcitx-utils/keysym.h>

namespace funput::probe {

namespace {

// Typed rather than chorded: a compositor can grab any key combination before the
// input method sees it, but not ordinary text. Four characters nobody types by
// accident, short enough to be worth typing by hand.
constexpr char kMagic[] = ";;;p";
constexpr size_t kMagicLen = sizeof(kMagic) - 1;

// The last few plain characters seen, so the sequence can be recognised as it is
// typed. Not a composition buffer — these keys still reach the composer normally.
std::string &recent() {
    static std::string value;
    return value;
}

// Feed one key and report whether the trigger sequence just completed. Anything that
// is not plain printable ASCII breaks the run, so the sequence has to be typed
// without interruption.
bool completesMagic(const fcitx::Key &key) {
    const uint32_t ch = fcitx::Key::keySymToUnicode(key.sym());
    if (ch == 0 || ch > 0x7F) {
        recent().clear();
        return false;
    }
    std::string &buffer = recent();
    buffer.push_back(static_cast<char>(ch));
    if (buffer.size() > kMagicLen) buffer.erase(0, buffer.size() - kMagicLen);
    return buffer == kMagic;
}

} // namespace

bool maybeStartSelfTest(fcitx::KeyEvent &event) {
    if (!enabled() || event.isRelease()) return false;
    const fcitx::Key key = event.key();
    const auto states = key.states();

    if (states.test(fcitx::KeyState::Ctrl) && states.test(fcitx::KeyState::Alt)) {
        // Pure diagnostic: whether *any* Ctrl+Alt chord reaches the input method.
        // The first round logged nothing for Ctrl+Alt+P, and this says whether the
        // compositor eats the whole family or only that one binding.
        detail::write({{"ev", "selftest"}, {"step", "chord-seen"}, {"sym", key.sym()}});
        recent().clear();
        if (key.sym() != FcitxKey_p && key.sym() != FcitxKey_P) return false;
        detail::startSelfTest(event.inputContext());
        return true;
    }

    // Any other modifier means this is a shortcut, not typing — the sequence has to
    // be plain keystrokes.
    if (states.test(fcitx::KeyState::Ctrl) || states.test(fcitx::KeyState::Alt) ||
        states.test(fcitx::KeyState::Super)) {
        recent().clear();
        return false;
    }

    if (!completesMagic(key)) return false;
    recent().clear();
    detail::startSelfTest(event.inputContext());
    // Swallow the final key so only the leading `;;;` reach the document.
    return true;
}

} // namespace funput::probe
