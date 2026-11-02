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
    // The file itself is read at the first activate(); configuring from the defaults
    // here is what lets FUNPUT_TRACE in the environment count before that.
    funput::Trace::shared().start("fcitx5");
    funput::Trace::shared().configure(composer_.settings());
}

void FunputEngine::applyNonPreeditMode() {
    // Never mid-word. The two modes disagree about where the composing word lives,
    // so flipping under one leaves the engine and the client describing different
    // things. Same gate as the IBus shell's applyNonPreeditMode().
    if (composer_.isComposing()) return;
    const bool wasNonPreedit = composer_.nonPreedit();
    fcitx::InputContext *context = instance_->lastFocusedInputContext();
    // The client decides. It has to have spoken, and it must not be one the allowlist
    // names: those are documented not to take a document repair (WPS turned
    // `nguyen64` into `nguyenênễn`), and with no setting to switch the mode off there
    // is nobody left to spare them the first mangled word.
    const bool hidden = context && hidesClientPreedit(context);
    composer_.setNonPreedit(lastSurroundingOk_ && !hidden);
    funput::Trace &trace = funput::Trace::shared();
    if (trace.enabled() && composer_.nonPreedit() != wasNonPreedit) {
        // Off although the client qualifies means the composer's own latch: it was
        // caught dropping a delete, and that verdict already stands in the trace.
        const char *reason = composer_.nonPreedit() ? "surrounding"
                             : !lastSurroundingOk_  ? "silent"
                             : hidden               ? "hidden-preedit"
                                                    : "refused";
        trace.mode(composer_.nonPreedit(), reason);
    }
    if (wasNonPreedit || !composer_.nonPreedit()) return;
    // Mode just turned on between words. A leftover preedit would sit as a ghost
    // while the new mode writes into the document — cheap if the panel is already
    // empty.
    if (context) clearPreedit(context);
}

void FunputEngine::onSettingsChanged() {
    if (!composer_.reloadSettings()) return;
    settingsReloaded();
    if (fcitx::InputContext *context = instance_->lastFocusedInputContext()) {
        refreshStatus(context);
    }
}

void FunputEngine::settingsReloaded() {
    funput::Trace::shared().configure(composer_.settings());
    composer_.applySettings();
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
    funput::Trace::shared().note("reset");
    applyPlan(event.inputContext(), composer_.flush());
}

void FunputEngine::activate(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    if (composer_.reloadSettingsIfChanged()) settingsReloaded();
    if (composer_.settings().autoCapitalize) composer_.armCapitalization();
    toggleChord_.reset();
    // This also runs on a capability change, and then it is the same client carrying
    // on: Cursor's editor toggles capabilities constantly. Only a real focus-in starts a
    // new one, so only then forget whether the last could be trusted with a repair — a
    // verdict has to survive the churn, and must not survive the focus, or on GNOME/Wayland
    // (one input context for every app) a client that broke the mode takes Chrome's with it.
    if (event.type() == fcitx::EventType::InputContextFocusIn) {
        // The program as Fcitx5 reports it, not clientId(): on GNOME/Wayland that is
        // `gnome-shell`, and seeing the bridge in the trace is the point.
        funput::Trace::shared().focus(event.inputContext()->program());
        composer_.onFocusChanged(clientId(event.inputContext()));
        // Like IBus `sawSurroundingText`: a new client has said nothing yet. `isValid()`
        // here can be leftover from the previous focus, so do not snapshot it — and the
        // last client's answer must not be credited to this one's first keystroke.
        lastSurroundingOk_ = false;
        surroundingFresh_ = false;
    }
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
        funput::Trace::shared().note("focus-out");
        composer_.discard();
        clearPreedit(context);
        return;
    }
    // Input-method switch (group change, capability change): nothing else flushes
    // the preedit, so commit it ourselves.
    applyPlan(context, composer_.flush());
}

FCITX_ADDON_FACTORY(FunputEngineFactory)
