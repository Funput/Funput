//! The edit inbox: how the app changes a store that only the keyboard writes.
//!
//! The app never touches the snapshot or the journal. It writes each batch of edits
//! to a file of its own under `edits/`, complete before it has a name, so the
//! keyboard sees a file whole or not at all. The keyboard reads them in name order,
//! applies them, makes them durable in its own journal, and only then deletes them.
//! A crash in between replays edits that are idempotent, so nothing is lost and
//! nothing counts twice. One file per post means the two processes never write the
//! same file, and no lock is needed between them.

mod name;

use std::fs;
use std::io::{self, Write};
use std::path::{Path, PathBuf};

use super::codec::inbox::{self, max_entry_bytes};
use super::fs::sync_dir;
use super::recovery::Access;
use super::schema::{MAX_INBOX_FILE_BYTES, MAX_INBOX_FILES};
use crate::dictionary::{Edit, EditKind};
use crate::engine::MAX_TOKEN_SCALARS;

const DIRECTORY: &str = "edits";

/// How many edits one file carries. A larger post becomes several files, in order,
/// so no file can grow past what the reader accepts.
const EDITS_PER_FILE: usize = 256;

const _: () = assert!(
    (14 + 4 + EDITS_PER_FILE * max_entry_bytes(MAX_TOKEN_SCALARS * 4)) as u64
        <= MAX_INBOX_FILE_BYTES
);

/// Edits read from the inbox, and the files they came from.
#[derive(Default)]
pub(crate) struct Pending {
    pub(crate) edits: Vec<(EditKind, String)>,
    pub(crate) files: Vec<PathBuf>,
}

/// Writes `edits` into the inbox of the store at `root`, as one file per
/// [`EDITS_PER_FILE`].
///
/// A word longer than any token is refused here, before anything is written: it
/// could never be applied, and it is what bounds the size of a file. Everything
/// else about a word is checked when it is applied.
pub(crate) fn post(root: &Path, edits: &[Edit<'_>]) -> io::Result<()> {
    if edits
        .iter()
        .any(|edit| edit.word.chars().count() > MAX_TOKEN_SCALARS)
    {
        return Err(io::Error::new(
            io::ErrorKind::InvalidInput,
            "a word is longer than any the store can hold",
        ));
    }
    if edits.is_empty() {
        return Ok(());
    }
    let directory = root.join(DIRECTORY);
    if !directory.exists() {
        fs::create_dir_all(&directory)?;
        sync_dir(root)?;
    }
    for chunk in edits.chunks(EDITS_PER_FILE) {
        write_file(&directory, chunk)?;
    }
    Ok(())
}

/// Complete before it has its name, so a reader sees it whole or not at all.
fn write_file(directory: &Path, edits: &[Edit<'_>]) -> io::Result<()> {
    let path = directory.join(name::next(directory)?);
    let temporary = path.with_extension(name::TEMPORARY);
    let mut file = fs::File::create(&temporary)?;
    file.write_all(&inbox::encode(edits))?;
    file.sync_all()?;
    fs::rename(&temporary, &path)?;
    sync_dir(directory)
}

/// Reads the oldest waiting files, up to [`MAX_INBOX_FILES`]. A damaged file is
/// deleted under [`Access::Repair`] and skipped otherwise; a file from a newer
/// build is always left for that build.
pub(crate) fn read(root: &Path, access: Access) -> io::Result<Pending> {
    let mut names = match fs::read_dir(root.join(DIRECTORY)) {
        Ok(entries) => entries
            .filter_map(Result::ok)
            .map(|entry| entry.path())
            .filter(|path| name::is_edit_file(path))
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

fn read_file(path: &Path) -> io::Result<Vec<(EditKind, String)>> {
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
