// The extra initial consonants a user admits beyond Vietnamese spelling — UniKey's
// "Cho phép phụ âm đầu Z, F, W, J", one switch per letter. Mirrors
// crates/funput-config/src/settings/onsets/mod.rs, which the Settings app and
// Windows persist with, so all of them read and write `extraOnsets` the same way.
//
// Stored spelled out (`"zj"`, `""` for none; see platforms/CONFIG_FORMAT.md). The
// bits inside [ExtraOnsetLetters] are private and never written anywhere: they are
// neither the core's own bits nor the C ABI's `ONSET_*` wire values, which
// ffi/onsets.h maps to letter by letter.

#ifndef FUNPUT_SETTINGS_ONSETS_LETTERS_H
#define FUNPUT_SETTINGS_ONSETS_LETTERS_H

#include <array>
#include <cstdint>
#include <string>
#include <string_view>

namespace funput {

// One consonant Vietnamese spelling lacks but teencode, loanwords and some names
// open a syllable with (`zô`, `fải`, `wá`, `jờ`). Adding one is a new enumerator
// here and in [kAllOnsetLetters]; the build then rejects every `switch` still
// missing it (`-Werror=switch`) — its symbol, and its FFI wire value.
enum class OnsetLetter : uint8_t { Z, F, W, J };

// Every letter, in the order a settings screen lists them — UniKey's "Z, F, W, J".
inline constexpr std::array<OnsetLetter, 4> kAllOnsetLetters = {
    OnsetLetter::Z, OnsetLetter::F, OnsetLetter::W, OnsetLetter::J};

// The letter as typed, lowercase.
char onsetSymbol(OnsetLetter letter);

// The letters a user admits as initial consonants. Empty — the default — admits
// none, which is native Vietnamese spelling.
class ExtraOnsetLetters {
public:
    constexpr ExtraOnsetLetters() = default;

    static ExtraOnsetLetters none() { return {}; }
    // Every letter — what UniKey's single switch turns on.
    static ExtraOnsetLetters all();

    bool contains(OnsetLetter letter) const { return (bits_ & bit(letter)) != 0; }
    bool empty() const { return bits_ == 0; }
    // These letters with `letter` admitted (`on`) or not.
    ExtraOnsetLetters with(OnsetLetter letter, bool on) const;

    // The letters spelled out in [kAllOnsetLetters] order — the stored form.
    std::string id() const;
    // Read a stored value. Any order and case; a letter this build does not know
    // is skipped rather than failing the rest (CONFIG_FORMAT.md's
    // forward-compatibility rule).
    static ExtraOnsetLetters fromId(std::string_view id);

    bool operator==(const ExtraOnsetLetters &other) const { return bits_ == other.bits_; }
    bool operator!=(const ExtraOnsetLetters &other) const { return bits_ != other.bits_; }

private:
    static constexpr uint8_t bit(OnsetLetter letter) {
        return static_cast<uint8_t>(1U << static_cast<uint8_t>(letter));
    }

    uint8_t bits_ = 0;
};

} // namespace funput

#endif // FUNPUT_SETTINGS_ONSETS_LETTERS_H
