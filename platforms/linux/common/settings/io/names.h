// The wire names settings.json spells each enum with — the same strings the
// Settings app writes through its serde attributes (settings-gtk/src/settings/
// types.rs). Renaming one means changing both sides. An unknown name parses to the
// option's default rather than failing the file, so a value written by a newer
// build cannot cost the user the rest.

#ifndef FUNPUT_SETTINGS_IO_NAMES_H
#define FUNPUT_SETTINGS_IO_NAMES_H

#include <string>

#include "settings/settings.h"

namespace funput::names {

Method parseMethod(const std::string &value);
const char *methodString(Method method);

ToneStyle parseTone(const std::string &value);
const char *toneString(ToneStyle tone);

Hotkey parseHotkey(const std::string &value);
const char *hotkeyString(Hotkey hotkey);

FlipHotkey parseFlip(const std::string &value);
const char *flipString(FlipHotkey hotkey);

} // namespace funput::names

#endif // FUNPUT_SETTINGS_IO_NAMES_H
