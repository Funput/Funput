use std::alloc::{GlobalAlloc, Layout, System};
use std::cell::Cell;

use funput_suggestions::{SuggestionConfig, SuggestionEngine};

struct CountingAllocator;

// A global allocator sees every thread: the test harness and any sibling test
// allocate while this one runs, and a shared counter would charge those to the
// lookup. Count per thread instead, so the budget measures the lookup path and
// nothing else. Both cells are const-init with no destructor, so reading them
// from inside the allocator never allocates or re-enters.
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

#[test]
fn warm_lookup_does_not_allocate() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    for word in ["không", "khỏe", "khoa", "hòa"] {
        engine.learn(word);
        engine.learn(word);
    }

    let allocations = measure(|| {
        for _ in 0..100_000 {
            std::hint::black_box(engine.suggest(std::hint::black_box("kh")));
        }
    });

    assert_eq!(allocations, 0, "warm lookup allocated {allocations} times");
}

/// The context path normalizes the previous word and walks its follower slots on
/// every keystroke, so it has to hold the same budget the plain lookup does.
#[test]
fn warm_lookup_with_a_context_does_not_allocate() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    for word in ["không", "khỏe", "khoa", "hòa"] {
        engine.learn(word);
        engine.learn(word);
    }
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "khỏe");
    engine.learn_after(Some("xin"), "khỏe");

    let allocations = measure(|| {
        for _ in 0..100_000 {
            std::hint::black_box(engine.suggest_with(
                std::hint::black_box(Some("xin")),
                std::hint::black_box("kh"),
            ));
        }
    });

    assert_eq!(
        allocations, 0,
        "warm lookup with a context allocated {allocations} times"
    );
}

fn measure(body: impl FnOnce()) -> usize {
    MEASURING.set(true);
    let before = ALLOCATIONS.get();
    body();
    let allocations = ALLOCATIONS.get() - before;
    MEASURING.set(false);
    allocations
}
