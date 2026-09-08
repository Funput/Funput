// Which client a non-preedit verdict was about.
//
// `NonPreeditState` stands the mode down when a client is caught dropping a
// `deleteSurroundingText`, and latches that so a shell re-asserting the mode cannot
// revive it. The latch used to be cleared by any `Composer::onFocusChanged()` — one
// verdict, one focus. That is too little memory for the Fcitx5 shell, because Fcitx5
// calls `activate()` on a *capability* change too, not only on a real focus change
// (see funput_engine.cpp's `deactivate()`). Cursor's editor changes capabilities
// constantly — the suggestion and inline-completion widgets among them — so the
// verdict was erased a keystroke or two after it was reached, the mode re-armed, and
// the next word was mangled again. Forever, instead of once.
//
// So the latch belongs to the client, not to the focus. The shell names the client it
// just focused and this decides whether the question is already answered.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_CLIENTS_H
#define FUNPUT_COMPOSE_NONPREEDIT_CLIENTS_H

#include <algorithm>
#include <cstddef>
#include <string>
#include <string_view>
#include <vector>

namespace funput {

class ClientMemory {
public:
    // A client took focus. Returns whether it is already known to drop deletes, which
    // is what keeps a re-activation of the same client from reopening a settled
    // question. An unnamed client is always a fresh question — the IBus shell has no
    // name to give — and it is the shell's job never to pass a name that stands for
    // more than one app (Fcitx5's `clientId()` refuses `gnome-shell` for that reason).
    bool focus(std::string_view id) {
        current_.assign(id);
        return !current_.empty() &&
               std::find(refused_.begin(), refused_.end(), current_) != refused_.end();
    }

    // The client in focus was just caught dropping a delete. Most recent first, so a
    // long session stays bounded without forgetting whoever was last seen.
    void remember() {
        if (current_.empty()) return;
        const auto seen = std::find(refused_.begin(), refused_.end(), current_);
        if (seen != refused_.end()) refused_.erase(seen);
        refused_.insert(refused_.begin(), current_);
        if (refused_.size() > kMax) refused_.resize(kMax);
    }

private:
    // Enough for every app one person keeps open; past that the oldest verdict is
    // dropped and that client is simply judged again, costing it one word.
    static constexpr size_t kMax = 8;

    std::string current_;
    std::vector<std::string> refused_;
};

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_CLIENTS_H
