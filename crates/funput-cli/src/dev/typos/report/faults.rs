//! The tally split by what went wrong with the word.
//!
//! The headline numbers answer "how much does correction help?"; this table
//! answers "help with *what*?". A touch slip is one row; each kind of injected
//! fault is another, so a change to the engine shows exactly which mistakes it
//! learned to repair — and which it started to break.

use super::Outcome;
use crate::dev::typos::Fault;

/// What correction did with the words of one row.
#[derive(Default, Clone, Copy, PartialEq, Eq, Debug)]
pub(in crate::dev::typos) struct Cell {
    /// Put back to the word that was meant.
    pub(in crate::dev::typos) repaired: usize,
    /// Replaced with some other word.
    pub(in crate::dev::typos) wrong: usize,
    /// Left as it came out: no candidate, vetoed, within Δ, as typed, refused, or
    /// another real syllable.
    pub(in crate::dev::typos) untouched: usize,
}

impl Cell {
    pub(super) fn add(&mut self, outcome: Outcome) {
        match outcome {
            Outcome::Fixed => self.repaired += 1,
            Outcome::Wrong => self.wrong += 1,
            Outcome::Missed(_) | Outcome::Homophone => self.untouched += 1,
            // Nothing was wrong with the word, so there is nothing to repair.
            Outcome::Typed | Outcome::Rewritten => {}
        }
    }

    pub(super) fn merge(&mut self, other: Cell) {
        self.repaired += other.repaired;
        self.wrong += other.wrong;
        self.untouched += other.untouched;
    }

    pub(in crate::dev::typos) fn n(&self) -> usize {
        self.repaired + self.wrong + self.untouched
    }
}

/// The row a word lands in: a touch slip alone first, then the faults in order.
pub(super) fn index(fault: Option<Fault>) -> usize {
    fault.map_or(0, |fault| fault as usize + 1)
}

/// What each row is called, in [`index`] order.
pub(super) fn label(row: usize) -> &'static str {
    match row.checked_sub(1) {
        None => "touch slip only",
        Some(fault) => Fault::ALL[fault].name(),
    }
}

/// The human table: one row per kind, with how many words there were and what
/// became of them, repaired and wrong as a share of the row.
pub(super) fn print_table(by_fault: &[Cell; 5]) {
    println!("\n  By fault kind        n  repaired         wrong    untouched");
    for (row, cell) in by_fault.iter().enumerate() {
        println!(
            "    {:<16}{:>7}  {:>7} ({:>5.1}%)  {:>5} ({:>5.1}%)  {:>7}",
            label(row),
            cell.n(),
            cell.repaired,
            super::print::pct(cell.repaired, cell.n()),
            cell.wrong,
            super::print::pct(cell.wrong, cell.n()),
            cell.untouched
        );
    }
}

/// The same table as one JSON object, keyed by row label.
pub(super) fn json(by_fault: &[Cell; 5]) -> String {
    let rows: Vec<String> = by_fault
        .iter()
        .enumerate()
        .map(|(row, cell)| {
            format!(
                "\"{}\": {{\"n\": {}, \"repaired\": {}, \"wrong\": {}, \"untouched\": {}}}",
                label(row),
                cell.n(),
                cell.repaired,
                cell.wrong,
                cell.untouched
            )
        })
        .collect();
    format!("{{{}}}", rows.join(", "))
}
