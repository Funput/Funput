//! Internal composition pipeline: how one keystroke becomes a platform edit.
//!
//! - `pipeline` orchestrates a keystroke into an [`crate::ImeResult`].
//! - `boundary` handles end-of-word commit and English restore.
//! - `doubled` keeps the keys a revert swallowed and writes them back for English.
//! - `flip` swaps the live word between its Vietnamese and raw-keystroke forms.
//! - `diff` turns a buffer change into a backspace count + inject suffix.
//! - `sorted_lines` looks words up in the embedded English lists, in place.
//!
//! Everything here is crate-internal — callers only see the [`crate::Engine`] facade.

pub(crate) mod boundary;
pub(crate) mod diff;
pub(crate) mod doubled;
pub(crate) mod flip;
pub(crate) mod pipeline;
pub(crate) mod sorted_lines;

pub(crate) use flip::RestoreOverride;
