//! Word-level behavior: word boundaries and text-expansion shortcuts (gõ tắt).

mod support;

#[path = "words/boundary.rs"]
mod boundary;
#[path = "words/shortcut.rs"]
mod shortcut;
#[path = "words/shortcut_composed.rs"]
mod shortcut_composed;
#[path = "words/shortcut_undo.rs"]
mod shortcut_undo;
