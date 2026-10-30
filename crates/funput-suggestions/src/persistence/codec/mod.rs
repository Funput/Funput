//! Bytes to data and back, and nothing else. No path, no file, no policy: reading
//! a possibly damaged store lives in `super::recovery`, and writing lives in
//! `super`. Keeping the translation pure is what lets a new record type be added
//! here without touching either.
//!
//! - `frame` — the checksummed wrapper the journal and the edit files share.
//! - `record` — one journal or edit-file entry and its tag byte.
//! - `journal` — the append-only frames of learned tokens and edits.
//! - `snapshot` — the whole store in one frame.
//! - `inbox` — one edit file posted by the app.

mod frame;
pub(crate) mod inbox;
pub(crate) mod journal;
pub(crate) mod record;
pub(crate) mod snapshot;
