#include "engine_internal.h"

#include <string>

namespace funput_ibus {

void applySettings(EngineState *state) {
    state->handle.setMethod(static_cast<uint8_t>(state->settings.method));
    state->handle.setToneStyle(static_cast<uint8_t>(state->settings.toneStyle));
    state->effectiveEnabled = state->settings.enabled;
    state->handle.setEnabled(state->effectiveEnabled);
    state->handle.setSmartRestore(state->settings.smartRestore);
    state->handle.setEagerRestore(state->settings.eagerRestore);
    state->handle.setSpellCheck(state->settings.spellCheck);
    state->handle.setAutoCapitalize(state->settings.autoCapitalize);
    state->handle.clearShortcuts();
    for (const auto &[trigger, expansion] : state->settings.shortcuts) {
        state->handle.addShortcut(trigger, expansion);
    }
    state->handle.clear();
}

void updatePreedit(IBusEngine *engine, EngineState *state) {
    const std::string buffer = state->handle.buffer();
    IBusText *text = ibus_text_new_from_string(buffer.c_str());
    const glong length = g_utf8_strlen(buffer.c_str(), -1);
    ibus_engine_update_preedit_text(engine, text, static_cast<guint>(length),
                                    buffer.empty() ? FALSE : TRUE);
}

void commitBuffer(IBusEngine *engine, EngineState *state) {
    const std::string buffer = state->handle.buffer();
    state->handle.clear();
    ibus_engine_hide_preedit_text(engine);
    if (!buffer.empty()) {
        ibus_engine_commit_text(engine, ibus_text_new_from_string(buffer.c_str()));
    }
}

bool handleBoundary(IBusEngine *engine, EngineState *state, char32_t scalar) {
    const std::string before = state->handle.buffer();
    if (before.empty()) {
        if (state->settings.autoCapitalize) {
            state->handle.process(static_cast<uint32_t>(scalar));
        }
        return false;
    }
    const FunputResult result = state->handle.process(static_cast<uint32_t>(scalar));
    std::string word = before;
    if (result.action == ACTION_SEND) {
        word = funput::Handle::output(result);
        if (!word.empty()) word.pop_back();
    }
    funput::appendUtf8(word, static_cast<uint32_t>(scalar));
    state->handle.clear();
    ibus_engine_hide_preedit_text(engine);
    ibus_engine_commit_text(engine, ibus_text_new_from_string(word.c_str()));
    return true;
}

bool matchesToggle(EngineState *state, guint keyval, guint modifiers) {
    if ((modifiers & IBUS_CONTROL_MASK) == 0) return false;
    switch (state->settings.toggleHotkey) {
    case funput::Hotkey::CtrlBacktick: return keyval == IBUS_KEY_grave;
    case funput::Hotkey::CtrlSpace: return keyval == IBUS_KEY_space;
    case funput::Hotkey::AltShift: return false;
    }
    return false;
}

bool matchesFlip(EngineState *state, guint keyval, guint modifiers) {
    const guint required = IBUS_CONTROL_MASK | IBUS_SHIFT_MASK;
    if ((modifiers & required) != required) return false;
    switch (state->settings.flipHotkey) {
    case funput::FlipHotkey::CtrlShiftZ:
        return keyval == IBUS_KEY_z || keyval == IBUS_KEY_Z;
    case funput::FlipHotkey::CtrlShiftX:
        return keyval == IBUS_KEY_x || keyval == IBUS_KEY_X;
    case funput::FlipHotkey::Off: return false;
    }
    return false;
}

void toggleEnabled(IBusEngine *engine, EngineState *state) {
    commitBuffer(engine, state);
    state->effectiveEnabled = !state->effectiveEnabled;
    state->settings.enabled = state->effectiveEnabled;
    state->handle.setEnabled(state->effectiveEnabled);
    state->settings.save();
}

} // namespace funput_ibus
