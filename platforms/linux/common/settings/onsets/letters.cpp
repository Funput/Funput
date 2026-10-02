#include "settings/onsets/letters.h"

namespace funput {

char onsetSymbol(OnsetLetter letter) {
    switch (letter) {
    case OnsetLetter::Z: return 'z';
    case OnsetLetter::F: return 'f';
    case OnsetLetter::W: return 'w';
    case OnsetLetter::J: return 'j';
    }
    return '?';
}

ExtraOnsetLetters ExtraOnsetLetters::all() {
    ExtraOnsetLetters letters;
    for (OnsetLetter letter : kAllOnsetLetters) letters = letters.with(letter, true);
    return letters;
}

ExtraOnsetLetters ExtraOnsetLetters::with(OnsetLetter letter, bool on) const {
    ExtraOnsetLetters next = *this;
    if (on) {
        next.bits_ = static_cast<uint8_t>(next.bits_ | bit(letter));
    } else {
        next.bits_ = static_cast<uint8_t>(next.bits_ & ~bit(letter));
    }
    return next;
}

std::string ExtraOnsetLetters::id() const {
    std::string out;
    for (OnsetLetter letter : kAllOnsetLetters) {
        if (contains(letter)) out.push_back(onsetSymbol(letter));
    }
    return out;
}

ExtraOnsetLetters ExtraOnsetLetters::fromId(std::string_view id) {
    ExtraOnsetLetters letters;
    for (char c : id) {
        // ASCII-only fold: every symbol is ASCII, and a UTF-8 continuation byte
        // must not be mistaken for one by a locale-aware `tolower`.
        const char lower = (c >= 'A' && c <= 'Z') ? static_cast<char>(c - 'A' + 'a') : c;
        for (OnsetLetter letter : kAllOnsetLetters) {
            if (onsetSymbol(letter) == lower) letters = letters.with(letter, true);
        }
    }
    return letters;
}

} // namespace funput
