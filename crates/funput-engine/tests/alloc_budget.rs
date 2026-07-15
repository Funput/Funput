//! Heap-allocation budget for the per-keystroke hot path.
//!
//! Counts every allocation the engine makes while typing a realistic Vietnamese
//! sentence, and fails if the average per keystroke exceeds the budget. This is
//! the regression guard for "least memory possible" work: optimizations ratchet
//! the budgets down; a change that silently adds allocations to the hot path
//! trips the test.
//!
//! Print the current numbers with:
//! `cargo test -p funput-engine --test alloc_budget -- --nocapture`
//!
//! The counting allocator is process-global, so this file must stay its own
//! integration-test binary and hold exactly one `#[test]` — a second test running
//! in parallel would pollute the counters.

use std::alloc::{GlobalAlloc, Layout, System};
use std::sync::atomic::{AtomicUsize, Ordering};

use funput_engine::Engine;

/// Measured baseline (2026-07, after the zero-alloc core + engine rewrite):
/// 1.1 allocs / 7 B per keystroke, with or without spell-check (identical in
/// debug and release) — essentially just the composed-text `String` the core
/// API returns. Budgets leave a little headroom for legitimate feature work;
/// ratchet them down if the hot path sheds its last allocation.
const MAX_ALLOCS_PER_KEY: f64 = 2.0;
const MAX_BYTES_PER_KEY: f64 = 24.0;

static ALLOCS: AtomicUsize = AtomicUsize::new(0);
static BYTES: AtomicUsize = AtomicUsize::new(0);

/// Counts allocations and requested bytes, then defers to the system allocator.
struct CountingAlloc;

unsafe impl GlobalAlloc for CountingAlloc {
    unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
        ALLOCS.fetch_add(1, Ordering::Relaxed);
        BYTES.fetch_add(layout.size(), Ordering::Relaxed);
        unsafe { System.alloc(layout) }
    }

    unsafe fn dealloc(&self, ptr: *mut u8, layout: Layout) {
        unsafe { System.dealloc(ptr, layout) }
    }

    unsafe fn realloc(&self, ptr: *mut u8, layout: Layout, new_size: usize) -> *mut u8 {
        // A grow-in-place still hits the allocator, so it counts as one event.
        ALLOCS.fetch_add(1, Ordering::Relaxed);
        BYTES.fetch_add(new_size, Ordering::Relaxed);
        unsafe { System.realloc(ptr, layout, new_size) }
    }
}

#[global_allocator]
static ALLOCATOR: CountingAlloc = CountingAlloc;

/// Realistic Telex input: tones, circumflex/horn/breve, the `đ` stroke, word
/// boundaries, and an English word (`text`) to exercise eager restore.
const TELEX_TEXT: &str =
    "Tooi yeeu tieesng Vieejt. Hoom nay troiwf nuwowcs ddepj. Go~ text nhanh! ";

/// Type `text` through `engine`, returning (allocs, bytes) attributed to it.
fn measure(engine: &mut Engine, text: &str) -> (usize, usize) {
    let before_allocs = ALLOCS.load(Ordering::Relaxed);
    let before_bytes = BYTES.load(Ordering::Relaxed);
    for key in text.chars() {
        std::hint::black_box(engine.process_char(key));
    }
    (
        ALLOCS.load(Ordering::Relaxed) - before_allocs,
        BYTES.load(Ordering::Relaxed) - before_bytes,
    )
}

fn assert_within_budget(label: &str, engine: &mut Engine) {
    let keys = TELEX_TEXT.chars().count() as f64;
    let (allocs, bytes) = measure(engine, TELEX_TEXT);
    let allocs_per_key = allocs as f64 / keys;
    let bytes_per_key = bytes as f64 / keys;
    println!("{label}: {allocs_per_key:.1} allocs/key, {bytes_per_key:.0} bytes/key");
    assert!(
        allocs_per_key <= MAX_ALLOCS_PER_KEY,
        "{label}: {allocs_per_key:.1} allocs/key exceeds the {MAX_ALLOCS_PER_KEY} budget \
         — a change added heap allocations to the keystroke hot path"
    );
    assert!(
        bytes_per_key <= MAX_BYTES_PER_KEY,
        "{label}: {bytes_per_key:.0} bytes/key exceeds the {MAX_BYTES_PER_KEY} budget \
         — a change added heap traffic to the keystroke hot path"
    );
}

#[test]
fn keystroke_alloc_budget() {
    let mut engine = Engine::new();

    // Warm-up: populate lazy statics (the deshaped-rhyme table behind eager
    // restore) so one-time init is not billed to the steady-state measurement.
    for key in TELEX_TEXT.chars() {
        engine.process_char(key);
    }
    engine.clear();

    assert_within_budget("default settings", &mut engine);

    engine.set_spell_check(true);
    assert_within_budget("spell-check on", &mut engine);
}
