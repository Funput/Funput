//! Per-keystroke transform latency/throughput for `funput-core`.
//!
//! Measures the pure transform (`apply` / `apply_checked`) by replaying a realistic
//! Vietnamese key sequence, resetting the buffer at each word boundary like a real
//! shell does. Reports ns/keystroke and keystrokes/second per input method.
//!
//! Run: `cargo bench -p funput-core`

use std::hint::black_box;

use criterion::{BenchmarkId, Criterion, Throughput, criterion_group, criterion_main};
use funput_core::{InputMethod, ToneStyle, apply_checked};

// Realistic typing with tones, circumflex/horn/breve and the `đ` stroke.
const TELEX: &str = "tooi yeeu tieesng vieejt nam ddaats nuwowcs hoom nay troiwf ddepj";
const VNI: &str = "to6i ye6u tie6ng1 vie6t5 nam dda6t1 nu7o7c1 ho6m nay tro7i2 dde9p5";
const TELEX_CIRCUMFLEX_CANONICAL: &str = "chaan chaanf deem hoom chaats Chaan";
const TELEX_CIRCUMFLEX_FREE: &str = "chana chanaf deme homo chatas Chana";

/// Replay `keys` through `apply_checked`, folding the buffer at each space.
fn type_through(keys: &str, method: InputMethod, spell_check: bool) -> String {
    let mut buffer = String::new();
    for k in keys.chars() {
        if k == ' ' {
            buffer.clear();
            continue;
        }
        buffer = apply_checked(&buffer, k, method, ToneStyle::Traditional, spell_check).text;
    }
    buffer
}

fn bench(c: &mut Criterion) {
    let mut group = c.benchmark_group("apply");
    for (name, keys, method) in [
        ("telex", TELEX, InputMethod::Telex),
        ("vni", VNI, InputMethod::Vni),
    ] {
        group.throughput(Throughput::Elements(keys.chars().count() as u64));
        group.bench_function(BenchmarkId::new("compose", name), |b| {
            b.iter(|| type_through(black_box(keys), method, false))
        });
        group.bench_function(BenchmarkId::new("spellcheck", name), |b| {
            b.iter(|| type_through(black_box(keys), method, true))
        });
    }

    for (name, keys) in [
        ("canonical", TELEX_CIRCUMFLEX_CANONICAL),
        ("free-position", TELEX_CIRCUMFLEX_FREE),
    ] {
        group.throughput(Throughput::Elements(keys.chars().count() as u64));
        group.bench_function(BenchmarkId::new("circumflex", name), |b| {
            b.iter(|| type_through(black_box(keys), InputMethod::Telex, false))
        });
    }
    group.finish();
}

criterion_group!(benches, bench);
criterion_main!(benches);
