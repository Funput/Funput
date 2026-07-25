#include "funput_engine.h"

#include <fcitx/inputcontext.h>

FunputEngine::FunputEngine(fcitx::Instance *instance) : instance_(instance) {
    applySettings();
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
    if (!settings_.reload()) return;
    applySettings();
    applyPerAppDefault(lastProgram_);
}

void FunputEngine::applySettings() {
    handle_.configure(settings_);
    effectiveEnabled_ = settings_.enabled;
    handle_.setEnabled(effectiveEnabled_);
    handle_.clearShortcuts();
    for (const auto &[trigger, expansion] : settings_.shortcuts) {
        handle_.addShortcut(trigger, expansion);
    }
    handle_.clear();
}

void FunputEngine::applyPerAppDefault(const std::string &program) {
    const bool enabled = settings_.isExcluded(program) ? false : settings_.enabled;
    if (enabled == effectiveEnabled_) return;
    effectiveEnabled_ = enabled;
    handle_.setEnabled(enabled);
    handle_.clear();
}

void FunputEngine::reset(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    commitBuffer(event.inputContext());
}

void FunputEngine::activate(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    if (settings_.reloadIfChanged()) applySettings();
    if (settings_.autoCapitalize) handle_.armCapitalization();
    lastProgram_ = event.inputContext()->program();
    applyPerAppDefault(lastProgram_);
    noteRecentApp(lastProgram_);
}

void FunputEngine::deactivate(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    commitBuffer(event.inputContext());
}

FCITX_ADDON_FACTORY(FunputEngineFactory)
