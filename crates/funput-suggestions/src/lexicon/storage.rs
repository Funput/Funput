//! Where an open lexicon's bytes live: mapped from the file when the platform
//! allows it, read into memory when it does not.
//!
//! Mapped pages are clean memory — the system loads only the pages a lookup
//! touches and can drop them at any time — which is what keeps a 400 KB word
//! list out of a keyboard extension's footprint.

use std::fs::File;
use std::io::{self, Read};
use std::ops::Deref;
use std::path::Path;

use memmap2::Mmap;

use super::format::MAX_FILE_BYTES;
use crate::binary::invalid_data;

pub(crate) enum Bytes {
    Mapped(Mmap),
    Owned(Box<[u8]>),
}

impl Bytes {
    pub(crate) fn open(path: &Path) -> io::Result<Self> {
        let file = File::open(path)?;
        if file.metadata()?.len() > MAX_FILE_BYTES {
            return Err(invalid_data());
        }
        // SAFETY: a mapping is only as stable as the file under it; truncating
        // the file while mapped makes a later read fault. The shells hand over a
        // file nothing else writes — inside the iOS extension's signed, read-only
        // bundle, or Android's private copy under noBackupFilesDir — and
        // validation reads every byte once before any lookup does.
        match unsafe { Mmap::map(&file) } {
            Ok(map) => Ok(Self::Mapped(map)),
            Err(_) => Self::read(file),
        }
    }

    /// The fallback for a platform or file system that will not map. Reads one
    /// byte past the limit, so a file that grew since `metadata` still fails.
    fn read(file: File) -> io::Result<Self> {
        let mut bytes = Vec::new();
        file.take(MAX_FILE_BYTES + 1).read_to_end(&mut bytes)?;
        Ok(Self::Owned(bytes.into_boxed_slice()))
    }
}

impl Deref for Bytes {
    type Target = [u8];

    fn deref(&self) -> &[u8] {
        match self {
            Self::Mapped(map) => map,
            Self::Owned(bytes) => bytes,
        }
    }
}
