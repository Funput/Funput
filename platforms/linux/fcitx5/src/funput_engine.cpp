#include "funput_engine.h"

#include <fcitx/event.h>
#include <fcitx/inputcontext.h>

FunputEngine::FunputEngine(fcitx::Instance *instance) : instance_(instance) {
    if (settingsWatcher_.fd() >= 0) {
        settingsWatch_ = instance_->eventLoop().addIOEvent(
            settingsWatcher_.fd(), fcitx::IOEventFlag::In,
            [this](fcitx::EventSourceIO *, int, fcitx::IOEventFlags) {
                if (settingsWatcher_.drain()) onSettingsChanged();
                return true;
            });
    }
    surroundingWatch_ = instance_->watchEvent(
        fcitx::EventType::InputContextSurroundingTextUpdated,
        fcitx::EventWatcherPhase::Default, [this](fcitx::Event &event) {
            auto *ic = static_cast<fcitx::InputContextEvent &>(event).inputContext();
            if (ic != instance_->lastFocusedInputContext()) return;
            // isValid(), not CapabilityFlag::SurroundingText: on GNOME/Wayland the
            // flag lies both ways. Empty text with cursor 0 is still valid — a blank
            // GTK field, and the IBus shell's "the client has spoken".
            lastSurroundingOk_ = ic->surroundingText().isValid();
        });
}

void FunputEngine::applyNonPreeditMode() {
    // Never mid-word. The two modes disagree about where the composing word lives,
    // so flipping under one leaves the engine and the client describing different
    // things. Same gate as the IBus shell's applyNonPreeditMode().
    if (composer_.isComposing()) return;
    const bool wasNonPreedit = composer_.nonPreedit();
    composer_.setNonPreedit(composer_.settings().nonPreedit && lastSurroundingOk_);
    if (wasNonPreedit || !composer_.nonPreedit()) return;
    // Mode just turned on between words. A leftover preedit would sit as a ghost
    // while the new mode writes into the document — same cleanup as a live settings
    // reload, cheap if the panel is already empty.
    if (fcitx::InputContext *context = instance_->lastFocusedInputContext()) {
        clearPreedit(context);
    }
}

void FunputEngine::onSettingsChanged() {
    if (!composer_.reloadSettings()) return;
    const bool wasNonPreedit = composer_.nonPreedit();
    composer_.applySettings();
    composer_.applyPerAppDefault(lastProgram_);
    applyNonPreeditMode();
    if (composer_.nonPreedit() == wasNonPreedit) return;
    // The mode just changed under a half-typed word. `applySettings()` dropped it
    // from the engine, but the preedit showing it belongs to this shell — left alone
    // it would sit on screen as a ghost while the new mode writes into the document.
    if (fcitx::InputContext *context = instance_->lastFocusedInputContext()) {
        clearPreedit(context);
    }
}

void FunputEngine::reset(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    applyPlan(event.inputContext(), composer_.flush());
}

void FunputEngine::activate(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    if (composer_.reloadSettingsIfChanged()) composer_.applySettings();
    if (composer_.settings().autoCapitalize) composer_.armCapitalization();
    // A new client: forget whether the last one could be trusted with a repair.
    composer_.onFocusChanged();
    lastProgram_ = event.inputContext()->program();
    composer_.applyPerAppDefault(lastProgram_);
    // Like IBus `sawSurroundingText`: a new client has said nothing yet. `isValid()`
    // here can be leftover from the previous focus, so do not snapshot it.
    lastSurroundingOk_ = false;
    applyNonPreeditMode();
    noteRecentApp(lastProgram_);
}

void FunputEngine::deactivate(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    auto *context = event.inputContext();
    if (event.type() == fcitx::EventType::InputContextFocusOut) {
        // The composing word is already on its way to the client: Fcitx5 commits
        // clientPreedit() in its ReservedFirst focus-out watcher, which runs before
        // this one, or the client commits it itself when it advertises
        // ClientUnfocusCommit. A second commit here would duplicate the word.
        //
        // Non-preedit needs no special case: there is no preedit for anyone to
        // commit, and the word is already in the document one keystroke at a time —
        // which is the whole point of the mode. Dropping the engine state is all
        // that is left to do either way.
        composer_.discard();
        clearPreedit(context);
        return;
    }
    // Input-method switch (group change, capability change): nothing else flushes
    // the preedit, so commit it ourselves.
    applyPlan(context, composer_.flush());
}

FCITX_ADDON_FACTORY(FunputEngineFactory)
