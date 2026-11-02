//! Split an evaluation text into the runs of words a keyboard sees, with the
//! same boundaries the shells' `AuthoredTokenTracker` draws.
//!
//! Letters (and combining marks) build a word. A space completes it and keeps it
//! as context for the next word; any other character — punctuation, a digit, a
//! line end — completes it and drops the context. So a run is a list of words
//! separated only by spaces, and each run starts with no context.

use std::path::Path;
use std::{fs, io};

use unicode_normalization::UnicodeNormalization;

/// Read `path` and split it into runs. See [`is_comment`] for the lines skipped.
pub(super) fn load(path: &Path) -> io::Result<Vec<Vec<String>>> {
    Ok(runs(&fs::read_to_string(path)?))
}

/// Split `text` into runs of lowercase NFC words. The suggestion engine stores
/// words lowercase and the shells re-case them, so the evaluation compares
/// lowercase too.
///
/// A run of whitespace counts as one space: a stray double space in the text
/// should not cost the next word its context.
pub(super) fn runs(text: &str) -> Vec<Vec<String>> {
    let mut runs = Vec::new();
    for line in text.lines().filter(|line| !is_comment(line)) {
        let mut run = Vec::new();
        let mut word = String::new();
        for ch in line.nfc().flat_map(char::to_lowercase) {
            if is_word_char(ch) {
                word.push(ch);
                continue;
            }
            if !word.is_empty() {
                run.push(std::mem::take(&mut word));
            }
            if !ch.is_whitespace() {
                close(&mut runs, &mut run);
            }
        }
        if !word.is_empty() {
            run.push(word);
        }
        close(&mut runs, &mut run);
    }
    runs
}

/// A line that is `#` alone or starts with `# `. A chat message that opens
/// with a hashtag (`#throwback hôm qua...`) is text, not a comment.
fn is_comment(line: &str) -> bool {
    let line = line.trim();
    line == "#" || line.starts_with("# ")
}

fn close(runs: &mut Vec<Vec<String>>, run: &mut Vec<String>) {
    if !run.is_empty() {
        runs.push(std::mem::take(run));
    }
}

/// What the trackers count as part of a word: letters and combining marks.
fn is_word_char(ch: char) -> bool {
    ch.is_alphabetic() || ('\u{0300}'..='\u{036F}').contains(&ch)
}
