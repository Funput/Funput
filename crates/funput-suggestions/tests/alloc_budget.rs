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

/// Prediction runs after every space, so it is on the same budget: it normalizes
/// the previous word and walks four follower slots with no heap in sight.
#[test]
fn warm_prediction_does_not_allocate() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    engine.learn_after(Some("xin"), "chào");

    let allocations = measure(|| {
        for _ in 0..100_000 {
            std::hint::black_box(
                engine.suggest_with(std::hint::black_box(Some("xin")), std::hint::black_box("")),
            );
        }
    });

    assert_eq!(
        allocations, 0,
        "warm prediction allocated {allocations} times"
    );
}

/// The lexicon cases need the encoder, which only the `lexicon-build` feature
/// exposes. `cargo test --workspace` turns it on through funput-lexicon-tool;
/// run `cargo test -p funput-suggestions --features lexicon-build` alone.
#[cfg(feature = "lexicon-build")]
mod lexicon {
    use std::io::Write;

    use funput_suggestions::lexicon_build::encode;

    use super::*;

    const EN_TSV: &str = include_str!("../data/lexicon/en.tsv");

    fn attached(engine: &mut SuggestionEngine) -> tempfile::NamedTempFile {
        let mut file = tempfile::NamedTempFile::new().unwrap();
        file.write_all(&encode(EN_TSV).unwrap()).unwrap();
        engine.attach_lexicon(file.path()).unwrap();
        file
    }

    /// Every shape the merged path takes on a keystroke: a heavy prefix, a light
    /// one, a miss, one the personal store answers in part, and one with a
    /// context — all against the shipped word list, through the mapped file.
    #[test]
    fn warm_lookup_through_the_lexicon_does_not_allocate() {
        let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
        for word in ["không", "khỏe", "work", "world"] {
            engine.learn(word);
            engine.learn(word);
        }
        engine.learn_after(None, "xin");
        engine.learn_after(Some("xin"), "world");
        let _file = attached(&mut engine);

        for (previous, prefix) in [
            (None, "th"),
            (None, "quiz"),
            (None, "zzq"),
            (None, "wo"),
            (None, "kh"),
            (Some("xin"), "wo"),
        ] {
            let allocations = measure(|| {
                for _ in 0..100_000 {
                    std::hint::black_box(engine.suggest_with(
                        std::hint::black_box(previous),
                        std::hint::black_box(prefix),
                    ));
                }
            });
            assert_eq!(
                allocations, 0,
                "lexicon lookup {previous:?} {prefix:?} allocated {allocations} times"
            );
        }
    }

    /// Deciding to yield reads the personal answer's marks on every keystroke of
    /// a Vietnamese typist, so it holds the same budget.
    #[test]
    fn a_lookup_that_yields_to_a_vietnamese_store_does_not_allocate() {
        let config = SuggestionConfig {
            lexicon_yield_after_words: 1,
            ..SuggestionConfig::default()
        };
        let mut engine = SuggestionEngine::in_memory(config);
        for word in ["ăn", "anh"] {
            engine.learn(word);
            engine.learn(word);
        }
        let _file = attached(&mut engine);
        assert_eq!(
            engine.suggest("an").len(),
            2,
            "the case must actually yield"
        );

        let allocations = measure(|| {
            for _ in 0..100_000 {
                std::hint::black_box(engine.suggest(std::hint::black_box("an")));
            }
        });
        assert_eq!(
            allocations, 0,
            "a yielding lookup allocated {allocations} times"
        );
    }
}

fn measure(body: impl FnOnce()) -> usize {
    MEASURING.set(true);
    let before = ALLOCATIONS.get();
    body();
    let allocations = ALLOCATIONS.get() - before;
    MEASURING.set(false);
    allocations
}
