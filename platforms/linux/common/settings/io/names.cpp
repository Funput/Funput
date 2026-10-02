#include "settings/io/names.h"

namespace funput::names {

Method parseMethod(const std::string &value) {
    if (value == "telex") return Method::Telex;
    if (value == "telex_advanced") return Method::TelexAdvanced;
    return Method::Vni;
}

const char *methodString(Method method) {
    switch (method) {
    case Method::Telex: return "telex";
    case Method::TelexAdvanced: return "telex_advanced";
    case Method::Vni: return "vni";
    }
    return "vni";
}

ToneStyle parseTone(const std::string &value) {
    return value == "modern" ? ToneStyle::Modern : ToneStyle::Traditional;
}

const char *toneString(ToneStyle tone) {
    return tone == ToneStyle::Modern ? "modern" : "traditional";
}

Hotkey parseHotkey(const std::string &value) {
    if (value == "ctrl_space") return Hotkey::CtrlSpace;
    if (value == "alt_shift") return Hotkey::AltShift;
    if (value == "super_space") return Hotkey::SuperSpace;
    if (value == "ctrl_shift_space") return Hotkey::CtrlShiftSpace;
    return Hotkey::CtrlBacktick;
}

const char *hotkeyString(Hotkey hotkey) {
    switch (hotkey) {
    case Hotkey::CtrlSpace: return "ctrl_space";
    case Hotkey::AltShift: return "alt_shift";
    case Hotkey::SuperSpace: return "super_space";
    case Hotkey::CtrlShiftSpace: return "ctrl_shift_space";
    case Hotkey::CtrlBacktick: return "ctrl_backtick";
    }
    return "ctrl_backtick";
}

FlipHotkey parseFlip(const std::string &value) {
    if (value == "ctrl_shift_z") return FlipHotkey::CtrlShiftZ;
    if (value == "ctrl_shift_x") return FlipHotkey::CtrlShiftX;
    return FlipHotkey::Off;
}

const char *flipString(FlipHotkey hotkey) {
    if (hotkey == FlipHotkey::CtrlShiftZ) return "ctrl_shift_z";
    if (hotkey == FlipHotkey::CtrlShiftX) return "ctrl_shift_x";
    return "off";
}

} // namespace funput::names
