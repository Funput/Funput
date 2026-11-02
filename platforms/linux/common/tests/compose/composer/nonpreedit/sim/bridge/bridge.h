// The gnome-shell bridge as one simulated client: gnome-shell, mutter, GTK3's
// imwayland and the app behind it (RFC #640 §4.1).
//
// Every hop keeps a copy of the surrounding text, and a delete is converted on those
// copies rather than delivered (§4.3). The copy is one snapshot here, since the hops
// receive it from each other in order: null until the client's first answer and
// after a click, as gnome-shell's `_surroundingText` is. A delete with no copy is
// dropped (`_onDeleteSurroundingText`). With one, mutter walks back `deleteChars`
// characters on it to get a byte count, and fewer characters than that before the
// caret is dropped too (`g_return_if_fail (before >= start)`). GTK turns the bytes
// back into characters on the same snapshot — which is why §4.5's byte corruption
// cannot happen — and the app deletes that many from the real document. The commit
// always lands. Dropped means dropped: nothing throws, nobody is told.
//
// Answers reach the shell as in ClientSim, `lag` keystrokes late, with one more fact
// from the IBus daemon in between: a snapshot equal to the last one forwarded is not
// forwarded again (`bus_engine_proxy_set_surrounding_text`, README Linux). So after a
// click the shell hears nothing until the document changes. The daemon's own seed is
// the empty string; this starts unseeded, as a field focused after one that held text.

#ifndef FUNPUT_TESTS_NONPREEDIT_SIM_BRIDGE_BRIDGE_H
#define FUNPUT_TESTS_NONPREEDIT_SIM_BRIDGE_BRIDGE_H

#include <optional>
#include <string>

#include "compose/composer/nonpreedit/sim/client.h"
#include "compose/transport.h"

namespace funput::test {

class BridgeSim : public ClientSim {
public:
    // `Transport::Direct` keeps the guard inert while the bridge still drops deletes:
    // the model of the bug without the fix.
    explicit BridgeSim(int lag, Transport transport = Transport::GnomeShell,
                       Method method = Method::Telex)
        : ClientSim(lag, method), lag_(lag) {
        composer().onFocusChanged(transport == Transport::GnomeShell ? identifyProgram("gnome-shell")
                                                                     : ClientIdentity{});
    }

    // A click into the focused field, between words. mutter resets its input focus;
    // gnome-shell's `vfunc_reset` forwards the reset — the shell flushes, as
    // `FunputEngine::reset()` does, and counts answers afresh — and nulls its copy.
    // The answers to the keys before the click have landed by then.
    void click() {
        perform(composer().flush(), KeyEvent{});
        heard_ = presses_;
        forwarded_ = document_;
        cache_.reset();
        spokeSinceReset_ = false;
    }

    // What the shell knows since the last focus or click.
    bool spokeSinceReset() const { return spokeSinceReset_; }

protected:
    // FunputEngine::applyNonPreeditMode() over this transport.
    bool wantsNonPreedit(const std::string &before) override {
        return !composer().bridgeBlocksRepairs(spokeSinceReset_, before);
    }

    void perform(const ComposePlan &plan, const KeyEvent &ev) override {
        if (plan.effect != Effect::Replace) return ClientSim::perform(plan, ev);
        ComposePlan delivered = plan;
        delivered.deleteChars = acrossBridge(plan.deleteChars);
        ClientSim::perform(delivered, ev);
    }

    std::string answer(bool &answered) override {
        ++presses_;
        const std::string snapshot = ClientSim::answer(answered);
        if (!answered) return snapshot;
        // Which press this snapshot followed: the daemon forwards only what is newer
        // than the last thing it forwarded, and only when it differs.
        const size_t at = presses_ > static_cast<size_t>(lag_) ? presses_ - 1 - lag_ : 0;
        if ((forwarded_ && at <= heard_) || snapshot == forwarded_) {
            answered = false;
            return *forwarded_; // the shell still holds the last answer it heard
        }
        heard_ = at;
        forwarded_ = cache_ = snapshot;
        spokeSinceReset_ = true;
        return snapshot;
    }

private:
    // `deleteChars` as the app receives it: `chars` when the copy carries it, else 0.
    uint32_t acrossBridge(uint32_t chars) const {
        if (!cache_) return 0;
        const std::string &copy = *cache_;
        // mutter: characters to bytes, walking back from the caret on its copy.
        size_t before = copy.size();
        for (uint32_t i = 0; i < chars; ++i) {
            if (before == 0) return 0;
            do {
                --before;
            } while (before > 0 && (static_cast<unsigned char>(copy[before]) & 0xC0) == 0x80);
        }
        // GTK: bytes to characters on the same copy.
        uint32_t delivered = 0;
        for (size_t at = before; at < copy.size(); ++at) {
            if ((static_cast<unsigned char>(copy[at]) & 0xC0) != 0x80) ++delivered;
        }
        return delivered;
    }

    int lag_;
    size_t presses_ = 0;
    // The bridge's copy of the surrounding text; the last snapshot the daemon
    // forwarded, and which press it followed.
    std::optional<std::string> cache_;
    std::optional<std::string> forwarded_;
    size_t heard_ = 0;
    bool spokeSinceReset_ = false;
};

} // namespace funput::test

#endif // FUNPUT_TESTS_NONPREEDIT_SIM_BRIDGE_BRIDGE_H
