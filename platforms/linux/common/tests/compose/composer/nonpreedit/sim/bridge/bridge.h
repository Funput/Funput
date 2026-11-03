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
// back into characters on the same snapshot, and the app deletes that many from the
// real document. The commit always lands. Dropped means dropped: nothing throws,
// nobody is told.
//
// Answers reach the shell as in ClientSim, `lag` keystrokes late. What sits between
// gnome-shell and the shell is the one choice (`Shell`): Fcitx5's IBus frontend hears
// every answer, since libibus compares `IBus.Text` objects and gnome-shell allocates
// a new one per answer (ibusinputcontext.c `text != priv->surrounding_text`, js/misc/
// inputMethod.js `IBus.Text.new_from_string`); the IBus engine sits behind
// ibus-daemon, whose `bus_engine_proxy_set_surrounding_text` (engineproxy.c) forwards
// an answer only when its text, cursor or anchor differ from the last one forwarded.
// The daemon's own seed is the empty string; this starts unseeded, as a field focused
// after one that held text.

#ifndef FUNPUT_TESTS_NONPREEDIT_SIM_BRIDGE_BRIDGE_H
#define FUNPUT_TESTS_NONPREEDIT_SIM_BRIDGE_BRIDGE_H

#include <optional>
#include <string>
#include <utility>

#include "compose/composer/nonpreedit/sim/client.h"
#include "compose/transport.h"

namespace funput::test {

enum class Shell { Fcitx5, IBus };

struct BridgeOptions {
    // `Transport::Direct` keeps the guard inert while the bridge still drops deletes:
    // the model of the bug without the fix.
    Transport transport = Transport::GnomeShell;
    Method method = Method::Telex;
    Shell shell = Shell::Fcitx5;
};

class BridgeSim : public ClientSim {
public:
    explicit BridgeSim(int lag, BridgeOptions options = {})
        : ClientSim(lag, options.method), lag_(lag), dedupe_(options.shell == Shell::IBus) {
        composer().onFocusChanged(options.transport == Transport::GnomeShell
                                      ? identifyProgram("gnome-shell")
                                      : ClientIdentity{});
    }

    // A click into the focused field, between words. GTK answers the click itself
    // with the text as it stands (`gtk_im_context_wayland_reset`), and that answer and
    // everything in flight land before the reset does. mutter then commits the
    // preedit (`clutter_input_focus_reset`, COMMIT mode) and forwards the reset, which
    // gnome-shell's `vfunc_reset` passes to the shell — `Composer::onReset()` — and
    // nulls its copy. A commit is a change to the document, answered `lag` keystrokes
    // late like any other; a click that changed nothing is answered by nobody.
    void click() {
        const bool changed = !preedit_.empty();
        forwarded_ = document_;
        document_ += std::exchange(preedit_, {});
        perform(composer().onReset(), KeyEvent{});
        ClientSim::click(document_, /*reset=*/false); // the reset went through onReset()
        const size_t at = ++events_;
        heard_ = changed ? at - 1 : at;
        cache_.reset();
        spokeSinceReset_ = false;
    }

    // What the shell knows since the last focus or click.
    bool spokeSinceReset() const { return spokeSinceReset_; }

protected:
    // FunputEngine::applyNonPreeditMode() over this transport.
    bool wantsNonPreedit(const std::string &before) override {
        return composer().transport() == Transport::Direct ||
               !composer().bridgeBlocksRepairs(spokeSinceReset_, before);
    }

    void perform(const ComposePlan &plan, const KeyEvent &ev) override {
        // mutter's `priv->preedit`: what a click would commit.
        if (plan.effect == Effect::Preedit) preedit_ = plan.text;
        if (plan.effect == Effect::Commit) preedit_.clear();
        if (plan.effect != Effect::Replace) return ClientSim::perform(plan, ev);
        ComposePlan delivered = plan;
        delivered.deleteChars = acrossBridge(plan.deleteChars);
        ClientSim::perform(delivered, ev);
    }

    std::string answer(bool &answered) override {
        // Which event this press's snapshot followed, in the history ClientSim keeps:
        // the seed is event 0, and this press will be event `events_ + 1`.
        const size_t at = events_ >= static_cast<size_t>(lag_) ? events_ - lag_ : 0;
        ++events_;
        const std::string snapshot = ClientSim::answer(answered);
        if (!answered) return snapshot;
        // Nothing older than what the shell already heard, and, behind ibus-daemon,
        // nothing equal to it.
        if ((forwarded_ && at <= heard_) || (dedupe_ && snapshot == forwarded_)) {
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
    bool dedupe_;
    // Presses and clicks so far: the index of the newest entry in ClientSim's history.
    size_t events_ = 0;
    std::string preedit_;
    // The bridge's copy of the surrounding text; the last snapshot the shell heard,
    // and which event it followed.
    std::optional<std::string> cache_;
    std::optional<std::string> forwarded_;
    size_t heard_ = 0;
    bool spokeSinceReset_ = false;
};

} // namespace funput::test

#endif // FUNPUT_TESTS_NONPREEDIT_SIM_BRIDGE_BRIDGE_H
