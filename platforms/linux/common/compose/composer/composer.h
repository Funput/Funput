// The Vietnamese composition state machine shared by the Linux shells.
//
// Owns everything that decides *what* happens for a keystroke: the engine handle,
// the persisted settings, and the runtime VI/EN state. It knows nothing about
// Fcitx5 or IBus — it takes a normalized [KeyEvent] and returns a [ComposePlan]
// the shell performs. That split is what keeps one copy of the typing rules
// instead of the two that Fcitx5 and IBus drifted into, and it is what let the
// non-preedit mode arrive as a new [Effect] rather than a second shell.
//
// Deliberately *not* here, because they are genuinely per-framework:
//   - publishing the preedit (Fcitx5 `setClientPreedit` + its Preedit capability
//     check; IBus `IBUS_ENGINE_PREEDIT_COMMIT`), including who commits on focus
//     loss — see each shell's comments;
//   - the settings file watcher, whose fd must be wired into that framework's own
//     event loop. The shell owns a [SettingsWatcher] and calls `reloadSettings()`.

#ifndef FUNPUT_COMPOSE_COMPOSER_H
#define FUNPUT_COMPOSE_COMPOSER_H

#include <string>

#include "compose/key/classify.h"
#include "compose/plan.h"
#include "ffi/handle.h"
#include "settings/settings.h"

namespace funput {

class Composer {
public:
    // Settings are injected rather than read from disk, so a test never touches
    // the user's real ~/.config/Funput/settings.json. A shell constructs the
    // composer with defaults and then calls `reloadSettings()`.
    explicit Composer(Settings settings = {});

    // --- typing ---------------------------------------------------------------

    // Handle one key press. Key releases must be dropped by the shell first.
    ComposePlan onKey(const KeyEvent &ev);

    // Commit whatever is composing and end the composition. For the events that
    // are not a focus loss: Fcitx5's `reset()` and its input-method-switch
    // `deactivate()`. `consumed` is meaningless here and is left false.
    ComposePlan flush();

    // Drop the composition *without* committing it. Used on focus loss, where the
    // framework has already flushed the preedit to the client and committing again
    // would type the word twice. Tearing down the visible preedit afterwards is the
    // shell's job, because the two frameworks differ on whether one is needed.
    void discard();

    // Arm auto-capitalize for the next word (a no-op unless the setting is on).
    void armCapitalization();

    // --- non-preedit ----------------------------------------------------------

    // Build the word in the document instead of in a preedit: every keystroke
    // emits an [Effect::Replace] that repairs what the previous one wrote. See
    // nonpreedit.cpp for what the mode assumes of its shell.
    //
    // `applySettings()` seeds this from `Settings::nonPreedit`; the setter is the
    // per-context override on top of it, for a shell that finds a client the mode
    // cannot be run against. No shell performs an [Effect::Replace] yet, so turning
    // it on changes nothing outside the tests.
    void setNonPreedit(bool on) { nonPreedit_ = on; }
    bool nonPreedit() const { return nonPreedit_; }

    // Whether a word is being composed right now. The shells ask before deciding
    // whether a Backspace is shortening a live word or eating a committed one.
    bool isComposing() const { return !handle_.buffer().empty(); }

    // A Backspace is about to delete the last character of `textBeforeCaret` (the
    // shell passes the document as it stands *now*; the app has not acted yet). If
    // that leaves the caret at the end of a finished Vietnamese word, re-open it so
    // the next keystroke can still fix its tone — `phủ` Space Backspace `s` gives
    // `phú`. Returns whether a word was taken; nothing is written to the document
    // either way, so a false costs nothing.
    //
    // Only for non-preedit, where the word really is in the document. A preedit
    // shell has nothing to re-open: Backspace there shortens the composition.
    bool adoptWordBeforeBackspace(const std::string &textBeforeCaret);

    // --- settings & VI/EN -----------------------------------------------------

    // Re-read the settings file only if its mtime moved; returns true if any value
    // changed. Push the result with `applySettings()`.
    bool reloadSettingsIfChanged();
    // Force a re-read regardless of mtime, for when the watcher already said the
    // file changed. Returns true if any value changed.
    bool reloadSettings();
    // Push `settings()` into the engine and reset the runtime VI/EN state to it.
    void applySettings();

    // Flip VI/EN, committing whatever was composing first, and persist the choice.
    ComposePlan toggleEnabled();

    // Default this app to English when it is on the exclusion list, Vietnamese
    // otherwise. Only the Fcitx5 shell calls this today — IBus has no app
    // identity yet; see platforms/linux/README.md.
    void applyPerAppDefault(const std::string &program);

    bool enabled() const { return effectiveEnabled_; }
    Settings &settings() { return settings_; }
    const Settings &settings() const { return settings_; }

private:
    // Commit the composing word and end the composition. `consumed` is the caller's
    // to decide: a word boundary swallows its key, a shortcut does not.
    ComposePlan commitBuffer(bool consumed);
    // A word boundary arrived while composing: commit the finished word plus the
    // boundary character itself as one string.
    ComposePlan onBoundary(char32_t scalar, KeySource source);

    // --- non-preedit (nonpreedit.cpp) -----------------------------------------

    // One engine result as a document repair.
    ComposePlan planFromResult(const FunputResult &result);
    // End the composition without committing: in non-preedit the word is already
    // in the document, so only the engine state is dropped.
    ComposePlan endComposition(bool consumed);

    Handle handle_;
    Settings settings_;
    // The VI/EN state actually in effect. Starts from `settings_.enabled` but is
    // driven per-app on focus; a manual toggle overrides it until the next focus
    // change.
    bool effectiveEnabled_ = true;
    // Likewise the mode actually in effect, seeded from `settings_.nonPreedit`.
    bool nonPreedit_ = false;
};

} // namespace funput

#endif // FUNPUT_COMPOSE_COMPOSER_H
