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
