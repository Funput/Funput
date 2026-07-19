use std::alloc::{GlobalAlloc, Layout, System};
use std::sync::atomic::{AtomicUsize, Ordering};

use funput_suggestions::{SuggestionConfig, SuggestionEngine};

struct CountingAllocator;

static ALLOCATIONS: AtomicUsize = AtomicUsize::new(0);

unsafe impl GlobalAlloc for CountingAllocator {
    unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
        ALLOCATIONS.fetch_add(1, Ordering::Relaxed);
        unsafe { System.alloc(layout) }
    }

    unsafe fn dealloc(&self, pointer: *mut u8, layout: Layout) {
        unsafe { System.dealloc(pointer, layout) };
    }

    unsafe fn realloc(&self, pointer: *mut u8, layout: Layout, size: usize) -> *mut u8 {
        ALLOCATIONS.fetch_add(1, Ordering::Relaxed);
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
    let before = ALLOCATIONS.load(Ordering::Relaxed);
    for _ in 0..100_000 {
        std::hint::black_box(engine.suggest(std::hint::black_box("kh")));
    }
    let allocations = ALLOCATIONS.load(Ordering::Relaxed) - before;
    assert_eq!(allocations, 0, "warm lookup allocated {allocations} times");
}
