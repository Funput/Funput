// Where a word ends: a boundary character typed into it, and every exit that
// commits what is composing. The decision tree that routes keys here: keys.cpp.

#include "compose/composer/composer.h"

#include "ffi/utf8.h"

namespace funput {

ComposePlan Composer::commitBuffer(bool consumed) {
    // Every exit funnels through here — flush(), toggleEnabled(), a shortcut and a
    // caret key — and each may move the caret, so the journal goes first (see
    // NonPreeditState::forget). Non-preedit then has nothing left to commit.
    nonPreedit_.forget();
    if (nonPreedit_.on) return endComposition(consumed);
    std::string buffer = handle_.buffer();
    handle_.clear();
    // An empty buffer still yields Effect::Commit — with no text that means "drop
    // the preedit and commit nothing", which is what ends a cancelled composition.
    return ComposePlan{Effect::Commit, std::move(buffer), consumed};
}

ComposePlan Composer::onBoundary(char32_t scalar, KeySource source) {
    const std::string before = handle_.buffer();
    if (before.empty()) {
        // Nothing composing: the boundary is just a character the app types itself.
        // It still reaches the engine when auto-capitalize is on, which tracks
        // sentence ends. Non-preedit journals a plain space (see `appPerforms`) and
        // nothing else: Tab indents or completes rather than inserting itself, and
        // editors auto-pair `(`, `[` and `"` — the `(` before the caret would confirm
        // a record of it, and the next ⌫ would become Funput's one-character delete,
        // leaving the `)` the app's own Backspace removes with its pair. Unrecorded,
        // such a key only costs `inSync`, which is the safe side.
        if (settings_.autoCapitalize) handle_.process(static_cast<uint32_t>(scalar), source);
        if (scalar != U' ') return ComposePlan::passThrough();
        return appPerforms(0, scalar);
    }
    const FunputResult result = handle_.process(static_cast<uint32_t>(scalar), source);
    // The engine's output already ends with the boundary character, and the word in
    // front of it is already in the document — so the repair covers both at once,
    // with none of the splicing the preedit path needs below.
    if (nonPreedit_.on) return planFromResult(result, scalar);
    std::string word = before;
    if (result.action == ACTION_SEND) {
        word = Handle::output(result);
        // The engine's output ends with the boundary character it was fed; drop it
        // and re-append below, so both paths commit exactly one copy of it. Every
        // boundary is ASCII (see `isBoundary`), so one byte is one character here.
        if (!word.empty()) word.pop_back();
    }
    appendUtf8(word, static_cast<uint32_t>(scalar));
    handle_.clear();
    return ComposePlan::commit(std::move(word));
}

} // namespace funput
