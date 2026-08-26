//! Writing the converted copies out.
//!
//! Never over the originals. These are the documents someone still needs, often the
//! only copy, and a conversion tool that edits them in place is one bad guess away
//! from destroying an archive. Every file is written into a subfolder beside where
//! it came from, under its own name.

use std::path::{Path, PathBuf};

use funput_core::charset::{self, Charset};

use super::Entry;

/// The subfolder converted copies land in, beside the originals.
pub(in crate::ui::convert) const OUT_DIR: &str = "Đã chuyển mã";

/// What a batch came to.
pub(in crate::ui::convert) struct Outcome {
    pub(in crate::ui::convert) written: usize,
    pub(in crate::ui::convert) skipped: usize,
    pub(in crate::ui::convert) failed: usize,
}

/// Convert and write every entry that has a charset. Runs off the UI thread.
///
/// A file with no charset is *skipped*, not guessed at — the row is still on screen
/// with its own picker, and the user can settle it and run again.
pub(in crate::ui::convert) fn write_all(entries: &[Entry], target: Charset) -> Outcome {
    let mut outcome = Outcome {
        written: 0,
        skipped: 0,
        failed: 0,
    };
    for entry in entries {
        let Some(from) = entry.charset else {
            outcome.skipped += 1;
            continue;
        };
        // Through the pivot, same as everywhere else: read into Unicode, then write
        // Unicode out as the target. `encode_bytes` is what makes a legacy target one
        // byte per letter rather than two.
        let unicode = charset::convert(&entry.text, from, Charset::Unicode);
        let (bytes, _) = charset::encode_bytes(&unicode.text, target);
        match destination(&entry.path).and_then(|path| std::fs::write(path, &bytes).ok()) {
            Some(()) => outcome.written += 1,
            None => outcome.failed += 1,
        }
    }
    outcome
}

/// A path in the output folder that no file occupies yet.
///
/// Creates the folder on the way. Returns `None` when the folder cannot be made —
/// a read-only volume, or a file already sitting where the folder should go.
fn destination(source: &Path) -> Option<PathBuf> {
    let dir = source.parent()?.join(OUT_DIR);
    std::fs::create_dir_all(&dir).ok()?;
    let name = source.file_name()?;
    let candidate = dir.join(name);
    if !candidate.exists() {
        return Some(candidate);
    }
    Some(numbered(&dir, source))
}

/// `vanban.txt` → `vanban (2).txt`, counting up until nothing is there.
///
/// Converting the same folder twice is an ordinary thing to do — a second target, or
/// a first attempt with the wrong source — and neither run should eat the other.
fn numbered(dir: &Path, source: &Path) -> PathBuf {
    let stem = source.file_stem().unwrap_or_default().to_string_lossy();
    let ext = source.extension().map(|e| e.to_string_lossy().into_owned());
    for n in 2..1000 {
        let name = match &ext {
            Some(ext) => format!("{stem} ({n}).{ext}"),
            None => format!("{stem} ({n})"),
        };
        let candidate = dir.join(name);
        if !candidate.exists() {
            return candidate;
        }
    }
    // A thousand copies of one name is not a real situation; overwrite the last
    // rather than fail the whole batch on it.
    dir.join(source.file_name().unwrap_or_default())
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A scratch folder of our own, so the tests can look at real files without
    /// pulling in a dependency for it.
    fn scratch(name: &str) -> PathBuf {
        let dir = std::env::temp_dir().join(format!("funput-convert-{name}"));
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).expect("scratch dir");
        dir
    }

    fn entry(dir: &Path, name: &str, text: &str, charset: Option<Charset>) -> Entry {
        let path = dir.join(name);
        std::fs::write(&path, "placeholder").expect("seed file");
        Entry {
            path,
            text: text.to_string(),
            charset,
            unmapped: 0,
        }
    }

    /// The originals are what someone still needs, often the only copy. Converted
    /// copies go beside them, never over them.
    #[test]
    fn the_original_is_left_exactly_as_it_was() {
        let dir = scratch("keeps-original");
        let entries = [entry(&dir, "vanban.txt", "Việt", Some(Charset::Unicode))];
        let outcome = write_all(&entries, Charset::Tcvn3);

        assert_eq!(outcome.written, 1);
        assert_eq!(
            std::fs::read(dir.join("vanban.txt")).unwrap(),
            b"placeholder"
        );
        assert_eq!(
            std::fs::read(dir.join(OUT_DIR).join("vanban.txt")).unwrap(),
            b"Vi\xD6t"
        );
    }

    /// Converting the same folder twice is ordinary — a second target, or a first go
    /// with the wrong source. Neither run may eat the other.
    #[test]
    fn a_second_run_does_not_overwrite_the_first() {
        let dir = scratch("second-run");
        let entries = [entry(&dir, "vanban.txt", "Việt", Some(Charset::Unicode))];
        write_all(&entries, Charset::Tcvn3);
        write_all(&entries, Charset::VniWindows);

        let out = dir.join(OUT_DIR);
        assert_eq!(std::fs::read(out.join("vanban.txt")).unwrap(), b"Vi\xD6t");
        assert!(out.join("vanban (2).txt").exists(), "second run was lost");
    }

    /// A file nothing explained is skipped, not converted on a guess. Its row is
    /// still on screen with a picker of its own.
    #[test]
    fn a_file_with_no_charset_is_skipped_rather_than_guessed_at() {
        let dir = scratch("skips-unknown");
        let entries = [
            entry(&dir, "known.txt", "Việt", Some(Charset::Unicode)),
            entry(&dir, "mystery.txt", "????", None),
        ];
        let outcome = write_all(&entries, Charset::Tcvn3);

        assert_eq!((outcome.written, outcome.skipped), (1, 1));
        assert!(!dir.join(OUT_DIR).join("mystery.txt").exists());
    }
}
