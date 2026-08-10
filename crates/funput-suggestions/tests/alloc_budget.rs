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

    MEASURING.set(true);
    let before = ALLOCATIONS.get();
    for _ in 0..100_000 {
        std::hint::black_box(engine.suggest(std::hint::black_box("kh")));
    }
    let allocations = ALLOCATIONS.get() - before;
    MEASURING.set(false);

    assert_eq!(allocations, 0, "warm lookup allocated {allocations} times");
}
