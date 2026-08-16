// The IBus side of a ComposePlan: how a preedit and a commit reach the client, and
// how the document in front of the caret is read back.

#include "engine/internal.h"

#include <vector>

#include "ffi/utf8.h"

namespace funput_ibus {

std::string textBeforeCaret(IBusEngine *engine) {
    // Nothing has arrived yet, so whatever the base class would hand back is stale or
    // empty — either way not something to delete against.
    if (!stateOf(engine)->sawSurroundingText) return {};
    IBusText *text = nullptr;
    guint cursor = 0;
    ibus_engine_get_surrounding_text(engine, &text, &cursor, nullptr);
    const gchar *raw = text != nullptr ? ibus_text_get_text(text) : nullptr;
    if (raw == nullptr) return {};
    // `cursor` is taken as a count of characters, which is what
    // ibus_engine_get_surrounding_text documents — note the vfunc spells the same
    // argument `cursor_index`, so the two halves of the API disagree in wording. The
    // g_debug line in callbacks.cpp is there to settle it against a real client; a
    // byte offset used as a character count cuts `ế` in half.
    const std::vector<uint32_t> chars = funput::decodeUtf8(raw);
    std::string out;
    for (size_t i = 0; i < chars.size() && i < cursor; ++i) funput::appendUtf8(out, chars[i]);
    return out;
}

bool hasSelection(IBusEngine *engine) {
    if (!stateOf(engine)->sawSurroundingText) return false;
    guint cursor = 0;
    guint anchor = 0;
    ibus_engine_get_surrounding_text(engine, nullptr, &cursor, &anchor);
    return cursor != anchor;
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
        // Non-preedit's document repair. Not reachable: this shell never calls
        // `Composer::setNonPreedit()`. IBus has `ibus_engine_delete_surrounding_text`,
        // so the mode is not closed to it in principle — but what makes the mode safe
        // was measured against Fcitx5 only, and nothing here has been measured yet.
        // Enabling it before that would be guessing with the user's text.
        break;
    }
}

} // namespace funput_ibus
