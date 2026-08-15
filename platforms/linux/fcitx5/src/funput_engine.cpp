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
