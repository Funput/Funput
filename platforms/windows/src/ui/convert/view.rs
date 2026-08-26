//! The window's state, and everything shown from it.
//!
//! [`refresh`] rebuilds the whole window, and every callback ends by calling it.
//! That is deliberate: with three states and two charsets in play, setting properties
//! one at a time is how a window ends up showing a target it is no longer converting
//! to.
//!
//! A charset is an index into [`charset::ALL`] here and in the Slint files, which
//! name no charset at all. Implementing VISCII in core lengthens every menu in this
//! window without a line changing in it.

use std::cell::RefCell;

use funput_convert::{
    Mode,
    charset::{self, Charset},
};
use slint::Weak;

use crate::ConvertWindow;

use super::files;

thread_local! {
    pub(super) static WINDOW: RefCell<Option<Weak<ConvertWindow>>> = const { RefCell::new(None) };
    pub(super) static STATE: RefCell<State> = const { RefCell::new(State::new()) };
}

pub(super) struct State {
    /// Index into [`charset::ALL`]. Unicode by default: converting *to* it is what
    /// nearly everyone opening this window came to do.
    pub(super) target: usize,
    /// Text state. `None` until something is identified or the user picks.
    pub(super) source: Option<usize>,
    pub(super) input: String,
    pub(super) files: Vec<funput_convert::Entry>,
}

impl State {
    pub(super) const fn new() -> Self {
        Self {
            target: 0,
            source: None,
            input: String::new(),
            files: Vec::new(),
        }
    }
}

pub(super) fn current() -> Option<ConvertWindow> {
    WINDOW.with(|cell| cell.borrow().as_ref().and_then(Weak::upgrade))
}

/// The charset an index names, clamped. An index can only come from a menu this
/// window built out of `ALL`, so clamping keeps a future mistake a wrong entry
/// rather than a panic.
pub(super) fn at(index: usize) -> Charset {
    charset::ALL[index.min(charset::ALL.len() - 1)]
}

pub(super) fn index_of(charset: Charset) -> Option<usize> {
    charset::ALL.iter().position(|&c| c == charset)
}

/// Rebuild the whole window from the state.
///
/// Which shape it takes is [`Mode::of`]'s call, shared with the GTK window on Linux —
/// including the rule that one file takes the text shape rather than a one-row table.
pub(super) fn refresh() {
    let Some(window) = current() else { return };
    STATE.with(|s| {
        let mut state = s.borrow_mut();
        let target = at(state.target);
        window.set_target_index(i32::try_from(state.target).unwrap_or(0));
        funput_convert::measure(&mut state.files, target);

        match Mode::of(&state.files, &state.input) {
            Mode::Empty => window.set_mode("empty".into()),
            // Two arms for one shape: a pasted paragraph and a single file look the
            // same on screen but are filled from different places. Split rather than
            // matched on `first()` because the `None` arm needs `state` mutably, and
            // the borrow taken to ask the question outlives the answer.
            Mode::Text if state.files.is_empty() => {
                show_pasted(&window, &mut state, target);
                window.set_mode("text".into());
            }
            Mode::Text => {
                if let Some(only) = state.files.first() {
                    files::show_one(&window, only, target);
                }
                window.set_mode("text".into());
            }
            Mode::Files => {
                files::show_many(&window, &state.files);
                window.set_mode("files".into());
            }
        }
    });
}

/// The pasted-paragraph state: identify, convert, and say what it will cost.
fn show_pasted(window: &ConvertWindow, state: &mut State, target: Charset) {
    window.set_from_file(false);
    // The user's own choice outranks the guess: they are looking at the document and
    // the detector is looking at statistics.
    let source = state
        .source
        .map_or_else(|| charset::detect(&state.input).and_then(index_of), Some);
    state.source = source;

    window.set_source_index(source.and_then(|i| i32::try_from(i).ok()).unwrap_or(-1));
    let Some(from) = source.map(at) else {
        // Nothing identified it and nothing was chosen: show the text back rather
        // than convert it under a guess.
        window.set_output_text(state.input.clone().into());
        window.set_loss(slint::SharedString::new());
        return;
    };
    window.set_output_text(funput_convert::preview(&state.input, from, target).text.into());
    window.set_loss(funput_convert::loss(&state.input, from, target).into());
}
