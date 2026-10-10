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

#[test]
fn a_host_that_reports_no_touches_pays_nothing_for_the_feature() {
    let mut off = engine(false);
    let mut on = engine(true);
    let baseline = measure(|| type_paragraph(&mut off, false));
    let with_feature = measure(|| type_paragraph(&mut on, false));
    println!("paragraph: {baseline} allocs off, {with_feature} allocs on");
    assert_eq!(
        baseline, with_feature,
        "switching typo correction on changed the keystroke hot path"
    );
}

/// With touch data flowing, every word boundary has to work out whether the word is
/// a finished syllable — the one question correction cannot answer without asking.
/// `funput-core` builds the rhyme it validates, so that is one allocation per word,
/// and only for words the English-restore path did not already check.
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

/// The search replays every one- and two-key substitution through the composition
/// pipeline, and `funput_core::apply_checked` returns a `String` per key — the
/// crate's documented ~1 allocation per keystroke. A nine-key word offering three
/// neighbours each is 396 replays, and measures at 4509 allocations: essentially one
/// per replayed key and nothing else, which is what this budget pins. It is paid once
/// per corrected word, never on the plain typing path.
const MAX_ALLOCS_PER_CORRECTED_WORD: usize = 5_000;

/// The worst case the key cap allows: every key of the word offering three
/// neighbours, which is the full 435-replay search.
fn type_mistyped_word(engine: &mut Engine) {
    for key in "dduwowfnh".chars() {
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
    println!("corrected word: {allocations} allocs");
    assert!(
        allocations <= MAX_ALLOCS_PER_CORRECTED_WORD,
        "correcting one word allocated {allocations} times, over the \
         {MAX_ALLOCS_PER_CORRECTED_WORD} budget — the search must reuse its scratch \
         session and candidate strings instead of building new ones"
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
