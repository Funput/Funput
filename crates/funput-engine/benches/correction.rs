//! Cost of the typo-correction search at a word boundary.
//!
//! The search is the one part of the feature that is not O(1): every combination of
//! up to two substituted keys is replayed through the composition pipeline. This
//! measures the worst case a real word can produce — ten keys, three neighbours each,
//! which is 435 replays — and the ordinary keystroke path with the feature on, which
//! must stay indistinguishable from having it off.
//!
//! Run: `cargo bench -p funput-engine --bench correction`

use std::hint::black_box;

use criterion::{Criterion, Throughput, criterion_group, criterion_main};
use funput_core::InputMethod;
use funput_engine::{Engine, KeyTouch};

/// Nine keys that end as raw keystrokes, one of them a neighbour away from `đường`.
const MISTYPED: &str = "dduwowfnh";
/// The worst case the key cap allows.
const LONGEST: &str = "dduwowfnhx";
const TELEX: &str = "Tooi yeeu tieesng Vieejt. Hoom nay troiwf nuwowcs ddepj. ";

fn engine() -> Engine {
    let mut engine = Engine::new();
    engine.update_config(|config| {
        config.method = InputMethod::Telex;
        config.typo_correction = true;
    });
    engine
}

/// Type `keys` reporting three neighbours for every one of them — the most work the
/// search can be asked to do for a word of that length.
fn type_crowded(engine: &mut Engine, keys: &str) {
    for key in keys.chars() {
        let touch = KeyTouch::new(key, 0.4)
            .with_alternate('g', 0.5)
            .with_alternate('o', 0.6)
            .with_alternate('s', 0.7);
        engine.set_next_key_touch(touch);
        let _ = engine.process_char(key);
    }
}

fn bench(c: &mut Criterion) {
    let mut group = c.benchmark_group("correction");
    for (name, keys) in [("nine-keys", MISTYPED), ("ten-keys", LONGEST)] {
        group.bench_function(name, |b| {
            b.iter(|| {
                let mut engine = engine();
                type_crowded(&mut engine, black_box(keys));
                let _ = engine.process_char(' ');
                let _ = engine.apply_correction(None);
            })
        });
    }
    group.throughput(Throughput::Elements(TELEX.chars().count() as u64));
    group.bench_function("paragraph-with-touches", |b| {
        b.iter(|| {
            let mut engine = engine();
            for key in black_box(TELEX).chars() {
                engine.set_next_key_touch(KeyTouch::new(key, 0.2));
                let _ = engine.process_char(key);
            }
        })
    });
    group.finish();
}

criterion_group!(benches, bench);
criterion_main!(benches);
