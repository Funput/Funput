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
#include <fcitx-utils/event.h>
#include <fcitx-utils/key.h>

#include "compose/composer/composer.h"
#include "settings/watch.h"

class FunputEngine : public fcitx::InputMethodEngineV2 {
public:
    explicit FunputEngine(fcitx::Instance *instance);

    void keyEvent(const fcitx::InputMethodEntry &entry, fcitx::KeyEvent &keyEvent) override;
    void reset(const fcitx::InputMethodEntry &entry, fcitx::InputContextEvent &event) override;
    void activate(const fcitx::InputMethodEntry &entry, fcitx::InputContextEvent &event) override;
    void deactivate(const fcitx::InputMethodEntry &entry, fcitx::InputContextEvent &event) override;

private:
    // Perform one plan against the client: show a preedit, or drop the preedit and
    // commit. Swallowing the key is the caller's job — only keyEvent() can.
    void applyPlan(fcitx::InputContext *ic, const funput::ComposePlan &plan);
    void updatePreedit(fcitx::InputContext *ic, const std::string &text);
    void clearPreedit(fcitx::InputContext *ic);
    void noteRecentApp(const std::string &program); // record for the Settings picker
    // Reload settings live when the watcher fires (Settings app wrote the file), and
    // re-apply the per-app default for the currently-focused app.
    void onSettingsChanged();

    fcitx::Instance *instance_;
    funput::Composer composer_;
    // Phase 0 diagnostics for the non-preedit work; null unless FUNPUT_PROBE=1.
    // See src/probe/probe.h — this whole member goes away with that directory.
    std::unique_ptr<fcitx::HandlerTableEntry<fcitx::EventHandler>> probeWatch_;
    // Program() of the most recently focused app, so a live settings reload can
    // re-apply the per-app default without waiting for the next focus-in.
    std::string lastProgram_;
    // Live settings reload: an inotify fd (settingsWatcher_) wired into Fcitx5's
    // event loop (settingsWatch_).
    funput::SettingsWatcher settingsWatcher_;
    std::unique_ptr<fcitx::EventSourceIO> settingsWatch_;
};

class FunputEngineFactory : public fcitx::AddonFactory {
public:
    fcitx::AddonInstance *create(fcitx::AddonManager *manager) override {
        return new FunputEngine(manager->instance());
    }
};

#endif // FUNPUT_ENGINE_H
