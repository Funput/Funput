// Everything about the composer that is not a keystroke: settings, the runtime
// VI/EN state, and the composition's non-key exits (flush and discard).

#include "compose/composer/composer.h"

#include <utility>

namespace funput {

Composer::Composer(Settings settings) : settings_(std::move(settings)) {
    applySettings();
}

// --- composition exits -------------------------------------------------------

ComposePlan Composer::flush() {
    // `consumed` has no meaning outside a keystroke; the callers ignore it.
    return commitBuffer(false);
}

void Composer::discard() {
    handle_.clear();
}

// Fcitx5 hands a reset on to the engine whatever sent it, and on the gnome-shell
// bridge the sender has already committed the word. There a reset is a click into
// the focused field: mutter's `clutter_input_focus_reset` (clutter-input-focus.c)
// commits `priv->preedit` first when the preedit was published with
// CLUTTER_PREEDIT_RESET_COMMIT — fcitx5's IBus frontend sends that mode for every
// preedit not marked DontCommit (ibusfrontend.cpp `updatePreeditDelegate`, with
// gnome-shell's `set_client_commit_preedit(true)`), and this composer marks none —
// and only then calls `clutter_input_method_reset`, which gnome-shell's `vfunc_reset`
// (js/misc/inputMethod.js) forwards as IBus `Reset` → `resetDBus` → `InputContext::
// reset()` → `FunputEngine::reset()`. Flushing here would type the word a second
// time, and the guard puts the first word of a field into a preedit on exactly this
// transport. Dropping the preedit is still the shell's job, so the plan says so. On
// a direct transport nothing else commits the preedit, as the IBus shell's comment
// on `reset` records for ibus-daemon's own commit, and it is flushed as before.
ComposePlan Composer::onReset() {
    if (nonPreedit_.transport == Transport::Direct) return flush();
    // The click moved the caret: the journal goes, as it does through `flush()`.
    nonPreedit_.forget();
    discard();
    return ComposePlan::flushThrough({});
}

void Composer::armCapitalization() {
    handle_.armCapitalization();
}

// --- settings ----------------------------------------------------------------

bool Composer::reloadSettingsIfChanged() {
    return settings_.reloadIfChanged();
}

bool Composer::reloadSettings() {
    return settings_.reload();
}

void Composer::applySettings() {
    // Only the gõ tắt table or its switches moved: leave the word and the VI/EN state
    // alone. The Settings app saves on every keystroke in its gõ tắt fields, so a full
    // push here dropped the word being typed into them. See state/applied.h.
    //
    // Nothing here touches the non-preedit mode: it is not a setting, and what a shell
    // decided about the client in front of it must survive a reload.
    const bool keepsWord = applied_.keepsComposition(settings_);
    if (!keepsWord) {
        handle_.configure(settings_);
        handle_.setExtraOnsets(settings_.extraOnsets);
        effectiveEnabled_ = settings_.enabled;
        handle_.setEnabled(effectiveEnabled_);
    }
    // The gõ tắt options ride separately from `configure` (see handle.h), and
    // ahead of the table itself only for reading order — none of them clears it.
    handle_.setShortcutsEnabled(settings_.shortcutsEnabled);
    handle_.setShortcutSmartCase(settings_.shortcutSmartCase);
    handle_.setShortcutsInEnglish(settings_.shortcutsInEnglish);
    // The engine holds a runtime mirror of the gõ tắt table: replace it wholesale
    // rather than diffing, since the table is small and the source of truth is the
    // settings file.
    handle_.clearShortcuts();
    for (const auto &[trigger, expansion] : settings_.shortcuts) {
        handle_.addShortcut(trigger, expansion);
    }
    // A change to how words compose cannot finish a half-typed one, so it is dropped.
    if (!keepsWord) handle_.clear();
    applied_.remember(settings_);
}

// --- VI/EN -------------------------------------------------------------------

ComposePlan Composer::toggleEnabled() {
    // Commit before flipping: the word was typed in the mode that is ending.
    ComposePlan plan = commitBuffer(true);
    effectiveEnabled_ = !effectiveEnabled_;
    settings_.enabled = effectiveEnabled_;
    applied_.rememberEnabled(effectiveEnabled_);
    handle_.setEnabled(effectiveEnabled_);
    settings_.save();
    return plan;
}

bool Composer::englishShortcutsActive() const {
    return settings_.shortcutsEnabled && settings_.shortcutsInEnglish &&
           !settings_.shortcuts.empty();
}

} // namespace funput
