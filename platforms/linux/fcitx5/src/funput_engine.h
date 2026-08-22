// Fcitx5 input method addon for Funput.
//
// A thin adapter over `funput::Composer` (platforms/linux/common/compose/), which
// holds the typing rules and is shared with the IBus shell. This file only
// translates: `fcitx::KeyEvent` in, `funput::ComposePlan` out, performed against
// the input context.
//
// The composing word is shown as underlined preedit and committed on a word
// boundary, navigation key, or VI/EN toggle — the same shape as the macOS IMKit
// shell (platforms/macos/.../FunputInputController.swift), NOT the Windows
// backspace-injection path. On focus loss Fcitx5 itself commits the client preedit
// before calling deactivate(), so the word survives a click into another field —
// see deactivate(), which must not commit again.

#ifndef FUNPUT_ENGINE_H
#define FUNPUT_ENGINE_H

#include <memory>
#include <string>

#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputcontext.h>
#include <fcitx/inputmethodengine.h>
#include <fcitx/instance.h>
#include <fcitx-config/configuration.h>
#include <fcitx-config/option.h>
#include <fcitx-utils/event.h>
#include <fcitx-utils/handlertable.h>
#include <fcitx-utils/key.h>

#include "compose/composer/composer.h"
#include "settings/watch.h"

// The only Fcitx5-native config: a button that opens the GTK Settings app.
// Real preferences stay in ~/.config/Funput/settings.json — settings-gtk
// rewrites that file wholesale, so they must not be duplicated as Options.
// getConfigForInputMethod() defaults to getConfig(), so the IM's Configure
// button in fcitx5-configtool uses this too. configtool 5.1.6+ launches the
// command directly when ExternalOption is the only field.
FCITX_CONFIGURATION(
    FunputEngineConfig,
    fcitx::ExternalOption openSettings{
        this, "OpenSettings", "Open Funput Settings", "funput-settings"};);

// Reading the focused client. Defined in funput_client.cpp beside the writing half —
// talking to the client is one concern, whichever direction it goes.
std::string textBeforeCaret(fcitx::InputContext *context);
bool hasSelection(fcitx::InputContext *context);

class FunputEngine : public fcitx::InputMethodEngineV2 {
public:
    explicit FunputEngine(fcitx::Instance *instance);

    void keyEvent(const fcitx::InputMethodEntry &entry, fcitx::KeyEvent &keyEvent) override;
    void reset(const fcitx::InputMethodEntry &entry, fcitx::InputContextEvent &event) override;
    void activate(const fcitx::InputMethodEntry &entry, fcitx::InputContextEvent &event) override;
    void deactivate(const fcitx::InputMethodEntry &entry, fcitx::InputContextEvent &event) override;
    const fcitx::Configuration *getConfig() const override { return &config_; }

private:
    // Perform one plan against the client: show a preedit, or drop the preedit and
    // commit. Swallowing the key is the caller's job — only keyEvent() can.
    void applyPlan(fcitx::InputContext *ic, const funput::ComposePlan &plan);
    void updatePreedit(fcitx::InputContext *ic, const std::string &text);
    void clearPreedit(fcitx::InputContext *ic);
    void noteRecentApp(const std::string &program); // record for the Settings picker
    // Turn non-preedit on for the focused client: the setting has to ask for it and
    // the client has to have sent surrounding text this focus, since the mode repairs
    // the document by reading it back. No-op while a word is composing — the two
    // modes disagree about where that word lives. Re-applied after every
    // `applySettings()`, which reseeds the mode from the setting alone.
    void applyNonPreeditMode();
    // Reload settings live when the watcher fires (Settings app wrote the file), and
    // re-apply the per-app default for the currently-focused app.
    void onSettingsChanged();

    fcitx::Instance *instance_;
    FunputEngineConfig config_;
    funput::Composer composer_;
    // Program() of the most recently focused app, so a live settings reload can
    // re-apply the per-app default without waiting for the next focus-in.
    std::string lastProgram_;
    // Whether the focused client has sent surrounding text this focus — the IBus
    // shell's `sawSurroundingText`. Not a snapshot of `isValid()` at focus-in: that
    // cache can be stale, and surrounding text often arrives only after the client
    // answers. Set by SurroundingTextUpdated; cleared on activate().
    bool lastSurroundingOk_ = false;
    // Live settings reload: an inotify fd (settingsWatcher_) wired into Fcitx5's
    // event loop (settingsWatch_).
    funput::SettingsWatcher settingsWatcher_;
    std::unique_ptr<fcitx::EventSourceIO> settingsWatch_;
    std::unique_ptr<fcitx::HandlerTableEntry<fcitx::EventHandler>> surroundingWatch_;
};

class FunputEngineFactory : public fcitx::AddonFactory {
public:
    fcitx::AddonInstance *create(fcitx::AddonManager *manager) override {
        return new FunputEngine(manager->instance());
    }
};

#endif // FUNPUT_ENGINE_H
