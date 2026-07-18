use funput_engine::Engine;

use super::{assert_counts_within_budget, measure};

const CIRCUMFLEX_CANONICAL: &str = "chaan chaanf deem hoom chaats Chaan ";
const CIRCUMFLEX_FREE: &str = "chana chanaf deme homo chatas Chana ";
const W_CANONICAL: &str = "lawms cown mowif muaw truowngf nguwowif ";
const W_DEFERRED: &str = "lwams cwon mwoif mwua trwuongf ngwuoif ";
const MULTI_CANONICAL: &str = "ddwuocj ddwuongf ddwon ddwoif ";
const MULTI_W_FIRST: &str = "dwduocj dwduongf dwdon dwdoif ";

fn measure_pair(
    engine: &mut Engine,
    canonical: &str,
    alternate: &str,
) -> ((usize, usize), (usize, usize)) {
    engine.clear();
    let canonical_counts = measure(engine, canonical);
    engine.clear();
    let alternate_counts = measure(engine, alternate);
    (canonical_counts, alternate_counts)
}

fn assert_pair(label: &str, text: &str, canonical: (usize, usize), alternate: (usize, usize)) {
    println!("{label}: canonical={canonical:?}, alternate={alternate:?}");
    assert!(
        alternate.0 <= canonical.0,
        "{label} added allocation events: {canonical:?} vs {alternate:?}"
    );
    assert_counts_within_budget(label, text, alternate.0, alternate.1);
}

pub(super) fn assert_paired_allocations(engine: &mut Engine) {
    let (canonical, alternate) = measure_pair(engine, CIRCUMFLEX_CANONICAL, CIRCUMFLEX_FREE);
    assert_pair(
        "free-position circumflex",
        CIRCUMFLEX_FREE,
        canonical,
        alternate,
    );
    let (canonical, alternate) = measure_pair(engine, W_CANONICAL, W_DEFERRED);
    assert_pair("deferred w", W_DEFERRED, canonical, alternate);
    let (canonical, alternate) = measure_pair(engine, MULTI_CANONICAL, MULTI_W_FIRST);
    assert_pair("multi-intent", MULTI_W_FIRST, canonical, alternate);
}
