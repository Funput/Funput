//! Where a word on screen starts, and what sits in front of it.

/// Byte index where the unbroken run of word characters at the end of `text`
/// begins; `text.len()` when `text` ends on a separator.
pub(crate) fn word_start(text: &str) -> usize {
    text.char_indices()
        .rev()
        .find(|(_, c)| is_separator(*c))
        .map_or(0, |(i, c)| i + c.len_utf8())
}

/// The character right before the word at `start` in `text`, for
/// [`funput_engine::Engine::adopt_after`]. `None` at the start of `text`, which
/// says nothing more is known.
pub(crate) fn char_before(text: &str, start: usize) -> Option<char> {
    text[..start].chars().next_back()
}

/// What ends a word on screen, mirroring the engine's own boundary rule. `[` and `]`
/// compose in advanced Telex rather than separate, but they only ever reach the
/// shadow inside a gõ tắt expansion, where separating is the right reading.
pub(super) fn is_separator(c: char) -> bool {
    c.is_whitespace() || c.is_ascii_punctuation()
}
