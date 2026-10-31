//! Contexts longer than one word, learned from the person typing.
//!
//! The one-word context lives inside each word's record (`bigram/`). Longer ones
//! cannot: a pair of words is a key of its own, and there are far more pairs than
//! words. So each higher order is an [`NgramTable`], fixed in size and filled the
//! same way a word's followers are.
//!
//! - `key` — a word of a context, held the way an edge holds its target.
//! - `context` — finding the words of a context in the store, in one pass.
//! - `chain` — the words the journal just wrote, which is what a trigram is
//!   learned from.
//! - `table/` — the fixed-size table itself, and how it follows words around
//!   when one is removed.

mod chain;
mod context;
mod key;
mod table;

pub(crate) use chain::JournalChain;
pub(crate) use key::WordRef;
pub(crate) use table::{Entry, NgramTable, PROBE_WINDOW};

/// The most words of context the engine reads. Two is a trigram; raising it to
/// three is the 4-gram, one more table and one more step down the backoff.
pub(crate) const MAX_CONTEXT: usize = 2;

/// The order of the one higher-order table there is today, as the snapshot
/// labels its block.
pub(crate) const TRIGRAM: u8 = 3;

/// The trigram table: keyed by two words, oldest first.
pub(crate) type TrigramTable = NgramTable<2>;
