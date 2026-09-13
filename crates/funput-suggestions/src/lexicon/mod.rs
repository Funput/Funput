//! The English lexicon: a read-only word list shipped beside the personal store,
//! never inside it.
//!
//! - [`format`] — the `en.lex` layout, its section readers, and the validation
//!   every file passes before anything reads it.
//! - `storage` — mapping the file, or reading it when mapping is unavailable.
//! - `encode` — `en.tsv` to `en.lex`, for the build only.
//!
//! See `docs/features/english-lexicon-suggestion.md`.

#[cfg(any(test, feature = "lexicon-build"))]
pub(crate) mod encode;
pub(crate) mod format;
mod storage;

use std::io;
use std::path::Path;

use format::sections::Layout;
use format::validate::validate;
use storage::Bytes;

/// An `en.lex` that passed validation. There is no way to hold one that did not.
pub(crate) struct Lexicon {
    bytes: Bytes,
    layout: Layout,
}

impl Lexicon {
    pub(crate) fn open(path: &Path) -> io::Result<Self> {
        Self::new(Bytes::open(path)?)
    }

    pub(crate) fn from_bytes(bytes: impl Into<Box<[u8]>>) -> io::Result<Self> {
        Self::new(Bytes::Owned(bytes.into()))
    }

    fn new(bytes: Bytes) -> io::Result<Self> {
        let layout = validate(&bytes)?;
        Ok(Self { bytes, layout })
    }

    pub(crate) fn layout(&self) -> &Layout {
        &self.layout
    }

    pub(crate) fn is_mapped(&self) -> bool {
        matches!(self.bytes, Bytes::Mapped(_))
    }
}

/// What `verify` found in a file that passed.
#[cfg(any(test, feature = "lexicon-build"))]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Stats {
    pub words: usize,
    pub heavy_prefixes: usize,
    pub bytes: usize,
}

/// Runs the library's own validation over `bytes`, so a build tool never writes
/// a file the keyboards would refuse.
#[cfg(any(test, feature = "lexicon-build"))]
pub fn verify(bytes: &[u8]) -> io::Result<Stats> {
    let layout = validate(bytes)?;
    Ok(Stats {
        words: layout.word_count,
        heavy_prefixes: layout.heavy_count,
        bytes: bytes.len(),
    })
}
