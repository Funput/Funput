//! The batch: one entry per dropped file, each read on its own terms.
//!
//! This is the part that beats the tool people are coming from. UniKey's file
//! converter takes one source charset for a whole folder, so an archive holding
//! documents from different eras converts most of itself to nonsense. Here every
//! file goes through [`charset::document::read`] by itself, and one that nothing
//! explains waits for the user instead of being swept along.

mod write;

use std::path::{Path, PathBuf};

use funput_core::charset::{self, Charset, document};

use super::view;

pub(super) use write::{OUT_DIR, Outcome, write_all};

/// One file, as read.
#[derive(Clone)]
pub(super) struct Entry {
    pub(super) path: PathBuf,
    /// The document as characters — already past the two doors.
    pub(super) text: String,
    /// `None` when nothing explained the bytes. The user picks, or it is skipped.
    pub(super) charset: Option<Charset>,
    /// Characters the current target cannot represent. Recomputed on target change.
    pub(super) unmapped: usize,
}

impl Entry {
    pub(super) fn name(&self) -> String {
        self.path
            .file_name()
            .map_or_else(|| self.path.display().to_string(), |n| {
                n.to_string_lossy().into_owned()
            })
    }
}

/// Read every path, skipping what cannot be read at all.
///
/// Runs off the UI thread — a folder of a few hundred documents is I/O bound, and
/// doing it inline would freeze the window for the whole drop.
pub(super) fn scan(paths: &[PathBuf]) -> Vec<Entry> {
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
pub(super) fn measure(entries: &mut [Entry], target: Charset) {
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
pub(super) fn ready(entries: &[Entry]) -> usize {
    entries.iter().filter(|e| e.charset.is_some()).count()
}

/// Where the converted copies will go, for the footer to show before committing.
pub(super) fn out_dir_label(entries: &[Entry]) -> String {
    let mut dirs: Vec<&Path> = entries.iter().filter_map(|e| e.path.parent()).collect();
    dirs.sort_unstable();
    dirs.dedup();
    match dirs.as_slice() {
        [] => String::new(),
        [only] => only.join(OUT_DIR).display().to_string(),
        // Dropped from several folders: each keeps its own, so no name can collide
        // with a same-named file from somewhere else.
        many => format!("{OUT_DIR}\\ trong {} thư mục", many.len()),
    }
}

/// Convert and write the batch, off the UI thread — it is file I/O over every entry.
pub(super) fn convert_all() {
    let Some(window) = view::current() else { return };
    let (entries, target) = view::STATE.with(|s| {
        let state = s.borrow();
        (state.files.clone(), view::at(state.target))
    });
    window.set_can_convert(false);
    window.set_progress("Đang chuyển…".into());
    std::thread::spawn(move || {
        let outcome = write_all(&entries, target);
        let _ = slint::invoke_from_event_loop(move || {
            let Some(window) = view::current() else { return };
            window.set_progress(report(&outcome).into());
            window.set_can_convert(true);
        });
    });
}

/// What to tell the user afterwards. A skipped file is not a failure — its row is
/// still on screen with its own picker, waiting to be settled.
fn report(outcome: &Outcome) -> String {
    let mut parts = vec![format!("Đã chuyển {} tệp", outcome.written)];
    if outcome.skipped > 0 {
        parts.push(format!("bỏ qua {} tệp chưa rõ bảng mã", outcome.skipped));
    }
    if outcome.failed > 0 {
        parts.push(format!("{} tệp ghi không được", outcome.failed));
    }
    parts.join(", ")
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
}
