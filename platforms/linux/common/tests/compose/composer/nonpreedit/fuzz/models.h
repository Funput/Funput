// Random key scripts against every client model the review measured, bounded and
// seeded so they run as tests: the honest models never earn a verdict from the
// journal and never have a repair land outside the word (honest_tests.cpp); the
// broken ones are caught at the rates the review recorded (broken_tests.cpp).
// A script is a string: letters, ` `, `\b` Backspace, `<` Left, `\n` Enter, `^` a
// bare Shift, `#` a click the client reports (with the input-method reset GTK and
// Qt send). The client keeps text after the caret: Left moves one character across
// it, Enter starts a new line.

#ifndef FUNPUT_TESTS_NONPREEDIT_FUZZ_MODELS_H
#define FUNPUT_TESTS_NONPREEDIT_FUZZ_MODELS_H

#include <string>
#include <vector>

#include "compose/composer/nonpreedit/sim/client.h"
#include "compose/key/chord.h"

namespace funput::test {

constexpr uint32_t kFuzzLeft = 0xFF51; // XK_Left, a NonText key

struct Rng {
    unsigned s;
    unsigned next() { return ((s = s * 1103515245u + 12345u) >> 16) & 0x7fff; }
    int below(int n) { return static_cast<int>(next() % static_cast<unsigned>(n)); }
};

inline std::string randomScript(Rng &rng, const std::string &alphabet, int len) {
    std::string keys;
    const int n = static_cast<int>(alphabet.size());
    for (int i = 0; i < len; ++i) keys += alphabet[static_cast<size_t>(rng.below(n))];
    return keys;
}

struct FuzzSim : ClientSim {
    // How the client answers: on time or `lag` late (Honest), that but silent for a
    // run of keys (Flaky: the shell hands back its cache), that but only when the
    // text changed (Dedupe: the IBus daemon), or always `` (AlwaysEmpty: Cursor).
    enum class Answer { Honest, Flaky, Dedupe, AlwaysEmpty };
    // What it does with a repair: applies it, drops every delete, drops only the
    // repair after a re-open (Chrome's address bar), or drops from key `dropFrom`.
    enum class Perform { Applies, DropsDeletes, DropsAfterReopen, DropsFrom };

    Answer answer_ = Answer::Honest;
    Perform perform_ = Perform::Applies;
    int dropFrom = 0;
    int silentFrom = 0, silentFor = 0; // Flaky: silent for keys from silentFrom on
    int keyNo = 0;
    std::string after; // text after the caret
    // What the oracle saw: repairs that deleted across a space or past the document
    // (a word re-opened from a prediction the document does not end with), text
    // deletes the client dropped, the reading the last key was judged on.
    int corrupt = 0;
    int droppedWithText = 0;
    std::string lastReading;

    explicit FuzzSim(int lag) : ClientSim(lag) {}

    void perform(const ComposePlan &plan, const KeyEvent &ev) override {
        ++keyNo;
        if (ev.keysym == kFuzzLeft) {
            std::vector<uint32_t> chars = decodeUtf8(document_);
            if (chars.empty()) return;
            std::string last;
            appendUtf8(last, chars.back());
            document_.resize(document_.size() - last.size());
            after = last + after;
            return;
        }
        if (ev.keysym == keysym::Return) {
            document_ = after = {};
            return;
        }
        if (ev.keysym == keysym::ShiftL) return;
        bool drop = false;
        switch (perform_) {
        case Perform::Applies: break;
        case Perform::DropsDeletes: drop = true; break;
        case Perform::DropsAfterReopen: drop = adopted(); break;
        case Perform::DropsFrom: drop = keyNo >= dropFrom; break;
        }
        if (plan.effect == Effect::Replace) {
            if (perform_ == Perform::Applies && !plan.text.empty()) {
                const std::vector<uint32_t> chars = decodeUtf8(document_);
                if (plan.deleteChars > chars.size()) ++corrupt;
                for (size_t i = 0; i < plan.deleteChars && i < chars.size(); ++i) {
                    if (chars[chars.size() - 1 - i] == U' ') ++corrupt;
                }
            }
            if (drop) {
                if (plan.deleteChars > 0 && !plan.text.empty()) ++droppedWithText;
                document_ += plan.text;
                return;
            }
        }
        ClientSim::perform(plan, ev);
    }

    std::string answer(bool &answered) override {
        std::string reading;
        bool fresh = false;
        switch (answer_) {
        case Answer::Honest:
            reading = ClientSim::answer(answered);
            break;
        case Answer::Flaky: {
            reading = ClientSim::answer(fresh);
            const int key = keyNo + 1;
            answered = fresh && (key < silentFrom || key >= silentFrom + silentFor);
            if (answered) cached_ = reading;
            reading = cached_;
            break;
        }
        case Answer::Dedupe:
            reading = ClientSim::answer(fresh);
            answered = fresh && (first_ || reading != last_);
            first_ = false;
            if (fresh) last_ = reading;
            break;
        case Answer::AlwaysEmpty:
            answered = true;
            break;
        }
        lastReading = reading;
        return reading;
    }

    void play(char c) {
        switch (c) {
        case '\b': pressBackspace(); break;
        case '<': press(bare(kFuzzLeft)); break;
        case '\n': press(control(keysym::Return, U'\r')); break;
        case '^': press(bare(keysym::ShiftL)); break;
        case '#': after.clear(); click(document_ + after); break; // the caret to the end
        default: press(ascii(c)); break;
        }
    }

    std::string full() const { return document_ + "|" + after; }

private:
    std::string cached_, last_;
    bool first_ = true;
};

} // namespace funput::test

#endif // FUNPUT_TESTS_NONPREEDIT_FUZZ_MODELS_H
