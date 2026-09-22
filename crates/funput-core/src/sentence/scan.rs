//! The state machine every reader shares.

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
/// with that character, or [`Scanner::awaiting_sentence`] at any point, when the
/// answer is about the position reached so far.
///
/// Two callers, two ways in. A reader holding the whole text starts at
/// [`Scanner::new`] and walks it. A keyboard engine sees only keystrokes and never
/// the document, so it starts at [`Scanner::mid_text`] and keeps one scanner alive
/// for the session.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Scanner {
    rules: Rules,
    /// A sentence has begun and its first letter has not arrived yet.
    awaiting: bool,
    /// A terminator has been seen and is waiting for the whitespace that confirms it.
    terminated: bool,
    /// Whether the character just pushed was a letter. Half of what tells `v.v.`
    /// apart from the end of a sentence.
    in_letter_run: bool,
    /// Whether a `.` was the last character that was not a letter. The other half:
    /// a full stop behind a run of letters that itself sits behind a full stop is
    /// an abbreviation.
    run_follows_dot: bool,
}

impl Scanner {
    /// Start at the beginning of a text, which is the beginning of a sentence.
    pub fn new(rules: Rules) -> Self {
        Self::awaiting_or_not(rules, true)
    }

    /// Start somewhere inside a text whose beginning is unknown.
    ///
    /// What a keyboard engine wants. It is handed keystrokes with no idea where the
    /// caret sits, so assuming the start of a document would capitalize the first
    /// word the user types after switching apps — in the middle of a paragraph.
    /// A host that does know, because it just saw a focus event, says so by
    /// replacing the scanner with [`Scanner::new`].
    pub fn mid_text(rules: Rules) -> Self {
        Self::awaiting_or_not(rules, false)
    }

    /// Whether `c` is the letter a waiting sentence has been waiting for.
    ///
    /// Only a walk that rewrites the text needs this; a predicate reads
    /// [`Scanner::awaiting_sentence`] instead.
    pub fn opens_sentence(&self, c: char) -> bool {
        self.awaiting && c.is_alphabetic()
    }

    /// Whether the position after everything pushed so far starts a sentence.
    pub fn awaiting_sentence(&self) -> bool {
        self.awaiting
    }

    /// Take the pending sentence start, so the next letter is not a second one.
    ///
    /// For a caller that acts on [`Scanner::awaiting_sentence`] *before* pushing the
    /// character it acted on, and whose character may not close the sentence by
    /// itself — a `[` expanding to a whole word, say, which as punctuation would
    /// otherwise leave the scan open over the word it just wrote.
    pub fn consume_sentence_start(&mut self) {
        self.awaiting = false;
        self.terminated = false;
    }

    pub fn push(&mut self, c: char) {
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

    fn awaiting_or_not(rules: Rules, awaiting: bool) -> Self {
        Self {
            rules,
            awaiting,
            terminated: false,
            in_letter_run: false,
            run_follows_dot: false,
        }
    }

    /// Whether the `.` about to be pushed closes an abbreviation rather than a
    /// sentence, judged by a `.` behind the run of letters in front of it.
    fn reads_as_abbreviation(&self) -> bool {
        self.rules.guard_abbreviations && self.in_letter_run && self.run_follows_dot
    }

    fn track_letters(&mut self, c: char) {
        if c.is_alphabetic() {
            self.in_letter_run = true;
        } else {
            self.in_letter_run = false;
            self.run_follows_dot = c == '.';
        }
    }
}
