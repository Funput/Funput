//! The two ends of a command that transforms a document: getting the bytes in, and
//! writing the bytes out.
//!
//! Shared rather than owned by one command, because `funput convert` and
//! `funput case` need the same two things and would otherwise answer the same
//! questions twice — what a byte-order mark means, whether a closed pipe is a
//! failure. Reaching into another command's module for them would say that one
//! command owns the other's plumbing, which is not true of either.

mod sink;
mod source;

pub(crate) use sink::write;
pub(crate) use source::read;
