//! What a personal-lexicon file on disk is allowed to look like: its magic bytes,
//! its schema version, and the size past which a file is treated as damaged rather
//! than read into memory.

pub(super) const SNAPSHOT_MAGIC: &[u8; 8] = b"FPSNAP01";
pub(super) const JOURNAL_MAGIC: &[u8; 4] = b"FPJR";
pub(super) const VERSION: u16 = 1;
pub(super) const MAX_SNAPSHOT_BYTES: u64 = 2 * 1024 * 1024;
pub(super) const MAX_JOURNAL_BYTES: u64 = 1024 * 1024;
