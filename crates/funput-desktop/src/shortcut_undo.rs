//! Backspace right after a gõ tắt expansion puts the trigger back.
//!
//! The engine keeps the undo armed from the expansion until the next key, so the
//! only question a hook + inject shell has to answer is whether the expansion is
//! still what sits before the caret. It cannot read the document, so it asks the
//! [`CommittedTail`] shadow, which every caret move, click or focus change clears,
//! together with the engine (and the undo in it).

use funput_engine::{Engine, ImeResult};

use crate::CommittedTail;

/// Take the undo when it applies: the shell then swallows Backspace and injects
/// the result, `vn` + Space + ⌫ showing `vn ` again instead of `Việt Na `.
///
/// `None` lets Backspace delete as usual. When an undo is armed but the shadow no
/// longer ends with the expansion, the shell has lost track of the screen, so the
/// undo is dropped rather than left to rewrite text that may not be there.
pub fn undo_shortcut(engine: &mut Engine, tail: &mut CommittedTail) -> Option<ImeResult> {
    let written = engine.shortcut_undo_written()?;
    if !tail.ends_with(written) {
        engine.dismiss_shortcut_undo();
        return None;
    }
    let written = written.to_owned();
    let result = engine.undo_shortcut()?;
    tail.rewrite_end(&written, &result.output);
    Some(result)
}
