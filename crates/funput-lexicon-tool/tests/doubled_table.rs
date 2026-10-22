//! The engine's doubled-key lists are what `doubled` makes of today's lexicon.
//!
//! The lists describe the reverts the engine makes, so a change to the core can
//! move a word or a decisive prefix without anyone touching the files.
//! Regenerating them here and comparing byte for byte is what catches that.

use std::fs;
use std::io::Write;
use std::path::PathBuf;
use std::process::{Command, Stdio};

const LEXICON: &str = include_str!("../../funput-suggestions/data/lexicon/en.tsv");
const DATA: &str = concat!(env!("CARGO_MANIFEST_DIR"), "/../funput-engine/data/doubled");

const REGENERATE: &str = "cargo run -p funput-lexicon-tool -- doubled \
    --overrides crates/funput-engine/data/doubled/overrides.txt \
    --out crates/funput-engine/data/doubled < crates/funput-suggestions/data/lexicon/en.tsv";

#[test]
fn the_shipped_lists_are_up_to_date() {
    let out = std::env::temp_dir().join(format!("funput-doubled-{}", std::process::id()));
    let mut child = Command::new(env!("CARGO_BIN_EXE_funput-lexicon-tool"))
        .arg("doubled")
        .arg("--overrides")
        .arg(PathBuf::from(DATA).join("overrides.txt"))
        .arg("--out")
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
        "doubled failed"
    );

    for list in ["words.txt", "decisive.txt"] {
        let generated = fs::read(out.join(list)).expect("the tool wrote the list");
        let shipped = fs::read(PathBuf::from(DATA).join(list)).expect("the list is shipped");
        assert!(
            generated == shipped,
            "data/doubled/{list} is stale; regenerate the lists with:\n  {REGENERATE}"
        );
    }
    fs::remove_dir_all(&out).expect("the scratch dir is removed");
}
