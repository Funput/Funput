//! Typo correction through the public API, the way a platform shell drives it.

mod support;

#[path = "correction/boundary.rs"]
mod boundary;
#[path = "correction/protocol.rs"]
mod protocol;
#[path = "correction/table.rs"]
mod table;
