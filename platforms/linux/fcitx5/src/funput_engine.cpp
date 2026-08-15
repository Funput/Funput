#include "funput_engine.h"

#include <fcitx/event.h>
#include <fcitx/inputcontext.h>

#include "probe/probe.h"

FunputEngine::FunputEngine(fcitx::Instance *instance) : instance_(instance) {
    if (funput::probe::enabled()) {
        // The client publishes surrounding text on its own schedule, so the only way
        // to measure how late it is relative to our commit is to watch for it.
        probeWatch_ = instance_->watchEvent(
            fcitx::EventType::InputContextSurroundingTextUpdated,
            fcitx::EventWatcherPhase::Default, [](fcitx::Event &event) {
                auto &contextEvent = static_cast<fcitx::InputContextEvent &>(event);
                funput::probe::noteSurroundingUpdate(contextEvent.inputContext());
            });
    }
    if (settingsWatcher_.fd() >= 0) {
        settingsWatch_ = instance_->eventLoop().addIOEvent(
            settingsWatcher_.fd(), fcitx::IOEventFlag::In,
            [this](fcitx::EventSourceIO *, int, fcitx::IOEventFlags) {
                if (settingsWatcher_.drain()) onSettingsChanged();
                return true;
            });
    }
}

void FunputEngine::onSettingsChanged() {
    if (!composer_.reloadSettings()) return;
    composer_.applySettings();
    composer_.applyPerAppDefault(lastProgram_);
}

void FunputEngine::reset(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    applyPlan(event.inputContext(), composer_.flush());
}

void FunputEngine::activate(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    if (composer_.reloadSettingsIfChanged()) composer_.applySettings();
    if (composer_.settings().autoCapitalize) composer_.armCapitalization();
    lastProgram_ = event.inputContext()->program();
    composer_.applyPerAppDefault(lastProgram_);
    noteRecentApp(lastProgram_);
    funput::probe::noteFocus(event.inputContext());
}

void FunputEngine::deactivate(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    auto *context = event.inputContext();
    if (event.type() == fcitx::EventType::InputContextFocusOut) {
        // The composing word is already on its way to the client: Fcitx5 commits
        // clientPreedit() in its ReservedFirst focus-out watcher, which runs before
        // this one, or the client commits it itself when it advertises
        // ClientUnfocusCommit. A second commit here would duplicate the word.
        composer_.discard();
        clearPreedit(context);
        return;
    }
    // Input-method switch (group change, capability change): nothing else flushes
    // the preedit, so commit it ourselves.
    applyPlan(context, composer_.flush());
}

FCITX_ADDON_FACTORY(FunputEngineFactory)
