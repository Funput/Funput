//! Bytes to data and back, and nothing else. No path, no file, no policy: reading
//! a possibly damaged store lives in `super::recovery`, and writing lives in
//! `super`. Keeping the translation pure is what lets a new record type be added
//! here without touching either.

pub(crate) mod binary;
pub(crate) mod journal;
pub(crate) mod snapshot;
