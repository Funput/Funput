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
    handle_.configure(settings_);
    effectiveEnabled_ = settings_.enabled;
    handle_.setEnabled(effectiveEnabled_);
    // The engine holds a runtime mirror of the gõ tắt table: replace it wholesale
    // rather than diffing, since the table is small and the source of truth is the
    // settings file.
    handle_.clearShortcuts();
    for (const auto &[trigger, expansion] : settings_.shortcuts) {
        handle_.addShortcut(trigger, expansion);
    }
    handle_.clear();
}

// --- VI/EN -------------------------------------------------------------------

ComposePlan Composer::toggleEnabled() {
    // Commit before flipping: the word was typed in the mode that is ending.
    ComposePlan plan = commitBuffer(true);
    effectiveEnabled_ = !effectiveEnabled_;
    settings_.enabled = effectiveEnabled_;
    handle_.setEnabled(effectiveEnabled_);
    settings_.save();
    return plan;
}

void Composer::applyPerAppDefault(const std::string &program) {
    const bool enabled = settings_.isExcluded(program) ? false : settings_.enabled;
    if (enabled == effectiveEnabled_) return;
    effectiveEnabled_ = enabled;
    handle_.setEnabled(enabled);
    handle_.clear();
}

} // namespace funput
