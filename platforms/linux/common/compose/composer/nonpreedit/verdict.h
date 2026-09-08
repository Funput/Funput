// What one reading of the document is worth.
//
// The comparison `NonPreeditState::observe()` makes is spelled out in nonpreedit.h.
// This is the enum it produces plus the one failure that comparison cannot see.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_VERDICT_H
#define FUNPUT_COMPOSE_NONPREEDIT_VERDICT_H

#include <cstddef>
#include <cstdint>
#include <string>

namespace funput {

// What the last repair turned out to be worth.
enum class Verdict : uint8_t {
    // Either it landed or the client has not said. Both mean carry on.
    Unknown,
    // The delete was dropped on a repair that followed a re-opened word. Chrome's
    // address bar does exactly this: ordinary repairs work, but one issued straight
    // after the app handled a Backspace itself is discarded. Only re-toning need go.
    RefuseRetone,
    // The delete was dropped on an ordinary repair, or the client never shows the
    // document at all. Nothing written here can be trusted; the mode goes.
    RefuseMode,
};

// A document that stays empty while repairs are being written into it.
//
// The three-string comparison can only convict when "the delete was dropped" and "the
// delete landed" are different strings. With nothing in front of the caret they never
// are — dropping N characters from an empty document leaves it empty — so every
// reading is a non-verdict and the mode writes on blind forever. That is not a corner
// case: it is how Cursor's editor answers. It reports surrounding text, which is what
// arms the mode, and then reports it empty on every keystroke, which is what kept the
// mode armed there while every single delete was dropped.
//
// So a document that will not grow is a verdict of its own — but only when the client
// is *answering*. This counts answers, never silence, and that distinction is the whole
// safety of the rule: an unanswered write reads as an empty document too, and 39% of
// commits go unanswered. A first version of this counted characters written regardless
// and stood the mode down in Chrome, the client the feature exists for. Silence is
// "no verdict"; an answer of nothing is a verdict.
class BlindWrites {
public:
    // `answered` is whether the client sent surrounding text since the last keystroke.
    // Returns true once it has answered with nothing often enough to mean it.
    bool observe(const std::string &document, bool answered) {
        if (!document.empty()) {
            count_ = 0;
            return false;
        }
        if (!answered) return false;
        return ++count_ >= kLimit;
    }

    void reset() { count_ = 0; }

private:
    // Two answers of nothing, which lands before the first repair that deletes
    // anything — the only kind that can corrupt. One could be a race with a commit
    // still in flight; two in a row is the client describing itself.
    static constexpr size_t kLimit = 2;

    size_t count_ = 0;
};

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_VERDICT_H
