#include "funput_engine.h"

#include <fcitx/event.h>
#include <fcitx/inputcontext.h>
#include <fcitx/userinterface.h>

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
            // GTK field, and the IBus shell's "the client has spoken". Judging the
            // *content* here was tried and reverted: it kept the mode off in Chrome,
            // the client this feature exists for. What an answer is worth is decided
            // after a write, by BlindWrites, not before one.
            lastSurroundingOk_ = ic->surroundingText().isValid();
            surroundingFresh_ = true;
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
    applyNonPreeditMode();
    if (fcitx::InputContext *context = instance_->lastFocusedInputContext()) {
        refreshStatus(context);
        if (composer_.nonPreedit() == wasNonPreedit) return;
        // The mode just changed under a half-typed word. `applySettings()` dropped
        // it from the engine, but the preedit showing it belongs to this shell.
        clearPreedit(context);
    }
}

std::string FunputEngine::subMode(const fcitx::InputMethodEntry &,
                                  fcitx::InputContext &) {
    return composer_.enabled() ? "Tiếng Việt" : "Tiếng Anh";
}

std::string FunputEngine::subModeIconImpl(const fcitx::InputMethodEntry &,
                                          fcitx::InputContext &) {
    return composer_.enabled() ? "funput" : "funput-mono";
}

void FunputEngine::refreshStatus(fcitx::InputContext *ic) {
    if (!ic) return;
    ic->updateUserInterface(fcitx::UserInterfaceComponent::StatusArea, true);
}

void FunputEngine::reset(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    applyPlan(event.inputContext(), composer_.flush());
}

void FunputEngine::activate(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    if (composer_.reloadSettingsIfChanged()) composer_.applySettings();
    if (composer_.settings().autoCapitalize) composer_.armCapitalization();
    toggleChord_.reset();
    // A new client: forget whether the last one could be trusted with a repair. Named,
    // because this runs on a capability change too, not only on a focus change — an
    // unnamed reset would hand the mode back to a client already caught breaking it
    // every time it toggled a capability, and Cursor's editor toggles them constantly.
    composer_.onFocusChanged(clientId(event.inputContext()));
    // Like IBus `sawSurroundingText`: a new client has said nothing yet. `isValid()`
    // here can be leftover from the previous focus, so do not snapshot it — and the
    // last client's answer must not be credited to this one's first keystroke either.
    lastSurroundingOk_ = false;
    surroundingFresh_ = false;
    applyNonPreeditMode();
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
