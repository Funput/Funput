// The Fcitx5 side of a ComposePlan: how a preedit, a commit and a document repair
// reach the client. The mirror of ibus/src/engine/client.cpp — one file per shell for
// the half that writes, so the half that reads keys stays about keys.

#include "funput_engine.h"

#include <cstdint>
#include <vector>

#include <fcitx/inputpanel.h>
#include <fcitx/text.h>
#include <fcitx/userinterface.h>

#include "compose/composer/nonpreedit/selection.h"

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

// Is the client holding a selection? Backspace must not take that key over, and
// a highlight that is not a suffix after the caret is the user's own text.
bool hasSelection(fcitx::InputContext *context) {
    const auto &surrounding = context->surroundingText();
    return surrounding.isValid() && surrounding.cursor() != surrounding.anchor();
}

uint32_t selectedAfter(fcitx::InputContext *context) {
    const auto &surrounding = context->surroundingText();
    if (!surrounding.isValid()) return 0;
    return funput::selectedAfterCaret(static_cast<uint32_t>(surrounding.cursor()),
                                      static_cast<uint32_t>(surrounding.anchor()));
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
        // Non-preedit's document repair: clear a selected suffix, take back what
        // the last keystroke wrote, then write what this one produced. Counts are
        // characters — the engine and deleteSurroundingText agree, so nothing
        // converts between them.
        if (plan.deleteAfterChars > 0) {
            context->deleteSurroundingText(0, static_cast<unsigned int>(plan.deleteAfterChars));
        }
        if (plan.deleteChars > 0) {
            context->deleteSurroundingText(-static_cast<int>(plan.deleteChars), plan.deleteChars);
        }
        if (!plan.text.empty()) {
            context->commitString(plan.text);
        }
        break;
    }
}
