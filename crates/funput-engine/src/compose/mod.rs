//! Internal composition pipeline: how one keystroke becomes a platform edit.
//!
//! - `pipeline` orchestrates a keystroke into an [`crate::ImeResult`].
//! - `boundary` handles end-of-word commit and English restore.
//! - `flip` swaps the live word between its Vietnamese and raw-keystroke forms.
//! - `diff` turns a buffer change into a backspace count + inject suffix.
//!
//! Everything here is crate-internal — callers only see the [`crate::Engine`] facade.

pub(crate) mod boundary;
pub(crate) mod diff;
pub(crate) mod flip;
pub(crate) mod pipeline;

pub(crate) use flip::RestoreOverride;
