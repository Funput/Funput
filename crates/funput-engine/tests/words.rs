//! Word-level behavior: word boundaries and text-expansion shortcuts (gõ tắt).

mod support;

#[path = "words/boundary.rs"]
mod boundary;
#[path = "words/expand_shortcut.rs"]
mod expand_shortcut;
#[path = "words/seal.rs"]
mod seal;
#[path = "words/shortcut.rs"]
mod shortcut;
#[path = "words/shortcut_affixed.rs"]
mod shortcut_affixed;
#[path = "words/shortcut_composed.rs"]
mod shortcut_composed;
#[path = "words/shortcut_glued.rs"]
mod shortcut_glued;
#[path = "words/shortcut_undo.rs"]
mod shortcut_undo;
