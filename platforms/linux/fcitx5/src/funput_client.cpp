// The Fcitx5 side of a ComposePlan: how a preedit, a commit and a document repair
// reach the client. The mirror of ibus/src/engine/client.cpp — one file per shell for
// the half that writes, so the half that reads keys stays about keys.

#include "funput_engine.h"

#include <cstdint>
#include <vector>

#include <fcitx/inputpanel.h>
#include <fcitx/text.h>
#include <fcitx/userinterface.h>

// The document in front of the caret, as UTF-8. Fcitx5 reports the cursor in
// characters while the text is UTF-8, so the two have to be reconciled here —
// slicing by the cursor as if it were a byte offset cuts Vietnamese in half.
std::string textBeforeCaret(fcitx::InputContext *context) {
    const auto &surrounding = context->surroundingText();
    if (!surrounding.isValid()) return {};
    const std::vector<uint32_t> chars = funput::decodeUtf8(surrounding.text());
    std::string out;
    for (size_t i = 0; i < chars.size() && i < surrounding.cursor(); ++i) {
        funput::appendUtf8(out, chars[i]);
    }
    return out;
}

// Who this input context is, for the non-preedit verdict to be remembered against.
//
// `program()` when it names one app, so a verdict survives clicking away and back. It
// is documented as possibly empty, and on GNOME/Wayland it is worse than empty: every
// client answers `gnome-shell`, the proxy they all speak through (see the measurements
// in README Linux). Remembering that name would stand the mode down for every app at
// once — curing one broken client by breaking the feature for everyone, which is the
// mistake this whole mechanism is built to avoid. Excluded by name rather than guessed
// at, the same way hidden_preedit.cpp names the clients it is about.
//
// Either way it falls back to the input context's own uuid. That is narrower — a real
// focus-out and back may build a new context — but it is the part that matters here:
// Fcitx5 calls `activate()` on a capability change as well as on a focus change, and
// that keeps the same context, so a client that churns capabilities (Cursor's editor
// does, constantly) can no longer wipe a verdict it has already earned.
std::string clientId(fcitx::InputContext *context) {
    const std::string &program = context->program();
    if (!program.empty() && program != "gnome-shell") return program;
    std::string hex;
    hex.reserve(context->uuid().size() * 2);
    for (const uint8_t byte : context->uuid()) {
        static constexpr char kDigits[] = "0123456789abcdef";
        hex.push_back(kDigits[byte >> 4]);
        hex.push_back(kDigits[byte & 0x0F]);
    }
    return hex;
}

// Is the client holding a selection right now? Non-preedit repairs the document by
// deleting backwards from the caret, and with a selection live that delete takes the
// highlighted text instead — the browser-autofill hazard. Measuring never caught a
// live selection in 93 commits, but "not seen" is not "cannot happen" and the price
// is the user's own words, so this guard is reasoned rather than observed.
bool hasSelection(fcitx::InputContext *context) {
    const auto &surrounding = context->surroundingText();
    return surrounding.isValid() && surrounding.cursor() != surrounding.anchor();
}

void FunputEngine::updatePreedit(fcitx::InputContext *context, const std::string &text) {
    fcitx::Text preedit;
    if (!text.empty()) preedit.append(text, fcitx::TextFormatFlag::Underline);
    preedit.setCursor(static_cast<int>(text.size()));
    auto &panel = context->inputPanel();
    // Always publish the client preedit, even for clients that cannot render one:
    // Fcitx5 commits clientPreedit() itself when the input context loses focus
    // (Instance's ReservedFirst focus-out watcher), which is what keeps a half-typed
    // word from vanishing on a click into another field or window. Display is
    // unaffected — InputContext::updatePreedit() returns early without the Preedit
    // capability, so those clients still get the Fcitx5-drawn panel preedit below.
    panel.setClientPreedit(preedit);
    if (!context->capabilityFlags().test(fcitx::CapabilityFlag::Preedit) ||
        hidesClientPreedit(context)) {
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

void FunputEngine::applyPlan(fcitx::InputContext *context, const funput::ComposePlan &plan) {
    switch (plan.effect) {
    case funput::Effect::None:
        break;
    case funput::Effect::Preedit:
        updatePreedit(context, plan.text);
        break;
    case funput::Effect::Commit:
        // Always drop the preedit first; an empty text then means the composition
        // ended without producing anything.
        clearPreedit(context);
        if (!plan.text.empty()) {
            context->commitString(plan.text);
        }
        break;
    case funput::Effect::Replace:
        // Non-preedit's document repair: take back what the last keystroke wrote,
        // then write what this one produced. Both counts are in characters — the
        // engine hands out characters and deleteSurroundingText takes characters, so
        // nothing converts between them anywhere along the way.
        if (plan.deleteChars > 0) {
            if (hasSelection(context)) {
                // Deleting now would take the user's highlighted text instead of our
                // own. Give up on the repair *and* the composition: leaving the engine
                // believing it owns a word it could not correct would send the next
                // keystroke's delete into text we never wrote.
                composer_.discard();
                break;
            }
            context->deleteSurroundingText(-static_cast<int>(plan.deleteChars), plan.deleteChars);
        }
        if (!plan.text.empty()) {
            context->commitString(plan.text);
        }
        break;
    }
}
