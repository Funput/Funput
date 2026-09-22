//! The state machine both readers share.

use super::Rules;

/// The characters that can end a sentence.
const TERMINATORS: [char; 4] = ['.', '!', '?', '…'];

/// Quotes and brackets that may sit between a terminator and the space confirming
/// it. ASCII `"` and `'` double as openers, which costs nothing: an opener never
/// follows a terminator with a sentence in between.
const CLOSERS: [char; 9] = ['"', '\'', ')', ']', '}', '»', '”', '’', '›'];

/// Walks text left to right, tracking whether the next letter opens a sentence.
///
/// Feed every character in order through [`Scanner::push`]. Ask
/// [`Scanner::opens_sentence`] *before* pushing, when the answer decides what to do
/// with that character, or [`Scanner::awaiting_sentence`] at the end, when the
/// answer is about the position the text ran out at.
pub(crate) struct Scanner {
    rules: Rules,
    /// A sentence has begun and its first letter has not arrived yet.
    awaiting: bool,
    /// A terminator has been seen and is waiting for the whitespace that confirms it.
    terminated: bool,
    /// Letters seen since the last character that was not one.
    letters: usize,
    /// The character before that run of letters, which is what tells `v.v.` apart
    /// from the end of a sentence.
    before_letters: Option<char>,
}

impl Scanner {
    pub(crate) fn new(rules: Rules) -> Self {
        Self {
            rules,
            // The start of the text is the start of a sentence.
            awaiting: true,
            terminated: false,
            letters: 0,
            before_letters: None,
        }
    }

    /// Whether `c` is the letter a waiting sentence has been waiting for.
    ///
    /// Only the rewriting walk needs this; a predicate reads the flag at the end.
    #[cfg(feature = "textcase")]
    pub(crate) fn opens_sentence(&self, c: char) -> bool {
        self.awaiting && c.is_alphabetic()
    }

    /// Whether the position after everything pushed so far starts a sentence.
    pub(crate) fn awaiting_sentence(&self) -> bool {
        self.awaiting
    }

    pub(crate) fn push(&mut self, c: char) {
        match c {
            '\n' => {
                self.awaiting = true;
                self.terminated = false;
            }
            '.' => self.terminated = !self.reads_as_abbreviation(),
            c if TERMINATORS.contains(&c) => self.terminated = true,
            // Transparent: `nói "Xin chào." rồi` still ends a sentence at the space.
            c if CLOSERS.contains(&c) => {}
            c if c.is_whitespace() => {
                self.awaiting = self.awaiting || self.terminated;
                self.terminated = false;
            }
            c => {
                // A digit ends the search for a first letter; punctuation does not.
                self.awaiting = self.awaiting && !c.is_alphanumeric();
                self.terminated = false;
            }
        }
        self.track_letters(c);
    }

    /// Whether the `.` about to be pushed closes an abbreviation rather than a
    /// sentence, judged by a `.` behind the run of letters in front of it.
    fn reads_as_abbreviation(&self) -> bool {
        self.rules.guard_abbreviations && self.letters > 0 && self.before_letters == Some('.')
    }

    fn track_letters(&mut self, c: char) {
        if c.is_alphabetic() {
            self.letters += 1;
        } else {
            self.before_letters = Some(c);
            self.letters = 0;
        }
    }
}
