// The IBus side of a ComposePlan: how a preedit and a commit reach the client.

#include "engine/internal.h"

namespace funput_ibus {

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
    }
}

} // namespace funput_ibus
