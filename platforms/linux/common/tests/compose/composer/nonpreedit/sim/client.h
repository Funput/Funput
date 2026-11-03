// A simulated client driving the composer the way a shell does.
//
// The honest client in appedits/sync.h and the disobedient one in verdicts/refusal.h
// model whether a plan is applied. The symptoms in issue #640 depend on *when* the
// client answers — Firefox's content cache is a keystroke behind — so this one models
// the answer too: on time, `lag` keystrokes late, or never (`kSilent`).
//
// `press()` is fcitx5/src/funput_input.cpp step for step: focus-in, then per key
// read the document, decide whether this Backspace may re-open a word, run the key,
// perform the plan, offer the word. A change to that sequence in either shell is a
// change here. `perform()`, `answer()` and `wantsNonPreedit()` are the seams a subclass
// overrides: a client that drops deletes, or the gnome-shell bridge and the shell's
// mode decision over it (bridge/bridge.h, RFC §4.3).
//
// One assumption worth naming: a client here re-sends its document on every key,
// changed or not, as Fcitx5's updates and Cursor do (the IBus daemon drops repeats;
// honest_tests.cpp's Dedupe models that). BlindWrites counts answers, so a client a
// few keys late on an empty field reaches its limit after a key that changed nothing
// (verdicts/blind_tests.cpp pins that trade-off).

#ifndef FUNPUT_TESTS_NONPREEDIT_SIM_CLIENT_H
#define FUNPUT_TESTS_NONPREEDIT_SIM_CLIENT_H

#include <string>
#include <vector>

#include "ffi/utf8.h"
#include "support.h"

namespace funput::test {

class ClientSim {
public:
    static constexpr int kSilent = -1; // never answers: every reading is the field at focus

    explicit ClientSim(int lag = 0, Method method = Method::Telex)
        : lag_(lag), composer_(composerFor(method)) {
        composer_.onFocusChanged(); // the shells' focus-in, before the first key
        composer_.setNonPreedit(true);
        history_.push_back({}); // the empty field, reported at focus
    }
    virtual ~ClientSim() = default;

    Composer &composer() { return composer_; }
    const std::string &document() const { return document_; } // caret at its end
    bool adopted() const { return adopted_; } // the last key re-opened a word
    const Observation &observation() const { return seen_; } // of the last reading

    bool selectionLive = false; // the key that follows deletes or replaces a selection
    uint32_t selectedAfter = 0; // of it, after the caret: a Sheets/Excel autocomplete suffix

    void seed(const std::string &text) { document_ = history_.front() = text; } // pre-existing

    // The user clicked: the text before the caret is now `before`. GTK and Qt reset the
    // input method on a click; Fcitx5 answers with `Composer::onReset()`, on this direct
    // transport a `flush()`: the composition ends and the journal is forgotten, as after
    // a caret key; the bridge: bridge/bridge.h. `reset = false`: the silent move match.h names.
    void click(const std::string &before, bool reset = true) {
        if (reset) composer_.onReset();
        document_ = before;
        history_.push_back(document_);
    }

    ComposePlan press(const KeyEvent &ev) {
        bool answered = false;
        const std::string before = answer(answered);
        const KeyKind kind = classify(ev, composer_.settings());
        // Between words the shell re-asks for the mode; the composer's latch answers.
        if (!composer_.isComposing()) composer_.setNonPreedit(wantsNonPreedit(before));
        if (composer_.nonPreedit()) {
            seen_ = composer_.observeDocument(before, selectionLive, answered, selectedAfter);
        }
        const bool reopen = composer_.nonPreedit() && !composer_.isComposing() &&
                            kind == KeyKind::Backspace;
        const ComposePlan plan = composer_.onKey(ev);
        perform(plan, ev);
        adopted_ = reopen && composer_.adoptWordBeforeBackspace();
        history_.push_back(document_);
        return plan;
    }

    std::string type(const std::string &keys) { // plain ASCII presses
        for (char c : keys) press(ascii(c));
        return document_;
    }

    bool pressBackspace() { return press(bare(keysym::BackSpace)).consumed; } // Funput's?

protected:
    // The shell's decision between words, as applyNonPreeditMode() makes it: this
    // client has spoken, so yes — unless the bridge cannot carry a repair (bridge/).
    virtual bool wantsNonPreedit(const std::string & /*before*/) { return true; }

    // Apply `plan` to the document, then whatever the app does with a key it was
    // handed: delete one character for Backspace, type the character otherwise.
    // Deletions count characters, as `deleteSurroundingText` does; the document being
    // the text before the caret only, `deleteAfterChars` (a selected suffix after it,
    // 0 everywhere but appedits/sync_tests.cpp) is popped from the same string.
    virtual void perform(const ComposePlan &plan, const KeyEvent &ev) {
        switch (plan.effect) {
        case Effect::None:
        case Effect::Preedit: // not in the document until it commits
            break;
        case Effect::Commit:
            document_ += plan.text;
            break;
        case Effect::Replace:
            popChars(document_, plan.deleteAfterChars);
            popChars(document_, plan.deleteChars);
            document_ += plan.text;
            break;
        }
        if (plan.consumed) return;
        if (ev.keysym == keysym::BackSpace) {
            popChars(document_, 1);
        } else if (ev.ch != 0) {
            appendUtf8(document_, static_cast<uint32_t>(ev.ch));
        }
    }

    // The surrounding text the client reports before this key, and whether it sent
    // any since the last one — the shells' `surroundingFresh`, set by an update from
    // the client, never by the shell asking. A lagging client reports the document as
    // it stood `lag` keystrokes ago, once it has anything that old; until then the
    // shell hands back its cache, the field as focus found it, which counts as an
    // answer once, on the first key. A silent client never answers.
    virtual std::string answer(bool &answered) {
        if (lag_ == kSilent) {
            answered = false;
            return history_.front();
        }
        const size_t behind = static_cast<size_t>(lag_);
        answered = history_.size() == 1 || history_.size() > behind;
        if (history_.size() <= behind) return history_.front();
        return history_[history_.size() - 1 - behind];
    }

    std::string document_;

private:
    int lag_;
    Composer composer_;
    std::vector<std::string> history_; // the field at focus, then after each press
    Observation seen_;
    bool adopted_ = false;
};

} // namespace funput::test

#endif // FUNPUT_TESTS_NONPREEDIT_SIM_CLIENT_H
