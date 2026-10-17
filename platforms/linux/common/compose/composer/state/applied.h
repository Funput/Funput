// What the composer last pushed into the engine, so a later push can tell whether it
// would disturb a word that is half-typed.
//
// Why it matters: in non-preedit mode every keystroke is committed to the document,
// and the Settings app's gõ tắt page saves settings.json on every change to a field.
// Typing in one of its fields therefore rewrote the file once per key, the shells'
// file watcher reloaded it, and `applySettings()` dropped the composing word each
// time — the next key found an empty engine and typed raw. The preedit never showed
// it, because nothing reaches the field until the whole word is committed.
//
// Nothing here is a client verdict: the client is behaving. The fault was Funput
// interrupting itself, so the answer is not to stand the mode down but to leave the
// word alone when nothing that shapes it has changed.

#ifndef FUNPUT_COMPOSE_COMPOSER_STATE_APPLIED_H
#define FUNPUT_COMPOSE_COMPOSER_STATE_APPLIED_H

#include <optional>

#include "settings/settings.h"

namespace funput {

class AppliedSettings {
public:
    // Whether `now` composes a word the way the last push did. False before any push:
    // the first one has to configure the engine in full.
    bool keepsComposition(const Settings &now) const {
        return last_.has_value() && last_->sameTyping(now);
    }

    void remember(const Settings &now) { last_ = now; }

    // A manual VI/EN toggle changes `enabled` without a push. Recording it here keeps
    // the reload caused by the toggle's own save from looking like a change.
    void rememberEnabled(bool enabled) {
        if (last_.has_value()) last_->enabled = enabled;
    }

private:
    std::optional<Settings> last_;
};

} // namespace funput

#endif // FUNPUT_COMPOSE_COMPOSER_STATE_APPLIED_H
