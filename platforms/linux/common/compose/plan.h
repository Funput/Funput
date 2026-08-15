// What the composer asks its shell to do for one keystroke.
//
// The Linux counterpart of `crates/funput-desktop/src/inject.rs`'s `InjectPlan`:
// there a hook shell is told "delete N characters, then type this"; here a preedit
// shell is told "show this preedit" or "commit this". Same idea — the composer
// decides, the shell performs — which is what lets one composer drive both Fcitx5
// and IBus, and now a non-preedit mode as well (Effect::Replace, which is that
// same "delete N, then type this" instruction under a different name).

#ifndef FUNPUT_COMPOSE_PLAN_H
#define FUNPUT_COMPOSE_PLAN_H

#include <cstdint>
#include <string>

namespace funput {

// How the plan's `text` reaches the client. Deliberately an enum rather than a set
// of bools: non-preedit is a case of its own (delete surrounding text, then commit)
// instead of another flag to combine wrongly.
enum class Effect : uint8_t {
    // Leave the client alone — the composition is unchanged.
    None,
    // Show `text` as the underlined preedit.
    Preedit,
    // Drop the preedit, then commit `text`. An empty `text` means "just drop the
    // preedit", which is how a focus change or a cancelled composition ends.
    Commit,
    // Non-preedit mode: delete `deleteChars` characters before the caret, then
    // commit `text`. The word is built in the document itself rather than in a
    // preedit, so each keystroke repairs what the previous one wrote.
    Replace,
};

// One instruction to the shell.
//
// `consumed` is independent of `effect`: a key can change the client *and* still
// have to reach the app. That is the Ctrl+A and Enter case — the half-typed word
// is committed first, then the key passes through untouched.
struct ComposePlan {
    Effect effect = Effect::None;
    std::string text;
    // The shell should swallow the key: Fcitx5 `event.filterAndAccept()`,
    // IBus `return TRUE`.
    bool consumed = false;
    // How much to delete before `text`, in **characters** — never bytes. Only
    // meaningful for Effect::Replace. Characters is what the engine counts
    // (`FunputResult::backspace`, the same number Windows turns into Backspace
    // presses) and what Fcitx5's `deleteSurroundingText` takes, so no conversion
    // sits between them. Getting this wrong is silent on ASCII and corrupts
    // Vietnamese: `ế` is one character and three bytes.
    uint32_t deleteChars = 0;

    // Nothing to do and nothing to swallow — the key passes straight through.
    bool isNoop() const { return effect == Effect::None && !consumed; }

    static ComposePlan passThrough() { return {}; }
    // Swallow the key without touching the client (a hotkey that changed nothing).
    static ComposePlan swallow() { return {Effect::None, {}, true}; }
    static ComposePlan preedit(std::string text) {
        return {Effect::Preedit, std::move(text), true};
    }
    // Commit `text` and swallow the key that triggered it (a word boundary).
    static ComposePlan commit(std::string text) {
        return {Effect::Commit, std::move(text), true};
    }
    // Commit `text` but let the key through (a shortcut, Enter, a caret key).
    static ComposePlan flushThrough(std::string text) {
        return {Effect::Commit, std::move(text), false};
    }
    // Repair the document in place and swallow the key that caused it. The key is
    // always swallowed: the composer has just written what the key would have
    // typed, so letting it through as well would double the character.
    static ComposePlan replace(uint32_t deleteChars, std::string text) {
        return {Effect::Replace, std::move(text), true, deleteChars};
    }
};

} // namespace funput

#endif // FUNPUT_COMPOSE_PLAN_H
