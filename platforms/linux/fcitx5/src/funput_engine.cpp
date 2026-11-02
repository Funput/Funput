// The engine object: construction, the mode decision between words, settings and
// status. The focus, reset and switch callbacks: engine/focus.cpp.

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
            spokeSinceReset_ = true;
        });
    // The file itself is read at the first activate() and reaches the trace through
    // settingsReloaded(); until then the trace is whatever FUNPUT_TRACE says, which
    // shared() applied itself.
    funput::Trace::shared().start("fcitx5");
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
    // And the path to it must be able to carry a repair right now: on the gnome-shell
    // bridge the first word in an empty field, or the first after a click, would lose
    // its repairs for certain (transport/guard.h). That word is composed in a preedit
    // instead; the question is asked again at the next word, by which time the client
    // has answered with the word before it. Asked here and nowhere else, so the mode
    // still never changes under a word.
    const bool bridge = context && composer_.bridgeBlocksRepairs(
                                       spokeSinceReset_, textBeforeCaret(context));
    composer_.setNonPreedit(lastSurroundingOk_ && !hidden && !bridge);
    funput::Trace &trace = funput::Trace::shared();
    if (trace.enabled() && composer_.nonPreedit() != wasNonPreedit) {
        // Off although the client qualifies means the composer's own latch: it was
        // caught dropping a delete, and that verdict already stands in the trace.
        const char *reason = composer_.nonPreedit() ? "surrounding"
                             : !lastSurroundingOk_  ? "silent"
                             : hidden               ? "hidden-preedit"
                             : bridge               ? "bridge"
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

FCITX_ADDON_FACTORY(FunputEngineFactory)
