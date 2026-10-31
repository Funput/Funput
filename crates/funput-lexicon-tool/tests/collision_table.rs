//! The engine's collision lists are what `collision` makes of today's lexicon.
//!
//! The lists describe what the core composes, so a change to it can move a word
//! without anyone touching the files. Regenerating them here and comparing byte
//! for byte is what catches that.

use std::fs;
use std::io::Write;
use std::path::PathBuf;
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

    let classes = fs::read_dir(&out).expect("the tool wrote its lists");
    let mut compared = 0;
    for class in classes {
        let name = class.expect("a class dir").file_name();
        let list = PathBuf::from(&name).join("words.txt");
        let generated = fs::read(out.join(&list)).expect("the tool wrote the list");
        let shipped = fs::read(PathBuf::from(DATA).join(&list)).unwrap_or_default();
        assert!(
            generated == shipped,
            "data/collision/{} is stale; regenerate the lists with:\n  {REGENERATE}",
            list.display()
        );
        compared += 1;
    }
    assert!(compared > 0, "the tool listed no class");
    fs::remove_dir_all(&out).expect("the scratch dir is removed");
}
