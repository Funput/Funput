// The words a trace line uses for the composer's enums.
//
// They are the trace's wire format, so a replay tool reads the same names the code
// does. Every switch is exhaustive: -Werror=switch names the case a new enumerator
// adds, which is how a value added to KeyKind or Verdict reaches the trace.

#ifndef FUNPUT_TRACE_NAMES_H
#define FUNPUT_TRACE_NAMES_H

#include <string>

#include "compose/composer/nonpreedit/verdict.h"
#include "compose/key/classify.h"
#include "compose/plan.h"

namespace funput::trace {

inline const char *kindName(KeyKind kind) {
    switch (kind) {
    case KeyKind::Modifier: return "Modifier";
    case KeyKind::Toggle: return "Toggle";
    case KeyKind::Flip: return "Flip";
    case KeyKind::Shortcut: return "Shortcut";
    case KeyKind::Backspace: return "Backspace";
    case KeyKind::NonText: return "NonText";
    case KeyKind::NumpadDigit: return "NumpadDigit";
    case KeyKind::Boundary: return "Boundary";
    case KeyKind::Compose: return "Compose";
    }
    return "?";
}

inline const char *effectName(Effect effect) {
    switch (effect) {
    case Effect::None: return "None";
    case Effect::Preedit: return "Preedit";
    case Effect::Commit: return "Commit";
    case Effect::Replace: return "Replace";
    }
    return "?";
}

inline const char *verdictName(Verdict verdict) {
    switch (verdict) {
    case Verdict::Unknown: return "Unknown";
    case Verdict::RefuseRetone: return "RefuseRetone";
    case Verdict::RefuseMode: return "RefuseMode";
    }
    return "?";
}

// Whether the key's identity is what the user typed. A letter, a digit or a space is
// content and stays out of a summary trace; which non-text key arrived — Backspace,
// an arrow, Ctrl+A — is the behaviour being diagnosed.
inline bool printsCharacter(KeyKind kind) {
    switch (kind) {
    case KeyKind::NumpadDigit:
    case KeyKind::Boundary:
    case KeyKind::Compose:
        return true;
    case KeyKind::Modifier:
    case KeyKind::Toggle:
    case KeyKind::Flip:
    case KeyKind::Shortcut:
    case KeyKind::Backspace:
    case KeyKind::NonText:
        return false;
    }
    return true;
}

// "ctrl+shift", or empty when nothing is held.
inline std::string modsName(const Mods &mods) {
    std::string out;
    const auto add = [&out](const char *name) {
        if (!out.empty()) out += '+';
        out += name;
    };
    if (mods.ctrl) add("ctrl");
    if (mods.alt) add("alt");
    if (mods.super) add("super");
    if (mods.shift) add("shift");
    return out;
}

} // namespace funput::trace

#endif // FUNPUT_TRACE_NAMES_H
