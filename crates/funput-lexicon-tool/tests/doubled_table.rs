//! The engine's doubled-key list is what `doubled` makes of today's lexicon.
//!
//! The list describes the reverts the engine makes, so a change to the core can
//! move a word in or out of it without anyone touching the file. Regenerating it
//! here and comparing byte for byte is what catches that.

use std::io::Write;
use std::process::{Command, Stdio};

const LEXICON: &str = include_str!("../../funput-suggestions/data/lexicon/en.tsv");
const SHIPPED: &str = include_str!("../../funput-engine/data/doubled/words.txt");
const OVERRIDES: &str = concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/../funput-engine/data/doubled/overrides.txt"
);

const REGENERATE: &str = "cargo run -p funput-lexicon-tool -- doubled \
    --overrides crates/funput-engine/data/doubled/overrides.txt \
    < crates/funput-suggestions/data/lexicon/en.tsv > crates/funput-engine/data/doubled/words.txt";

#[test]
fn the_shipped_list_is_up_to_date() {
    let mut child = Command::new(env!("CARGO_BIN_EXE_funput-lexicon-tool"))
        .args(["doubled", "--overrides", OVERRIDES])
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .spawn()
        .expect("the tool runs");
    child
        .stdin
        .take()
        .expect("stdin is piped")
        .write_all(LEXICON.as_bytes())
        .expect("the lexicon is written");
    let output = child.wait_with_output().expect("the tool finishes");
    assert!(output.status.success(), "doubled failed");
    let generated = String::from_utf8(output.stdout).expect("the list is UTF-8");
    assert!(
        generated == SHIPPED,
        "data/doubled/words.txt is stale; regenerate it with:\n  {REGENERATE}"
    );
}
