//! Heap-allocation budget for typo correction, from both sides.
//!
//! Switching the feature on must not cost the keystroke hot path anything, and the
//! search it adds at a word boundary must stay proportionate: it replays a bounded
//! number of candidates through the composition pipeline, each of which pays
//! `funput-core`'s documented one allocation per key, and nothing else.
//!
//! Its own binary, and its own thread-local counter, so `tests/alloc_budget.rs` keeps
//! the single-test process-global invariant it was built around.
//!
//! Print the numbers with:
//! `cargo test -p funput-engine --test alloc_budget_correction -- --nocapture`

use std::alloc::{GlobalAlloc, Layout, System};
use std::cell::Cell;

use funput_core::InputMethod;
use funput_engine::{Engine, KeyTouch};

struct CountingAllocator;

// A global allocator sees every thread; count per thread so a sibling test running in
// parallel is not charged to this budget. Both cells are const-init with no
// destructor, so reading them from inside the allocator never re-enters.
thread_local! {
    static MEASURING: Cell<bool> = const { Cell::new(false) };
    static ALLOCATIONS: Cell<usize> = const { Cell::new(0) };
}

fn record() {
    if MEASURING.get() {
        ALLOCATIONS.set(ALLOCATIONS.get() + 1);
    }
}

unsafe impl GlobalAlloc for CountingAllocator {
    unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
        record();
        unsafe { System.alloc(layout) }
    }

    unsafe fn dealloc(&self, pointer: *mut u8, layout: Layout) {
        unsafe { System.dealloc(pointer, layout) };
    }

    unsafe fn realloc(&self, pointer: *mut u8, layout: Layout, size: usize) -> *mut u8 {
        record();
        unsafe { System.realloc(pointer, layout, size) }
    }
}

#[global_allocator]
static ALLOCATOR: CountingAllocator = CountingAllocator;

fn measure(body: impl FnOnce()) -> usize {
    MEASURING.set(true);
    let before = ALLOCATIONS.get();
    body();
    let allocations = ALLOCATIONS.get() - before;
    MEASURING.set(false);
    allocations
}

const TELEX_TEXT: &str = "Tooi yeeu tieesng Vieejt. Hoom nay troiwf nuwowcs ddepj. ";

fn engine(typo_correction: bool) -> Engine {
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = InputMethod::Telex;
        config.typo_correction = typo_correction;
    });
    // Warm the lazy tables behind eager restore so one-time init is not billed.
    for key in TELEX_TEXT.chars() {
        engine.process_char(key);
    }
    engine.clear();
    engine
}

/// Type a paragraph, reporting a touch for every key but never a neighbour — so the
/// search has nothing to work with and only the bookkeeping runs.
fn type_paragraph(engine: &mut Engine, touches: bool) {
    for key in TELEX_TEXT.chars() {
        if touches {
            engine.set_next_key_touch(KeyTouch::new(key, 0.2));
        }
        std::hint::black_box(engine.process_char(key));
    }
}

/// With the feature on, every word boundary has to work out whether the word is a
/// finished syllable — the one question correction cannot answer without asking,
/// touch data or not. `funput-core` builds the rhyme it validates, so that is one
/// allocation per word, and only for words the English-restore path did not already
/// check. Nothing else is paid: the words are all syllables, so none is searched.
#[test]
fn a_host_that_reports_no_touches_pays_at_most_one_syllable_check_per_word() {
    let mut off = engine(false);
    let mut on = engine(true);
    let baseline = measure(|| type_paragraph(&mut off, false));
    let with_feature = measure(|| type_paragraph(&mut on, false));
    let words = TELEX_TEXT.split_whitespace().count();
    println!("paragraph: {baseline} allocs off, {with_feature} allocs on ({words} words)");
    assert!(
        with_feature <= baseline + words,
        "typing with the feature on allocated {with_feature} times against a baseline \
         of {baseline} over {words} words — the boundary is judging a word more than once"
    );
}

/// The switch is the only thing that costs: off again, the keystroke path is what it
/// was before the feature existed.
#[test]
fn switching_the_setting_off_again_costs_exactly_what_plain_typing_does() {
    let mut plain = engine(false);
    let mut toggled = engine(true);
    toggled.update_config(|config| config.typo_correction = false);
    let baseline = measure(|| type_paragraph(&mut plain, false));
    let after_toggle = measure(|| type_paragraph(&mut toggled, false));
    println!("paragraph: {baseline} allocs plain, {after_toggle} allocs after toggling off");
    assert_eq!(
        baseline, after_toggle,
        "switching the feature off left a cost behind"
    );
}

/// Touch data changes nothing about what a boundary pays for.
#[test]
fn reporting_touches_costs_at_most_one_syllable_check_per_word() {
    let mut off = engine(false);
    let mut on = engine(true);
    let baseline = measure(|| type_paragraph(&mut off, false));
    let with_touches = measure(|| type_paragraph(&mut on, true));
    let words = TELEX_TEXT.split_whitespace().count();
    println!(
        "paragraph: {baseline} allocs off, {with_touches} allocs with touches ({words} words)"
    );
    assert!(
        with_touches <= baseline + words,
        "typing with touch data allocated {with_touches} times against a baseline of \
         {baseline} over {words} words — the boundary is judging a word more than once"
    );
}

#[test]
fn recording_a_touch_point_never_allocates() {
    let mut engine = engine(true);
    let allocations = measure(|| {
        for _ in 0..10_000 {
            engine.set_next_key_touch(std::hint::black_box(
                KeyTouch::new('a', 0.2).with_alternate('s', 0.4),
            ));
        }
    });
    assert_eq!(
        allocations, 0,
        "reporting a touch allocated {allocations} times"
    );
}

/// The search replays variants through the composition pipeline, and
/// `funput_core::apply_checked` returns a `String` per key — the crate's documented
/// ~1 allocation per keystroke. The replay budget is 450 variants, so a twelve-key
/// word that spends all of it is about 5 400 replayed keys plus what the candidate
/// strings cost the first time: this pins that worst case. It is paid once per word
/// searched, never on the plain typing path.
const MAX_ALLOCS_PER_CORRECTED_WORD: usize = 8_000;

/// Type `keys` with every key offering three neighbours, then the space.
fn type_mistyped(engine: &mut Engine, keys: &str) {
    for key in keys.chars() {
        engine.set_next_key_touch(
            KeyTouch::new(key, 0.35)
                .with_alternate('g', 0.15)
                .with_alternate('o', 0.5)
                .with_alternate('s', 0.6),
        );
        engine.process_char(key);
    }
    engine.process_char(' ');
}

/// The worst case the key cap allows: twelve keys, three neighbours each, nothing
/// the cheap tiers settle — the search runs to its replay budget.
fn type_mistyped_word(engine: &mut Engine) {
    type_mistyped(engine, "dduwowfnhxxx");
}

#[test]
fn a_corrected_word_stays_inside_the_search_budget() {
    let mut engine = engine(true);
    type_mistyped_word(&mut engine); // first word warms the candidate strings
    engine.apply_correction(Some(0));
    engine.clear();

    let allocations = measure(|| {
        type_mistyped_word(&mut engine);
        std::hint::black_box(engine.apply_correction(Some(0)));
    });
    println!("corrected word (12 keys, budget spent): {allocations} allocs");
    assert!(
        allocations <= MAX_ALLOCS_PER_CORRECTED_WORD,
        "correcting one word allocated {allocations} times, over the \
         {MAX_ALLOCS_PER_CORRECTED_WORD} budget — the search must reuse its scratch \
         session and candidate strings instead of building new ones"
    );
}

/// A bounced key is the commonest slip and the cheapest edit: the search finds it in
/// the first tier and, once the second has shown nothing within the margin, stops
/// before it tries a single insertion. With three neighbours offered for every key
/// that is about 175 replays of a six-key word — a fifth of the budget.
#[test]
fn a_bounced_key_is_repaired_before_the_search_reaches_the_dear_tiers() {
    let mut engine = engine(true);
    type_mistyped(&mut engine, "chowii");
    engine.apply_correction(Some(0));
    engine.clear();

    let allocations = measure(|| {
        type_mistyped(&mut engine, "chowii");
        std::hint::black_box(engine.apply_correction(Some(0)));
    });
    println!("bounced key: {allocations} allocs");
    assert!(
        allocations <= 1_500,
        "repairing a bounced key allocated {allocations} times — the search did not \
         stop after the cheap tiers"
    );
}

#[test]
fn a_word_the_search_refuses_costs_no_more_than_typing_it() {
    // The gate runs before the search, so a word that is already a syllable pays for
    // the syllable check the boundary already did, and nothing else.
    let mut plain_engine = engine(true);
    let plain = measure(|| {
        for key in "chaof ".chars() {
            plain_engine.process_char(key);
        }
    });
    let mut touched_engine = engine(true);
    let touched = measure(|| {
        for key in "chaof ".chars() {
            touched_engine.set_next_key_touch(KeyTouch::new(key, 0.2).with_alternate('s', 0.4));
            touched_engine.process_char(key);
        }
    });
    println!("valid word: {plain} allocs plain, {touched} allocs with touches");
    assert_eq!(
        plain, touched,
        "a word correction refuses must cost the same"
    );
}

/// Type the worst-case word without ending it.
fn type_mistyped_keys(engine: &mut Engine) {
    for key in "dduwowfnh".chars() {
        engine.set_next_key_touch(
            KeyTouch::new(key, 0.35)
                .with_alternate('g', 0.15)
                .with_alternate('o', 0.5)
                .with_alternate('s', 0.6),
        );
        engine.process_char(key);
    }
}

/// A preview runs the search so the boundary does not have to: the space after one
/// costs what a boundary with nothing to search costs — a finished syllable's, which
/// pays for its one syllable check — plus the parked correction's bookkeeping.
#[test]
fn the_boundary_after_a_preview_does_not_search_again() {
    let mut plain = engine(true);
    for key in "chaof".chars() {
        plain.process_char(key);
    }
    let baseline = measure(|| {
        std::hint::black_box(plain.process_char(' '));
    });

    let mut previewed = engine(true);
    // One full round first, so the preview's and the parked correction's strings
    // are not billed to the boundary being measured.
    type_mistyped_keys(&mut previewed);
    previewed.preview_correction();
    previewed.process_char(' ');
    previewed.clear();
    type_mistyped_keys(&mut previewed);
    assert!(previewed.preview_correction() > 0);
    let boundary = measure(|| {
        std::hint::black_box(previewed.process_char(' '));
    });
    println!("boundary: {baseline} allocs for a syllable, {boundary} allocs after a preview");
    assert!(
        boundary <= baseline + 2,
        "the boundary allocated {boundary} times after a preview against {baseline} \
         for a word with nothing to search — it is running the search a second time"
    );
}

/// A word that could still become a syllable is answered from what the session
/// already knows: no replay, no syllable check, nothing built.
#[test]
fn previewing_a_word_that_is_still_alive_allocates_nothing() {
    let mut engine = engine(true);
    for (key, neighbour) in "chaof".chars().zip("xjsp;".chars()) {
        engine.set_next_key_touch(KeyTouch::new(key, 0.35).with_alternate(neighbour, 0.15));
        engine.process_char(key);
    }
    let allocations = measure(|| {
        std::hint::black_box(engine.preview_correction());
    });
    assert_eq!(
        allocations, 0,
        "previewing a live word allocated {allocations} times"
    );
}
