//! The engine's collision lists are what `collision` makes of today's lexicon.
//!
//! The lists describe what the core composes, so a change to it can move a word
//! without anyone touching the files. Regenerating them here and comparing byte
//! for byte is what catches that.

use std::collections::BTreeSet;
use std::fs;
use std::io::Write;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};

const LEXICON: &str = include_str!("../../funput-suggestions/data/lexicon/en.tsv");
const DATA: &str = concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/../funput-engine/data/collision"
);

const REGENERATE: &str = "cargo run -p funput-lexicon-tool -- collision \
    --data crates/funput-engine/data/collision \
    --out crates/funput-engine/data/collision < crates/funput-suggestions/data/lexicon/en.tsv";

#[test]
fn the_shipped_lists_are_up_to_date() {
    let out = std::env::temp_dir().join(format!("funput-collision-{}", std::process::id()));
    let mut child = Command::new(env!("CARGO_BIN_EXE_funput-lexicon-tool"))
        .args(["collision", "--data", DATA, "--out"])
        .arg(&out)
        .stdin(Stdio::piped())
        .stderr(Stdio::null())
        .spawn()
        .expect("the tool runs");
    child
        .stdin
        .take()
        .expect("stdin is piped")
        .write_all(LEXICON.as_bytes())
        .expect("the lexicon is written");
    assert!(
        child.wait().expect("the tool finishes").success(),
        "collision failed"
    );

    // Both sides, so a class only the engine ships (or one renamed on one side) is
    // a stale list too, not a directory nobody compares.
    let generated = class_dirs(&out);
    let shipped = class_dirs(&PathBuf::from(DATA));
    assert!(!generated.is_empty(), "the tool listed no class");
    assert_eq!(
        generated, shipped,
        "data/collision/ holds other classes than the tool generates"
    );
    for class in &generated {
        let list = PathBuf::from(class).join("words.txt");
        let fresh = fs::read(out.join(&list)).expect("the tool wrote the list");
        let current = fs::read(PathBuf::from(DATA).join(&list)).unwrap_or_default();
        assert!(
            fresh == current,
            "data/collision/{} is stale; regenerate the lists with:\n  {REGENERATE}",
            list.display()
        );
    }
    fs::remove_dir_all(&out).expect("the scratch dir is removed");
}

/// The class directories under `dir`, by name.
fn class_dirs(dir: &Path) -> BTreeSet<String> {
    fs::read_dir(dir)
        .expect("the directory is readable")
        .map(|entry| entry.expect("an entry"))
        .filter(|entry| entry.path().is_dir())
        .map(|entry| entry.file_name().to_string_lossy().into_owned())
        .collect()
}
