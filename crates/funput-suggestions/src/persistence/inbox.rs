//! The edit inbox: how the app changes a store that only the keyboard writes.
//!
//! The app never touches the snapshot or the journal. It writes each batch of edits
//! to a file of its own under `edits/`, complete before it has a name, so the
//! keyboard sees a file whole or not at all. The keyboard reads them in name order,
//! applies them, makes them durable in its own journal, and only then deletes them.
//! A crash in between replays edits that are idempotent, so nothing is lost and
//! nothing counts twice. One file per post means the two processes never write the
//! same file, and no lock is needed between them.

use std::fs;
use std::io::{self, Write};
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicU32, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

use super::JournalEntry;
use super::codec::inbox;
use super::fs::sync_dir;
use super::recovery::Access;
use super::schema::{MAX_INBOX_FILE_BYTES, MAX_INBOX_FILES};
use crate::dictionary::Edit;

const DIRECTORY: &str = "edits";
const EXTENSION: &str = "edit";

/// Edits read from the inbox, and the files they came from.
#[derive(Default)]
pub(crate) struct Pending {
    pub(crate) edits: Vec<JournalEntry>,
    pub(crate) files: Vec<PathBuf>,
}

/// Writes `edits` as one new file in the inbox of the store at `root`.
pub(crate) fn post(root: &Path, edits: &[Edit<'_>]) -> io::Result<()> {
    if edits.is_empty() {
        return Ok(());
    }
    let directory = root.join(DIRECTORY);
    if !directory.exists() {
        fs::create_dir_all(&directory)?;
        sync_dir(root)?;
    }
    let path = directory.join(file_name());
    let temporary = path.with_extension("tmp");
    let mut file = fs::File::create(&temporary)?;
    file.write_all(&inbox::encode(edits))?;
    file.sync_all()?;
    fs::rename(&temporary, &path)?;
    sync_dir(&directory)
}

/// Reads the oldest waiting files, up to [`MAX_INBOX_FILES`]. A damaged file is
/// deleted under [`Access::Repair`] and skipped otherwise; a file from a newer
/// build is always left for that build.
pub(crate) fn read(root: &Path, access: Access) -> io::Result<Pending> {
    let mut names = match fs::read_dir(root.join(DIRECTORY)) {
        Ok(entries) => entries
            .filter_map(Result::ok)
            .map(|entry| entry.path())
            .filter(|path| path.extension().is_some_and(|ext| ext == EXTENSION))
            .collect::<Vec<_>>(),
        Err(error) if error.kind() == io::ErrorKind::NotFound => return Ok(Pending::default()),
        Err(error) => return Err(error),
    };
    names.sort_unstable();
    let mut pending = Pending::default();
    for path in names.into_iter().take(MAX_INBOX_FILES) {
        match read_file(&path) {
            Ok(edits) => {
                pending.edits.extend(edits);
                pending.files.push(path);
            }
            Err(error) if error.kind() == io::ErrorKind::Unsupported => {}
            Err(_) if access == Access::Repair => remove_file(&path)?,
            Err(_) => {}
        }
    }
    Ok(pending)
}

/// Deletes files whose edits are now durable in the journal.
pub(crate) fn remove(root: &Path, files: &[PathBuf]) -> io::Result<()> {
    for path in files {
        remove_file(path)?;
    }
    if files.is_empty() {
        Ok(())
    } else {
        sync_dir(&root.join(DIRECTORY))
    }
}

fn read_file(path: &Path) -> io::Result<Vec<JournalEntry>> {
    if fs::metadata(path)?.len() > MAX_INBOX_FILE_BYTES {
        return Err(crate::binary::invalid_data());
    }
    inbox::decode(&fs::read(path)?)
}

/// Gone already is as good as deleted: the edits it held are what matters.
fn remove_file(path: &Path) -> io::Result<()> {
    match fs::remove_file(path) {
        Err(error) if error.kind() != io::ErrorKind::NotFound => Err(error),
        _ => Ok(()),
    }
}

/// Sorts in posting order: the clock first, then the process and a counter, so
/// two posts in the same nanosecond still get distinct names.
fn file_name() -> String {
    static COUNTER: AtomicU32 = AtomicU32::new(0);
    let nanos = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_or(0, |elapsed| elapsed.as_nanos());
    let count = COUNTER.fetch_add(1, Ordering::Relaxed);
    format!(
        "{nanos:024}-{:010}-{count:010}.{EXTENSION}",
        std::process::id()
    )
}
