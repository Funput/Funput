// Alt+Shift is a modifier-only chord: matching it on keydown would steal
// Alt+Shift+Tab. Both shells feed every press and release here; the watcher
// fires on the first modifier release if nothing else was pressed, the same
// rule as the Windows `hotkey::Watcher`.

#ifndef FUNPUT_COMPOSE_KEY_CHORD_H
#define FUNPUT_COMPOSE_KEY_CHORD_H

#include "compose/key/classify.h"
#include "compose/key/event.h"

namespace funput {
namespace keysym {
inline constexpr uint32_t ShiftL = 0xFFE1;
inline constexpr uint32_t ShiftR = 0xFFE2;
inline constexpr uint32_t CtrlL = 0xFFE3;
inline constexpr uint32_t CtrlR = 0xFFE4;
inline constexpr uint32_t AltL = 0xFFE9;
inline constexpr uint32_t AltR = 0xFFEA;
inline constexpr uint32_t SuperL = 0xFFEB;
inline constexpr uint32_t SuperR = 0xFFEC;
} // namespace keysym

inline Mods bitFor(uint32_t sym) {
    Mods bit;
    if (sym == keysym::ShiftL || sym == keysym::ShiftR) bit.shift = true;
    else if (sym == keysym::CtrlL || sym == keysym::CtrlR) bit.ctrl = true;
    else if (sym == keysym::AltL || sym == keysym::AltR) bit.alt = true;
    else if (sym == keysym::SuperL || sym == keysym::SuperR) bit.super = true;
    return bit;
}

inline bool anyMod(Mods m) { return m.ctrl || m.alt || m.super || m.shift; }

inline bool isAltShift(Mods m) { return m.alt && m.shift && !m.ctrl && !m.super; }

inline Mods withBit(Mods m, Mods bit) {
    m.ctrl = m.ctrl || bit.ctrl;
    m.alt = m.alt || bit.alt;
    m.super = m.super || bit.super;
    m.shift = m.shift || bit.shift;
    return m;
}

inline Mods withoutBit(Mods m, Mods bit) {
    m.ctrl = m.ctrl && !bit.ctrl;
    m.alt = m.alt && !bit.alt;
    m.super = m.super && !bit.super;
    m.shift = m.shift && !bit.shift;
    return m;
}

class ToggleChord {
public:
    void reset() { *this = ToggleChord{}; }

    // `released` is true for key-up. Returns whether this event completes the
    // configured toggle and should run `Composer::toggleEnabled()`.
    bool feed(const KeyEvent &ev, bool released, Hotkey preset) {
        if (preset != Hotkey::AltShift) {
            return !released && matchesToggle(ev, preset);
        }
        return feedAltShift(ev, released);
    }

private:
    Mods down_{};
    Mods peak_{};
    bool spent_ = false;
    bool interrupted_ = false;

    bool feedAltShift(const KeyEvent &ev, bool released) {
        const Mods bit = bitFor(ev.keysym);
        if (!anyMod(bit)) {
            if (!released) interrupted_ = true;
            return false;
        }
        if (!released) {
            if (spent_ && !interrupted_) {
                spent_ = false;
                peak_ = down_;
            } else if (!anyMod(down_)) {
                *this = ToggleChord{};
            }
            down_ = withBit(down_, bit);
            // Event mods catch a modifier that was already held (Ctrl+Alt+Shift)
            // whose own key-down we never saw.
            peak_ = withBit(withBit(peak_, bit), ev.mods);
            return false;
        }
        down_ = withoutBit(down_, bit);
        const bool fire = !spent_ && !interrupted_ && isAltShift(peak_);
        spent_ = true;
        if (!anyMod(down_)) *this = ToggleChord{};
        return fire;
    }
};

} // namespace funput

#endif // FUNPUT_COMPOSE_KEY_CHORD_H
