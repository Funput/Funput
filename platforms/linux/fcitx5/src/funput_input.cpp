#include "funput_engine.h"

#include <fcitx/inputpanel.h>
#include <fcitx/text.h>
#include <fcitx/userinterface.h>
#include <fcitx-utils/keysym.h>

#include "boundary.h"

void FunputEngine::updatePreedit(fcitx::InputContext *context) {
    const std::string buffer = handle_.buffer();
    fcitx::Text preedit;
    if (!buffer.empty()) preedit.append(buffer, fcitx::TextFormatFlag::Underline);
    preedit.setCursor(static_cast<int>(buffer.size()));
    auto &panel = context->inputPanel();
    // Always publish the client preedit, even for clients that cannot render one:
    // Fcitx5 commits clientPreedit() itself when the input context loses focus
    // (Instance's ReservedFirst focus-out watcher), which is what keeps a half-typed
    // word from vanishing on a click into another field or window. Display is
    // unaffected — InputContext::updatePreedit() returns early without the Preedit
    // capability, so those clients still get the Fcitx5-drawn panel preedit below.
    panel.setClientPreedit(preedit);
    if (!context->capabilityFlags().test(fcitx::CapabilityFlag::Preedit)) {
        panel.setPreedit(preedit);
    }
    context->updatePreedit();
    context->updateUserInterface(fcitx::UserInterfaceComponent::InputPanel);
}

void FunputEngine::clearPreedit(fcitx::InputContext *context) {
    context->inputPanel().reset();
    context->updatePreedit();
    context->updateUserInterface(fcitx::UserInterfaceComponent::InputPanel);
}

void FunputEngine::commitBuffer(fcitx::InputContext *context) {
    const std::string buffer = handle_.buffer();
    handle_.clear();
    clearPreedit(context);
    if (!buffer.empty()) context->commitString(buffer);
}

bool FunputEngine::handleBoundary(fcitx::InputContext *context, char32_t scalar,
                                  funput::KeySource source) {
    const std::string before = handle_.buffer();
    if (before.empty()) {
        if (settings_.autoCapitalize) handle_.process(static_cast<uint32_t>(scalar), source);
        return false;
    }
    const FunputResult result = handle_.process(static_cast<uint32_t>(scalar), source);
    std::string word = before;
    if (result.action == ACTION_SEND) {
        word = funput::Handle::output(result);
        if (!word.empty()) word.pop_back();
    }
    std::string boundary;
    funput::appendUtf8(boundary, static_cast<uint32_t>(scalar));
    handle_.clear();
    clearPreedit(context);
    context->commitString(word + boundary);
    return true;
}

void FunputEngine::keyEvent(const fcitx::InputMethodEntry &, fcitx::KeyEvent &event) {
    if (event.isRelease()) return;
    auto *context = event.inputContext();
    const fcitx::Key key = event.key();
    if (matchesToggle(key)) {
        toggleEnabled(context);
        event.filterAndAccept();
        return;
    }
    if (!effectiveEnabled_) return;
    if (matchesFlip(key)) {
        if (handle_.flipComposing().action != ACTION_NONE) updatePreedit(context);
        event.filterAndAccept();
        return;
    }
    const auto states = key.states();
    if (states.test(fcitx::KeyState::Ctrl) || states.test(fcitx::KeyState::Alt) ||
        states.test(fcitx::KeyState::Super)) {
        commitBuffer(context);
        return;
    }
    if (key.sym() == FcitxKey_BackSpace) {
        if (!handle_.buffer().empty()) {
            handle_.backspace();
            updatePreedit(context);
            event.filterAndAccept();
        }
        return;
    }
    const uint32_t scalar = fcitx::Key::keySymToUnicode(key.sym());
    if (scalar == 0) {
        if (settings_.autoCapitalize && key.sym() == FcitxKey_Return) {
            handle_.armCapitalization();
        }
        commitBuffer(context);
        return;
    }
    // A numpad digit is a literal number, not a VNI tone/shape: commit the current
    // word and emit the digit, like a word boundary (mirrors the macOS shell).
    if (funput::isNumpadDigitKeysym(static_cast<uint32_t>(key.sym()))) {
        if (handleBoundary(context, static_cast<char32_t>(scalar), funput::KeySource::Numpad))
            event.filterAndAccept();
        return;
    }
    if (funput::isBoundary(static_cast<char32_t>(scalar), settings_.method)) {
        if (handleBoundary(context, static_cast<char32_t>(scalar))) event.filterAndAccept();
        return;
    }
    handle_.process(scalar);
    if (handle_.buffer().empty()) return;
    updatePreedit(context);
    event.filterAndAccept();
}
