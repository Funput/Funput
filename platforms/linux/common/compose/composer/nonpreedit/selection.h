// Characters selected after the caret — a spreadsheet or browser autocomplete
// suffix, not a user highlight of already-typed text.
//
// Google Sheets (and Excel) fill the rest of a matching cell and select that
// suffix: the caret sits at the end of what the user typed, the anchor past the
// suggestion. A user select-all is cursor == 0; a backward highlight is
// anchor < cursor. Neither is a suffix, so both return 0.

#ifndef FUNPUT_COMPOSE_NONPREEDIT_SELECTION_H
#define FUNPUT_COMPOSE_NONPREEDIT_SELECTION_H

#include <cstdint>

namespace funput {

inline uint32_t selectedAfterCaret(uint32_t cursor, uint32_t anchor) {
    return (anchor > cursor && cursor > 0) ? (anchor - cursor) : 0;
}

} // namespace funput

#endif // FUNPUT_COMPOSE_NONPREEDIT_SELECTION_H
