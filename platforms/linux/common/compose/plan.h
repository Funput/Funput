// What the composer asks its shell to do for one keystroke.
//
// The Linux counterpart of `crates/funput-desktop/src/inject.rs`'s `InjectPlan`:
// there a hook shell is told "delete N characters, then type this"; here a preedit
// shell is told "show this preedit" or "commit this". Same idea — the composer
// decides, the shell performs — which is what lets one composer drive both Fcitx5
// and IBus, and what will let it drive a non-preedit mode later.

#ifndef FUNPUT_COMPOSE_PLAN_H
#define FUNPUT_COMPOSE_PLAN_H

#include <cstdint>
#include <string>

namespace funput {

// How the plan's `text` reaches the client. Deliberately an enum rather than a set
// of bools: the coming non-preedit mode adds a case (delete surrounding text, then
// commit) instead of another flag to combine wrongly.
enum class Effect : uint8_t {
    // Leave the client alone — the composition is unchanged.
    None,
    // Show `text` as the underlined preedit.
    Preedit,
    // Drop the preedit, then commit `text`. An empty `text` means "just drop the
    // preedit", which is how a focus change or a cancelled composition ends.
    Commit,
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
};

} // namespace funput

#endif // FUNPUT_COMPOSE_PLAN_H
