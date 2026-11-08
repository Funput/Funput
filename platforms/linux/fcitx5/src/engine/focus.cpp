// The Fcitx5 callbacks around a keystroke: a client gained or lost focus, Fcitx5
// reset the composition, the input method was switched. One keystroke itself:
// src/funput_input.cpp. The mirror of ibus/src/engine/focus.cpp.

#include "funput_engine.h"

#include <fcitx/event.h>
#include <fcitx/inputcontext.h>

#include "compose/transport.h"

void FunputEngine::reset(const fcitx::InputMethodEntry &, fcitx::InputContextEvent &event) {
    funput::Trace::shared().note("reset");
    // On the gnome-shell bridge this is a click into the focused field: mutter resets
    // its input focus, and gnome-shell's `vfunc_reset` forwards the reset here and
    // nulls its copy of the surrounding text. Until the client answers again, every
    // delete is dropped — see transport/guard.h — so count the answers from here.
    spokeSinceReset_ = false;
    // Whether the composing word is committed depends on the same transport: mutter
    // has already committed the preedit on the bridge, and a flush would type it
    // twice. The composer decides (Composer::onReset, state.cpp).
    applyPlan(event.inputContext(), composer_.onReset());
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
        // The program as Fcitx5 reports it, not the identity: on GNOME/Wayland that is
        // `gnome-shell`, and seeing the bridge in the trace is the point.
        const std::string &program = event.inputContext()->program();
        funput::Trace::shared().focus(program);
        composer_.onFocusChanged(funput::identifyProgram(program));
        // Like IBus `sawSurroundingText`: a new client has said nothing yet. `isValid()`
        // here can be leftover from the previous focus, so do not snapshot it — and the
        // last client's answer must not be credited to this one's first keystroke.
        lastSurroundingOk_ = false;
        surroundingFresh_ = false;
        spokeSinceReset_ = false;
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
