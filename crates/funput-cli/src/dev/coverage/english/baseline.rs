//! The committed counts English coverage is held to: one `name count` per line.
//!
//! Every count must match exactly. A rise is a regression. A fall is welcome, but it
//! has to be written down in the same change, so the next regression is caught from
//! the new level rather than the old one.

/// The command that rewrites the baseline, as the failure message quotes it.
pub(super) const REGENERATE: &str = "cargo run -p funput-cli -- dev coverage --english \
     --baseline benchmarks/results/english-coverage.txt --write-baseline";

/// `name count` pairs, skipping blank lines and `#` comments. A malformed line is
/// kept as a count of `usize::MAX`, which then fails the comparison by name.
pub(super) fn parse(text: &str) -> Vec<(String, usize)> {
    text.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .map(|line| {
            let (name, count) = line.split_once(' ').unwrap_or((line, ""));
            (name.to_owned(), count.trim().parse().unwrap_or(usize::MAX))
        })
        .collect()
}

pub(super) fn format(counts: &[(String, usize)]) -> String {
    let mut text = String::from(
        "# `funput dev coverage --english`: the lowercase words of en.tsv typed under\n\
         # Telex, each followed by a space, with eager restore on and off. Every count\n\
         # must match; lower one only together with the change that earns it.\n",
    );
    for (name, count) in counts {
        text.push_str(&format!("{name} {count}\n"));
    }
    text
}

/// What differs between the baseline and the counts just measured, one line each.
pub(super) fn compare(baseline: &[(String, usize)], current: &[(String, usize)]) -> Vec<String> {
    let mut problems = Vec::new();
    for (name, now) in current {
        match baseline.iter().find(|(listed, _)| listed == name) {
            None => problems.push(format!("{name} is not in the baseline")),
            Some((_, was)) if name.ends_with(".total") && was != now => problems.push(format!(
                "{name}: the lexicon has {now} words, the baseline {was}"
            )),
            Some((_, was)) if now > was => {
                problems.push(format!("{name} regressed: {was} -> {now}"));
            }
            Some((_, was)) if now < was => problems.push(format!(
                "{name} improved: {was} -> {now}; lower the baseline in this change"
            )),
            Some(_) => {}
        }
    }
    for (name, _) in baseline {
        if !current.iter().any(|(measured, _)| measured == name) {
            problems.push(format!("{name} is no longer measured"));
        }
    }
    if !problems.is_empty() {
        problems.push(format!("rewrite the baseline with: {REGENERATE}"));
    }
    problems
}
