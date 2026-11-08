// IBus engine for Funput.
//
// A thin adapter over `funput::Composer` (platforms/linux/common/compose/), which
// holds the typing rules and is shared with the Fcitx5 shell. This file's job is
// translation only: IBus keyvals in, a `funput::ComposePlan` performed against the
// engine out.

#ifndef FUNPUT_IBUS_ENGINE_INTERNAL_H
#define FUNPUT_IBUS_ENGINE_INTERNAL_H

#include <cstdint>
#include <string>

#include <ibus.h>

#include "compose/composer/composer.h"
#include "compose/key/chord.h"
#include "engine.h"
#include "settings/watch.h"

namespace funput_ibus {

struct EngineState {
    funput::Composer composer;
    funput::ToggleChord toggleChord;
    funput::SettingsWatcher watcher;
    guint watcherSource = 0;
    // Whether the focused client has actually sent surrounding text, as opposed to
    // claiming it can. `IBUS_CAP_SURROUNDING_TEXT` sits right there in
    // `engine->client_capabilities` and is not used on purpose: the same flag on
    // Fcitx5 was absent on contexts where surrounding text worked perfectly and
    // present on contexts that answered nothing (platforms/linux/README.md). Having
    // received text is a fact; advertising support is a promise.
    //
    // Cleared on focus change, because it describes the client, not the engine. It
    // therefore cannot be true at focus-in the way Fcitx5's `isValid()` can — only
    // once the client has spoken at least once.
    bool sawSurroundingText = false;
    // Whether the client has answered since the last keystroke — what tells an empty
    // answer apart from no answer. See BlindWrites.
    bool surroundingFresh = false;
    // Whether the client has answered since the last focus-in or `reset`. The
    // gnome-shell bridge drops every delete between a reset and the client's next
    // answer (compose/transport/guard.h); the guard reads this once the
    // focus is named `gnome-shell` (focus.cpp), and is inert while the focus is
    // unnamed, as it is on a daemon older than 1.5.27.
    bool spokeSinceReset = false;
    // Between a focus-in and its focus-out, and the input context's object path for
    // it: empty for a plain `focus-in`, which carries none. A daemon of 1.5.32 or
    // later can name a focus late — re-send a focus it has already delivered once
    // the engine's FocusId property has answered: bus/engineproxy.c
    // `_get_active_surrounding_text_cb` forces `has_focus` off and calls
    // `bus_engine_proxy_focus_in` again, so the engine sees `FocusIn` and then
    // `FocusInId`, or `FocusInId` twice. That second call must not pass as a new
    // client, which would clear the flags above while the daemon, which never
    // repeats itself, keeps the client's answer. The daemon keys its own dedupe on
    // the path (`bus_engine_proxy_focus_in`: `has_focus && !g_strcmp0 (object_path,
    // engine->object_path)`), so this does the same: a focus-in-id for the path in
    // hand, or for a focus that came unnamed, re-identifies that focus; any other
    // path is a new focus, however the daemon came to skip the focus-out.
    bool focused = false;
    std::string focusPath;
    // The client's last surrounding text, kept rather than read back on demand.
    // `ibus_engine_get_surrounding_text` hands out null here even right after the
    // client has sent a perfectly good string, so the only reliable copy is the one
    // that arrives in `set_surrounding_text`. Holding it also removes any question of
    // how fresh the framework's own cache is.
    std::string surroundingText;
    guint surroundingCursor = 0;  // in characters
    guint surroundingAnchor = 0;
};

EngineState *stateOf(IBusEngine *engine);

// Perform one plan against the client. Swallowing the key is the caller's job —
// only processKeyEvent() can, by returning TRUE.
void applyPlan(IBusEngine *engine, const funput::ComposePlan &plan);
void updatePreedit(IBusEngine *engine, const std::string &text);

// --- reading the document -----------------------------------------------------
//
// The IBus twins of `textBeforeCaret`, `hasSelection` and `selectedAfter` in
// fcitx5/src/funput_client.cpp; same names so both shells read alike. They return
// the "nothing usable" answer when the client has never sent surrounding text.

std::string textBeforeCaret(IBusEngine *engine);
bool hasSelection(IBusEngine *engine);
uint32_t selectedAfter(IBusEngine *engine);

// Decide whether the composer may build the word in the document: only once the
// client has sent surrounding text. Nothing else asks — there is no setting for it,
// so a settings reload has no say.
void applyNonPreeditMode(IBusEngine *engine);

// Tell the client this engine wants surrounding text. Per input context, not per
// engine, so it has to be re-asked on every focus.
void requestSurroundingText(IBusEngine *engine);

// The file was re-read and something moved: push it into the engine, and into the
// trace, whose switches are hidden keys in the same file (object.cpp).
void settingsReloaded(EngineState *state);

// One keystroke: callbacks.cpp. Everything around it: focus.cpp. The `Id` pair is
// what a daemon of 1.5.27 or later calls when the engine was built with
// `has-focus-id` (main.cpp); `client` is the string compose/transport.h reads.
gboolean processKeyEvent(IBusEngine *engine, guint keyval, guint keycode, guint modifiers);
void focusIn(IBusEngine *engine);
void focusInId(IBusEngine *engine, const gchar *objectPath, const gchar *client);
void focusOut(IBusEngine *engine);
void focusOutId(IBusEngine *engine, const gchar *objectPath);
void enable(IBusEngine *engine);
void disable(IBusEngine *engine);
void reset(IBusEngine *engine);
void setSurroundingText(IBusEngine *engine, IBusText *text, guint cursorPos, guint anchorPos);

} // namespace funput_ibus

#endif
