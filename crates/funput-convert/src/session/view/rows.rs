//! One row per file, and what it will cost.
//!
//! Split out of [`super`] rather than sitting beside it because the two answer
//! different questions: the view is what the window shows *about the document*, and
//! this is what it shows *about each file in a batch*. They also grow for different
//! reasons — a new field on the view, a new column here — and this crate's files run
//! close enough to the line budget that sharing one would make the next addition a
//! refactor instead of an addition.

use funput_core::charset;

use crate::casing;
use crate::index_of;

use super::Session;

/// One file's row in the batch table.
#[non_exhaustive]
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Row {
    pub name: String,
    /// Index into `charset::ALL`, or `None` when nothing explained the file.
    pub charset: Option<usize>,
    /// "N chữ sẽ mất", or empty.
    pub note: String,
}

/// Rows for the window only — but every count above runs over the whole batch, so a
/// capped list stays honest. Rebuilding two thousand of them on every target change
/// is what the window exists to avoid.
pub(super) fn rows(session: &Session, target: charset::Charset) -> Vec<Row> {
    let (first, len) = session.window;
    session
        .files
        .iter()
        .skip(first)
        .take(len)
        .map(|entry| Row {
            name: entry.name(),
            charset: entry.charset.and_then(index_of),
            note: match entry.charset {
                Some(from) => {
                    // Through `casing::render`, so the count is what this file will
                    // actually cost: UPPERCASE makes letters TCVN3 has no room for,
                    // and a note taken before the transform would understate it.
                    let lost = casing::render(&entry.text, from, &session.casing, target)
                        .cost
                        .unrepresentable;
                    if lost > 0 {
                        format!("{lost} chữ sẽ mất")
                    } else {
                        String::new()
                    }
                }
                None => String::new(),
            },
        })
        .collect()
}
