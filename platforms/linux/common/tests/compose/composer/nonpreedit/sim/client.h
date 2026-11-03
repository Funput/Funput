// A simulated client driving the composer the way a shell does.
//
// The honest client in sync.h and the disobedient one in refusal.h each model one
// thing: whether a plan is applied. The symptoms in issue #640 depend on *when* the
// client answers — Firefox's content cache is a keystroke behind — so this one models
// the answer too: on time, `lag` keystrokes late, or never (`kSilent`).
//
// `press()` is fcitx5/src/funput_input.cpp step for step: read the document, decide
// whether this Backspace may re-open a word, run the key, perform the plan, offer the
// word. A change to that sequence in either shell is a change here, or the tests
// drive a pipeline the shells no longer run. `perform()` and `answer()` are the two
// seams a subclass overrides to model a client that drops deletes, or a bridge
// between the shell and the client (the gnome-shell/mutter path of RFC §4.3).

#ifndef FUNPUT_TESTS_NONPREEDIT_SIM_CLIENT_H
#define FUNPUT_TESTS_NONPREEDIT_SIM_CLIENT_H

#include <string>
#include <vector>

#include "ffi/utf8.h"
#include "support.h"

namespace funput::test {

class ClientSim {
public:
    // A client that never answers: every reading is the field as it was before
    // typing began.
    static constexpr int kSilent = -1;

    explicit ClientSim(int lag = 0, Method method = Method::Telex)
        : lag_(lag), composer_(composerFor(method)) {
        composer_.setNonPreedit(true);
        history_.push_back({}); // the empty field, reported at focus
    }
    virtual ~ClientSim() = default;

    Composer &composer() { return composer_; }
    // The document as the client really holds it, caret at the end.
    const std::string &document() const { return document_; }
    // Whether the last key re-opened the word before it.
    bool adopted() const { return adopted_; }
    // What the composer made of the last reading.
    const Observation &observation() const { return seen_; }

    // The user is holding a selection; the key that follows deletes or replaces it.
    bool selectionLive = false;
    // Of it, the characters after the caret: a Sheets/Excel autocomplete suffix.
    uint32_t selectedAfter = 0;

    // Text that was in the field before Funput was watching.
    void seed(const std::string &text) { document_ = history_.front() = text; }

    // The user clicked: the text before the caret is now `before`. Reported like
    // any other change, so a lagging client shows it as late as it shows a key.
    void click(const std::string &before) {
        document_ = before;
        history_.push_back(document_);
    }

    ComposePlan press(const KeyEvent &ev) {
        bool answered = false;
        const std::string before = answer(answered);
        const KeyKind kind = classify(ev, composer_.settings());
        // Between words, re-ask for the mode: this client has sent surrounding
        // text, so the shell asks for it, and the composer's own latch answers.
        if (!composer_.isComposing()) composer_.setNonPreedit(true);
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

    // Type `keys` as plain ASCII presses and return the document.
    std::string type(const std::string &keys) {
        for (char c : keys) press(ascii(c));
        return document_;
    }

    // Returns whether Funput took the key over (performed the deletion itself).
    bool pressBackspace() { return press(bare(keysym::BackSpace)).consumed; }

protected:
    // Apply `plan` to the document, then whatever the app does with a key it was
    // handed: delete one character for Backspace, type the character otherwise.
    // Deletions count characters, as `deleteSurroundingText` does.
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
    // the client, never by the shell asking. A lagging client reports the document
    // as it stood `lag` keystrokes ago, once it has anything that old; until then
    // the shell hands back what it cached, which is the field as focus found it,
    // and that answer counts once, on the first key: it is the one that turned the
    // mode on. A silent client never counts as having answered. IBus's daemon also
    // drops an update identical to the previous one, so a client whose document did
    // not change answers nothing there; no case here depends on that.
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
    // The field before typing began, then the document after each press.
    std::vector<std::string> history_;
    Observation seen_;
    bool adopted_ = false;
};

} // namespace funput::test

#endif // FUNPUT_TESTS_NONPREEDIT_SIM_CLIENT_H
