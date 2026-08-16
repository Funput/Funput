// The IBus side of a ComposePlan: how a preedit and a commit reach the client, and
// how the document in front of the caret is read back.

#include "engine/internal.h"

#include <vector>

#include "ffi/utf8.h"

namespace funput_ibus {

std::string textBeforeCaret(IBusEngine *engine) {
    // Nothing has arrived yet, so whatever the base class would hand back is stale or
    // empty — either way not something to delete against.
    const EngineState *state = stateOf(engine);
    if (!state->sawSurroundingText) return {};
    // The cursor is a count of characters — measured, not assumed: the client reported
    // 4 for `phủ ` (five bytes) and 3 for `phủ` (four). Slicing by it as if it were a
    // byte offset would cut `ế` in half.
    const std::vector<uint32_t> chars = funput::decodeUtf8(state->surroundingText);
    std::string out;
    for (size_t i = 0; i < chars.size() && i < state->surroundingCursor; ++i) {
        funput::appendUtf8(out, chars[i]);
    }
    return out;
}

bool hasSelection(IBusEngine *engine) {
    const EngineState *state = stateOf(engine);
    return state->sawSurroundingText && state->surroundingCursor != state->surroundingAnchor;
}

void updatePreedit(IBusEngine *engine, const std::string &text) {
    IBusText *ibusText = ibus_text_new_from_string(text.c_str());
    const glong length = g_utf8_strlen(text.c_str(), -1);
    // IBUS_ENGINE_PREEDIT_COMMIT, not the default CLEAR: this is what makes IBus
    // flush a half-typed word into the client when focus moves away, instead of
    // throwing it out. bus_input_context_{focus_out,disable,unset_engine} and
    // _ic_reset all clear the preedit *before* calling into the engine, and only
    // commit it first when the mode is COMMIT — so with CLEAR the word is already
    // gone by the time focusOut()/reset() run here, and a commit from those
    // handlers has nowhere left to go (unset_engine even disconnects our
    // commit-text handler first). With COMMIT set, IBus owns the flush and our
    // focus/reset handlers only drop state; see engine/callbacks.cpp.
    ibus_engine_update_preedit_text_with_mode(engine, ibusText, static_cast<guint>(length),
                                              text.empty() ? FALSE : TRUE,
                                              IBUS_ENGINE_PREEDIT_COMMIT);
}

void applyPlan(IBusEngine *engine, const funput::ComposePlan &plan) {
    switch (plan.effect) {
    case funput::Effect::None:
        break;
    case funput::Effect::Preedit:
        updatePreedit(engine, plan.text);
        break;
    case funput::Effect::Commit:
        // Always drop the preedit first; an empty text then means the composition
        // ended without producing anything.
        ibus_engine_hide_preedit_text(engine);
        if (!plan.text.empty()) {
            ibus_engine_commit_text(engine, ibus_text_new_from_string(plan.text.c_str()));
        }
        break;
    case funput::Effect::Replace:
        // Non-preedit's document repair: take back what the last keystroke wrote, then
        // write what this one produced. Both counts are characters —
        // `ibus_engine_delete_surrounding_text` takes `nchars`, and the cursor this
        // shell reads back was measured to be a character count too, so no conversion
        // sits between them.
        if (plan.deleteChars > 0) {
            // Same reasoning as the Fcitx5 shell's arm in fcitx5/src/funput_client.cpp:
            // deleting under a live selection eats the user's highlighted text, and
            // abandoning only the delete would double the word or desynchronise the
            // engine from the document. So the composition goes too.
            if (hasSelection(engine)) {
                stateOf(engine)->composer.discard();
                break;
            }
            ibus_engine_delete_surrounding_text(engine, -static_cast<gint>(plan.deleteChars),
                                                plan.deleteChars);
        }
        if (!plan.text.empty()) {
            ibus_engine_commit_text(engine, ibus_text_new_from_string(plan.text.c_str()));
        }
        break;
    }
}

} // namespace funput_ibus
