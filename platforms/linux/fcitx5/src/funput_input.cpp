// The Fcitx5 side of one keystroke: normalize the key, hand it to the composer,
// perform the plan. The typing rules themselves live in common/compose/.

#include "funput_engine.h"

#include <fcitx/inputpanel.h>
#include <fcitx/text.h>
#include <fcitx/userinterface.h>

#include "probe/probe.h"

namespace {

// Fcitx5's key -> the composer's normalized one. Its keysyms are X11 keysyms, the
// same values IBus uses, so they pass straight through; only the modifier set and
// the keysym-to-Unicode mapping are Fcitx5's own.
funput::KeyEvent toKeyEvent(const fcitx::Key &key) {
    funput::KeyEvent ev;
    ev.keysym = static_cast<uint32_t>(key.sym());
    ev.ch = static_cast<char32_t>(fcitx::Key::keySymToUnicode(key.sym()));
    const auto states = key.states();
    ev.mods.ctrl = states.test(fcitx::KeyState::Ctrl);
    ev.mods.alt = states.test(fcitx::KeyState::Alt);
    ev.mods.super = states.test(fcitx::KeyState::Super);
    ev.mods.shift = states.test(fcitx::KeyState::Shift);
    return ev;
}

// Is the client holding a selection right now? Non-preedit repairs the document by
// deleting backwards from the caret, and with a selection live that delete takes the
// highlighted text instead — the browser-autofill hazard question 5 of the probe
// went looking for. It never caught one in 93 commits, but "not seen" is not
// "cannot happen" and the price is the user's own words.
bool hasSelection(fcitx::InputContext *context) {
    const auto &surrounding = context->surroundingText();
    return surrounding.isValid() && surrounding.cursor() != surrounding.anchor();
}

} // namespace

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
            // Snapshot before the client sees it — see src/probe/probe.h.
            funput::probe::noteCommit(context, plan.text);
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
                // own. Give up on the repair *and* the composition: leaving the
                // engine believing it owns a word it could not correct would send the
                // next keystroke's delete into text we never wrote.
                composer_.discard();
                break;
            }
            context->deleteSurroundingText(-static_cast<int>(plan.deleteChars), plan.deleteChars);
        }
        if (!plan.text.empty()) {
            funput::probe::noteCommit(context, plan.text);
            context->commitString(plan.text);
        }
        break;
    }
}

void FunputEngine::keyEvent(const fcitx::InputMethodEntry &, fcitx::KeyEvent &event) {
    // Releases never reach the composer: nothing in the typing rules depends on
    // them, and each framework reports them differently.
    if (event.isRelease()) return;

    // Ctrl+Alt+P, and only with FUNPUT_PROBE=1 — see src/probe/probe.h. Checked
    // ahead of the composer so the chord never reaches the typing rules.
    if (funput::probe::maybeStartSelfTest(event)) {
        event.filterAndAccept();
        return;
    }

    const funput::ComposePlan plan = composer_.onKey(toKeyEvent(event.key()));
    applyPlan(event.inputContext(), plan);
    if (plan.consumed) event.filterAndAccept();
}
