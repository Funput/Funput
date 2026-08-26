//! The batch: one entry per dropped file, each read on its own terms.
//!
//! This is the part that beats the tool people are coming from. UniKey's file
//! converter takes one source charset for a whole folder, so an archive holding
//! documents from different eras converts most of itself to nonsense. Here every
//! file goes through [`charset::document::read`] by itself, and one that nothing
//! explains waits for the user instead of being swept along.

use std::path::PathBuf;

use funput_core::charset::{self, Charset, document};

/// One file, as read.
#[derive(Clone)]
pub struct Entry {
    pub path: PathBuf,
    /// The document as characters — already past the two doors.
    pub text: String,
    /// `None` when nothing explained the bytes. The user picks, or it is skipped.
    pub charset: Option<Charset>,
    /// Characters the current target cannot represent. Recomputed on target change.
    pub unmapped: usize,
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
pub fn collect(paths: &[PathBuf]) -> Vec<PathBuf> {
    let mut out = Vec::new();
    for path in paths {
        if path.is_dir() {
            let Ok(dir) = std::fs::read_dir(path) else {
                continue;
            };
            out.extend(dir.flatten().map(|e| e.path()).filter(|p| p.is_file()));
        } else {
            out.push(path.clone());
        }
    }
    out
}

/// Read every path, skipping what cannot be read at all.
///
/// I/O bound over a folder of a few hundred documents, so a shell calls this off its
/// UI thread — inline would hold the window still for the whole drop.
pub fn scan(paths: &[PathBuf]) -> Vec<Entry> {
    paths
        .iter()
        .filter(|path| path.is_file())
        .filter_map(|path| {
            let bytes = std::fs::read(path).ok()?;
            let doc = document::read(bytes).ok()?;
            Some(Entry {
                path: path.clone(),
                text: doc.text,
                charset: doc.charset,
                unmapped: 0,
            })
        })
        .collect()
}

/// Recompute what each file would lose when written as `target`.
///
/// Conversion only, no I/O, so this is cheap enough to run every time the target
/// changes. The bytes are thrown away and made again at write time: keeping a
/// converted copy of every document in memory costs more than converting twice.
pub fn measure(entries: &mut [Entry], target: Charset) {
    for entry in entries {
        entry.unmapped = match entry.charset {
            Some(from) => {
                let unicode = charset::convert(&entry.text, from, Charset::Unicode);
                charset::convert(&unicode.text, Charset::Unicode, target).unmapped
            }
            None => 0,
        };
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
        let entries = scan(&paths);

        assert_eq!(entries.len(), 3);
        assert_eq!(entries[0].charset, Some(Charset::Tcvn3));
        assert_eq!(entries[1].charset, Some(Charset::Unicode));
        assert_eq!(entries[2].charset, None, "nothing explains these bytes");
        assert_eq!(ready(&entries), 2, "only the two that were identified");
    }

    /// The count behind "N chữ sẽ mất", and it must follow the target rather than be
    /// fixed at read time.
    #[test]
    fn what_will_be_lost_is_measured_against_the_target_of_the_moment() {
        let dir = scratch("measure");
        std::fs::write(dir.join("hoa.txt"), "Ổn").unwrap();
        let mut entries = scan(&[dir.join("hoa.txt")]);

        // TCVN3 has no code for an uppercase toned vowel.
        measure(&mut entries, Charset::Tcvn3);
        assert_eq!(entries[0].unmapped, 1);

        // The same document costs nothing going somewhere that can spell it.
        measure(&mut entries, Charset::UnicodeCombining);
        assert_eq!(entries[0].unmapped, 0);
    }

    /// Dropping a folder of documents has to work; dropping a home directory must
    /// not walk it. One level, and no further.
    #[test]
    fn a_dropped_folder_is_read_one_level_deep() {
        let dir = scratch("folder");
        std::fs::write(dir.join("top.txt"), "việt").unwrap();
        std::fs::create_dir_all(dir.join("nested")).unwrap();
        std::fs::write(dir.join("nested").join("deep.txt"), "nam").unwrap();

        let collected = collect(std::slice::from_ref(&dir));

        assert_eq!(collected.len(), 1, "the nested folder was walked into");
        assert_eq!(collected[0], dir.join("top.txt"));
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
            unmapped: 0,
        };
        assert!(out_dir_label(&[entry(&a)]).ends_with(crate::OUT_DIR));
        assert_eq!(
            out_dir_label(&[entry(&a), entry(&b)]),
            format!("{} trong 2 thư mục", crate::OUT_DIR)
        );
    }
}
