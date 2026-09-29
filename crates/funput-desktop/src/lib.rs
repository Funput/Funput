//! Platform-agnostic logic for desktop "hook + inject" input shells.
//!
//! A global-hook shell (Windows `WH_KEYBOARD_LL`, or any host that intercepts raw
//! key events and types text back) only differs from the others in *how* it reads
//! keys and *how* it injects text. The decisions in between — what a key means and
//! what to emit for a [`funput_engine::ImeResult`] — are pure and live here so they
//! can be unit tested without any OS APIs. This mirrors the model in `funput-term`'s
//! `result_bytes`, but produces a host-neutral plan instead of terminal bytes.
//!
//! Two pure concerns: `key` (what a keystroke means — [`classify`] over a
//! [`KeyEvent`]) and `inject` (what to emit — [`plan_inject`] into an [`InjectPlan`]).
//!
//! `layout` is a third: whether the keyboard layout under the caret is one
//! Vietnamese can be typed on at all, decided from the handle the host hands it.
//!
//! `caret` says what a shell knows about where the caret landed after it moved
//! without typing — see [`Caret`].
//!
//! `retone` adds the one piece of state such a shell needs: a [`CommittedTail`]
//! shadow of the text it has typed, which stands in for the document it cannot read
//! when Backspace should re-open a finished word.

//! `shell` then holds the state those decisions are made against — the engine,
//! the shadow, the settings, and which app is focused — as a plain struct the
//! platform owns and can unit-test.

mod caret;
mod inject;
mod key;
mod layout;
mod retone;
mod shell;

pub use caret::Caret;
pub use funput_engine::{ImeResult, KeySource};
pub use inject::{InjectPlan, plan_inject};
pub use key::{KeyEvent, KeyKind, Mods, classify};
pub use layout::is_foreign_layout;
pub use retone::CommittedTail;
pub use shell::ShellState;
