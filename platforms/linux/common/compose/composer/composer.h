// The Vietnamese composition state machine shared by the Linux shells.
//
// Owns everything that decides *what* happens for a keystroke: the engine handle,
// the persisted settings, and the runtime VI/EN state. It knows nothing about
// Fcitx5 or IBus — it takes a normalized [KeyEvent] and returns a [ComposePlan]
// the shell performs. That split is what keeps one copy of the typing rules
// instead of the two that Fcitx5 and IBus drifted into, and it is what let the
// non-preedit mode arrive as a new [Effect] rather than a second shell.
//
// Deliberately *not* here, because they are genuinely per-framework: publishing the
// preedit (Fcitx5 `setClientPreedit`; IBus `IBUS_ENGINE_PREEDIT_COMMIT`), including
// who commits on focus loss — see each shell's comments; and the settings file
// watcher, whose fd is wired into that framework's own event loop.

#ifndef FUNPUT_COMPOSE_COMPOSER_H
#define FUNPUT_COMPOSE_COMPOSER_H

#include <string>

#include "compose/composer/nonpreedit.h"
#include "compose/composer/state/applied.h"
#include "compose/key/classify.h"
#include "compose/plan.h"
#include "ffi/handle.h"
#include "settings/settings.h"

namespace funput {

class Composer {
public:
    // Settings are injected rather than read from disk, so a test never touches the
    // user's real settings.json; a shell starts from defaults and `reloadSettings()`.
    explicit Composer(Settings settings = {});

    // --- typing ---------------------------------------------------------------

    // Handle one key press. Key releases must be dropped by the shell first.
    ComposePlan onKey(const KeyEvent &ev);

    // Commit whatever is composing and end the composition, for an event that is
    // not a focus loss: Fcitx5's input-method-switch `deactivate()`. `consumed` is
    // meaningless here and is left false.
    ComposePlan flush();

    // Fcitx5's `reset()`: `flush()`, except on the gnome-shell bridge, where a reset
    // is a click and mutter has committed the preedit itself (state.cpp).
    ComposePlan onReset();

    // Drop the composition *without* committing it. Used on focus loss, where the
    // framework has already flushed the preedit to the client and committing again
    // would type the word twice. Tearing down the visible preedit is the shell's job.
    void discard();

    // Arm auto-capitalize for the next word (a no-op unless the setting is on).
    void armCapitalization();

    // --- non-preedit (nonpreedit.cpp) -----------------------------------------

    // Build the word in the document instead of in a preedit: every keystroke
    // emits an [Effect::Replace] that repairs what the previous one wrote. On from
    // the start; this is how a shell narrows it per client, never how it revives
    // a mode a client was caught breaking.
    void setNonPreedit(bool on);
    bool nonPreedit() const { return nonPreedit_.on; }

    // A client took focus: who it is (an empty name is a fresh question; a client
    // caught dropping a delete keeps that verdict) and what the writes travel
    // through. See compose/transport.h.
    void onFocusChanged(ClientIdentity client = {});

    // What the writes travel through, as told at focus. A shell asks this before it
    // reads the document for `bridgeBlocksRepairs()`: only the bridge can say yes.
    Transport transport() const { return nonPreedit_.transport; }

    // Whether the bridge this client sits behind is certain to drop the next repair,
    // so a word started now must not be typed straight. For the shell's mode decision
    // between words only; it accuses nobody. See transport/guard.h.
    bool bridgeBlocksRepairs(bool spokeSinceReset, const std::string &textBeforeCaret) const;

    // The document before the caret as it stands now, before this keystroke: checks
    // that the previous repair landed. `answered` is whether the client sent
    // surrounding text since the last keystroke. The result is for the trace only.
    Observation observeDocument(const std::string &textBeforeCaret, bool selectionLive = false,
                                bool answered = true, uint32_t selectedAfter = 0);

    // Whether a word is being composed right now: is a Backspace shortening a live
    // word or eating a committed one?
    bool isComposing() const { return !handle_.buffer().empty(); }

    // Right after `onKey()` handled a Backspace with nothing composing: if the
    // deletion leaves the caret at the end of a finished Vietnamese word, re-open it so
    // the next keystroke can still fix its tone. The word comes from the document as
    // the journal predicts it, not from a reading that may be a keystroke old. Returns
    // whether a word was taken — nothing is written either way. Refused over a live
    // selection and on a reading that may be a deletion behind. See nonpreedit/backspace.cpp.
    bool adoptWordBeforeBackspace();

    // --- settings & VI/EN -----------------------------------------------------

    // Re-read the settings file only if its mtime moved; returns true if any value
    // changed. Push the result with `applySettings()`.
    bool reloadSettingsIfChanged();
    // Force a re-read regardless of mtime, for when the watcher already said the
    // file changed. Returns true if any value changed.
    bool reloadSettings();
    // Push `settings()` into the engine; resets VI/EN and the word unless only gõ tắt moved.
    void applySettings();

    // Flip VI/EN, committing whatever was composing first, and persist the choice.
    ComposePlan toggleEnabled();

    bool enabled() const { return effectiveEnabled_; }
    Settings &settings() { return settings_; }
    const Settings &settings() const { return settings_; }

private:
    // Commit the composing word and end the composition. `consumed` is the caller's
    // call: a word boundary swallows its key, a shortcut does not.
    ComposePlan commitBuffer(bool consumed);
    // A word boundary arrived while composing: commit the finished word plus the
    // boundary character itself as one string.
    ComposePlan onBoundary(char32_t scalar, KeySource source);
    bool englishShortcutsActive() const;

    // --- non-preedit (nonpreedit.cpp) -----------------------------------------

    // One engine result as a document repair.
    ComposePlan planFromResult(const FunputResult &result, char32_t typed);
    // A Backspace with nothing composing: who removes the character.
    ComposePlan backspaceOutsideWord();
    // An edit the app is about to make itself — `deleted` characters, then `typed`
    // (0 for none) — journaled so the next reading can confirm it. Passes the key on.
    ComposePlan appPerforms(uint32_t deleted, char32_t typed);
    // End the composition without committing: in non-preedit the word is already
    // in the document, so only the engine state is dropped.
    ComposePlan endComposition(bool consumed);

    Handle handle_;
    Settings settings_;
    AppliedSettings applied_;
    // The VI/EN state actually in effect. Starts from `settings_.enabled`; a
    // manual toggle updates both this and the persisted setting.
    bool effectiveEnabled_ = true;
    // The mode in effect plus what has been learned about this client.
    NonPreeditState nonPreedit_;
};

} // namespace funput

#endif // FUNPUT_COMPOSE_COMPOSER_H
