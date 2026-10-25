use funput_engine::Engine;

use super::measure;

/// Allocations one `vn ` cycle may cost: the two letters, then the expansion with the
/// undo bookkeeping it arms.
///
/// Measured: 11 per cycle (704 over 64), of which about three are the keystrokes
/// themselves and the rest are the expansion string, the matched form, the plain
/// form and the written text the undo remembers. The budget keeps the undo from
/// quietly growing the cost of every expansion; a word that is not an expansion
/// never pays any of it, which the second half of the check pins.
const MAX_ALLOCS_PER_EXPANSION: usize = 12;

pub(super) fn assert_shortcut_expansion_within_budget(engine: &mut Engine) {
    engine.clear();
    engine.add_shortcut("vn", "Việt Nam");
    // The first expansion boxes the undo state and sizes the strings.
    measure(engine, "vn ");

    let expansions = 64;
    let text = "vn ".repeat(expansions);
    let (allocs, bytes) = measure(engine, &text);
    println!("gõ tắt: {allocs} allocs, {bytes} bytes over {expansions} expansions");
    assert!(
        allocs <= expansions * MAX_ALLOCS_PER_EXPANSION,
        "an expansion costs {:.1} allocations (budget {MAX_ALLOCS_PER_EXPANSION}) — the \
         undo state must refill what it holds rather than build it again",
        allocs as f64 / expansions as f64
    );

    // Typing that is not an expansion must not pay for the undo at all.
    engine.clear_shortcuts();
    engine.clear();
    let plain = "toois ";
    let (without, _) = measure(engine, &plain.repeat(expansions));
    let per_word = without as f64 / expansions as f64;
    assert!(
        per_word <= 8.0,
        "ordinary words cost {per_word:.1} allocations each once the table is empty"
    );
}
