//! The machine-readable rendering of a [`Tally`].

use std::path::Path;

use super::{Tally, faults, print::pct};
use crate::dev::typos::Options;

pub(in crate::dev::typos) fn print_json(
    corpus: &Path,
    syllables: &[String],
    tally: &Tally,
    options: &Options,
) {
    println!("{{");
    println!("  \"corpus\": \"{}\",", corpus.display());
    println!("  \"syllables\": {},", syllables.len());
    println!("  \"noise\": {:.2},", options.noise);
    println!("  \"seed\": {},", options.seed);
    println!("  \"seeds\": {},", options.seeds.max(1));
    println!(
        "  \"faults\": {{\"rate\": {:.2}, \"only\": {}}},",
        options.faults.overall,
        options
            .faults
            .only
            .map_or("null".to_owned(), |fault| format!("\"{}\"", fault.name()))
    );
    println!("  \"typed\": {},", tally.typed);
    println!("  \"slips\": {},", tally.slips());
    println!("  \"fixed\": {},", tally.fixed);
    println!("  \"wrong\": {},", tally.wrong);
    println!("  \"missed\": {},", tally.missed());
    println!("  \"missed_by_reason\": {:?},", tally.missed);
    println!("  \"rewritten\": {},", tally.rewritten);
    println!("  \"another_word\": {},", tally.homophone);
    println!("  \"by_fault\": {},", faults::json(&tally.by_fault));
    println!(
        "  \"fixed_pct_of_slips\": {:.2},",
        pct(tally.fixed, tally.slips())
    );
    println!(
        "  \"wrong_pct_of_corrections\": {:.2}",
        pct(tally.wrong, tally.corrections())
    );
    println!("}}");
}
