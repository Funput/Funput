//! The machine-readable rendering of a [`Tally`], for comparing two runs (say,
//! `main` against a branch that adds shipped pairs).

use std::path::Path;

use super::print::{method, tone};
use super::{Tally, pct};
use crate::dev::suggest::Options;

pub(in crate::dev::suggest) fn print_json(text: &Path, tally: &Tally, options: &Options) {
    let shown = tally.predict_shown();
    println!("{{");
    println!("  \"text\": {:?},", text.display().to_string());
    println!("  \"method\": \"{}\",", method(options.method));
    println!(
        "  \"shipped\": {},",
        if options.seed {
            format!("\"{}\"", tone(options.tone))
        } else {
            "null".to_owned()
        }
    );
    println!("  \"learning\": {},", options.learn);
    println!("  \"lexicon\": {},", options.lexicon.is_some());
    println!(
        "  \"thresholds\": {{\"predict_uses\": {}, \"dominance\": {}, \"evidence\": {}}},",
        options.config.context_predict_uses,
        options.config.context_dominance_percent,
        options.config.context_evidence_seen
    );
    println!("  \"words\": {},", tally.words);
    println!("  \"keys\": {},", tally.keys);
    println!("  \"saved\": {},", tally.saved);
    println!("  \"saved_pct\": {:.2},", pct(tally.saved, tally.keys));
    println!("  \"predict\": {{");
    println!("    \"chances\": {},", tally.predict_chances);
    println!("    \"shown\": {shown},");
    println!(
        "    \"right\": {{\"shipped\": {}, \"personal\": {}}},",
        tally.predict_right[0], tally.predict_right[1]
    );
    println!(
        "    \"wrong\": {{\"shipped\": {}, \"personal\": {}}},",
        tally.predict_wrong[0], tally.predict_wrong[1]
    );
    println!(
        "    \"wrong_pct_of_shown\": {:.2}",
        pct(tally.predict_wrong_total(), shown)
    );
    println!("  }},");
    println!("  \"completion\": {{");
    println!("    \"words\": {},", tally.completion_words);
    println!("    \"found\": {},", tally.completion_found);
    println!(
        "    \"prefix_letters\": {},",
        tally.completion_prefix_letters
    );
    println!("    \"bar\": {:?}", tally.bar);
    println!("  }},");
    let wrong: Vec<String> = tally
        .top_wrong(options.show)
        .into_iter()
        .map(|((previous, guess, word, source), n)| {
            format!(
                "{{\"previous\": {previous:?}, \"guess\": {guess:?}, \"typed\": {word:?}, \"source\": \"{}\", \"times\": {n}}}",
                source.name()
            )
        })
        .collect();
    println!("  \"wrong\": [{}],", wrong.join(", "));
    let silent: Vec<String> = tally
        .top_silent(options.show)
        .into_iter()
        .map(|((previous, word), n)| {
            format!("{{\"previous\": {previous:?}, \"typed\": {word:?}, \"times\": {n}}}")
        })
        .collect();
    println!("  \"silent\": [{}]", silent.join(", "));
    println!("}}");
}
