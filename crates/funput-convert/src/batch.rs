//! The batch: one entry per dropped file, each read on its own terms.
//!
//! This is the part that beats the tool people are coming from. UniKey's file
//! converter takes one source charset for a whole folder, so an archive holding
//! documents from different eras converts most of itself to nonsense. Here every
//! file goes through [`charset::document::read`] by itself, and one that nothing
//! explains waits for the user instead of being swept along.

mod write;

use std::collections::HashSet;
use std::path::PathBuf;

use funput_core::charset::{Charset, document};

use crate::session::Unreadable;

pub use write::{OUT_DIR, Outcome, report, write_all};

/// One file, as read.
#[derive(Clone)]
pub struct Entry {
    pub path: PathBuf,
    /// The document as characters — already past the two doors.
    pub text: String,
    /// `None` when nothing explained the bytes. The user picks, or it is skipped.
    pub charset: Option<Charset>,
}

impl Entry {
    pub fn name(&self) -> String {
        self.path.file_name().map_or_else(
            || self.path.display().to_string(),
            |n| n.to_string_lossy().into_owned(),
        )
    }
}

/// Expand what was dropped into the files to read.
///
/// **One level, never recursive.** Dropping a folder of documents is the ordinary
/// case and has to work; walking a whole home directory because someone let go over
/// the wrong icon is the failure mode that makes a tool feel dangerous. A folder
/// inside a folder is a deliberate second drop away.
///
/// Order is `read_dir` order, which is the filesystem's, not sorted: the rows carry
/// file names and the user reads those, not positions.
///
/// **A path named twice is read once.** Dragging a folder and one of the files
/// inside it is an ordinary slip, and so is letting go over a selection that already
/// held the folder. Two rows for one file would be confusing on screen and would ask
/// the writer for two copies of the same document.
pub fn collect(paths: &[PathBuf]) -> Vec<PathBuf> {
    let mut seen = HashSet::new();
    let mut out = Vec::new();
    for path in paths {
        if path.is_dir() {
            let Ok(dir) = std::fs::read_dir(path) else {
                continue;
            };
            for entry in dir.flatten().map(|e| e.path()).filter(|p| p.is_file()) {
                if seen.insert(entry.clone()) {
                    out.push(entry);
                }
            }
        } else if seen.insert(path.clone()) {
            out.push(path.clone());
        }
    }
    out
}

/// What a drop turned out to be: the files that could be read, and the ones that
/// could not.
pub struct Scan {
    pub(crate) entries: Vec<Entry>,
    pub(crate) unreadable: Vec<Unreadable>,
}

/// Read every path, **naming** what cannot be read rather than swallowing it.
///
/// Ten files dropped and eight rows shown is a question a count cannot answer, and
/// the reason separates "fix the permissions" from "that file is not what you think
/// it is". This used to be `.ok()?` twice.
///
/// I/O bound over a folder of a few hundred documents, so a shell calls this off its
/// UI thread — inline would hold the window still for the whole drop.
pub fn scan(paths: &[PathBuf]) -> Scan {
    let mut scan = Scan {
        entries: Vec::new(),
        unreadable: Vec::new(),
    };
    for path in paths.iter().filter(|path| path.is_file()) {
        let name = || {
            path.file_name().map_or_else(
                || path.display().to_string(),
                |n| n.to_string_lossy().into_owned(),
            )
        };
        match std::fs::read(path).map(document::read) {
            Ok(Ok(doc)) => scan.entries.push(Entry {
                path: path.clone(),
                text: doc.text,
                charset: doc.charset,
            }),
            // The one shape `document::read` refuses outright: a UTF-16 file whose
            // last character is half there. Worth saying, because it means the file
            // is truncated rather than in some charset we do not know.
            Ok(Err(_)) => scan.unreadable.push(Unreadable {
                name: name(),
                reason: "tệp UTF-16 bị cắt cụt".to_string(),
            }),
            Err(error) => scan.unreadable.push(Unreadable {
                name: name(),
                reason: reason(&error),
            }),
        }
    }
    scan
}

/// Why a file would not open, in the words a user can act on.
fn reason(error: &std::io::Error) -> String {
    match error.kind() {
        std::io::ErrorKind::PermissionDenied => "không có quyền đọc".to_string(),
        std::io::ErrorKind::NotFound => "không còn ở đó nữa".to_string(),
        _ => format!("đọc không được: {error}"),
    }
}

/// How many files are ready to convert — the ones a charset was settled for.
pub fn ready(entries: &[Entry]) -> usize {
    entries.iter().filter(|e| e.charset.is_some()).count()
}

/// Where the converted copies will go, for a footer to show before committing.
pub fn out_dir_label(entries: &[Entry]) -> String {
    let mut dirs: Vec<&std::path::Path> = entries.iter().filter_map(|e| e.path.parent()).collect();
    dirs.sort_unstable();
    dirs.dedup();
    match dirs.as_slice() {
        [] => String::new(),
        [only] => only.join(crate::OUT_DIR).display().to_string(),
        // Dropped from several folders: each keeps its own, so no name can collide
        // with a same-named file from somewhere else.
        many => format!("{} trong {} thư mục", crate::OUT_DIR, many.len()),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn scratch(name: &str) -> PathBuf {
        let dir = std::env::temp_dir().join(format!("funput-scan-{name}"));
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).expect("scratch dir");
        dir
    }

    /// The reason this window exists rather than the one people use today. UniKey's
    /// file converter takes one source charset for a whole folder, so an archive
    /// holding documents from different eras converts most of itself to nonsense.
    #[test]
    fn a_folder_of_mixed_charsets_is_read_one_file_at_a_time() {
        let dir = scratch("mixed");
        // The same sentence, stored three different ways, plus one nothing explains.
        std::fs::write(dir.join("a.txt"), b"vi\xD6t nam h\xB5 n\xE9i").unwrap();
        std::fs::write(dir.join("b.txt"), "việt nam hà nội").unwrap();
        std::fs::write(dir.join("c.txt"), b"the quick brown \xFF fox jumps over it").unwrap();

        let paths = [dir.join("a.txt"), dir.join("b.txt"), dir.join("c.txt")];
        let entries = scan(&paths).entries;

        assert_eq!(entries.len(), 3);
        assert_eq!(entries[0].charset, Some(Charset::Tcvn3));
        assert_eq!(entries[1].charset, Some(Charset::Unicode));
        assert_eq!(entries[2].charset, None, "nothing explains these bytes");
        assert_eq!(ready(&entries), 2, "only the two that were identified");
    }

    /// Dragging a folder and one of the files inside it is an ordinary slip. Two
    /// rows for one document would be confusing on screen and would ask the writer
    /// for two copies of it.
    #[test]
    fn a_path_named_twice_is_read_once() {
        let dir = scratch("twice");
        std::fs::write(dir.join("top.txt"), "việt").unwrap();

        let collected = collect(&[dir.clone(), dir.join("top.txt"), dir.clone()]);

        assert_eq!(collected, vec![dir.join("top.txt")]);
    }

    /// One folder names itself; several cannot, because each keeps its own output
    /// folder beside it.
    #[test]
    fn several_source_folders_are_counted_rather_than_listed() {
        let a = scratch("label-a");
        let b = scratch("label-b");
        let entry = |dir: &PathBuf| Entry {
            path: dir.join("x.txt"),
            text: String::new(),
            charset: None,
        };
        assert!(out_dir_label(&[entry(&a)]).ends_with(crate::OUT_DIR));
        assert_eq!(
            out_dir_label(&[entry(&a), entry(&b)]),
            format!("{} trong 2 thư mục", crate::OUT_DIR)
        );
    }
}
