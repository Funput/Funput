use std::fmt::Write;
use std::hint::black_box;

use criterion::{BatchSize, BenchmarkId, Criterion, Throughput, criterion_group, criterion_main};
use funput_suggestions::{SuggestionConfig, SuggestionEngine};
use tempfile::TempDir;

fn populated() -> SuggestionEngine {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    for index in 0..5_000 {
        let word = format!("word{index:04}");
        engine.learn(&word);
        engine.learn(&word);
    }
    for word in ["không", "khỏe", "khoa", "hòa", "hoa"] {
        engine.learn(word);
        engine.learn(word);
    }
    engine
}

fn bench_lookup(c: &mut Criterion) {
    let engine = populated();
    let mut group = c.benchmark_group("suggestions/lookup");
    group.throughput(Throughput::Elements(1));
    for (name, prefix) in [
        ("exact-short", "w"),
        ("exact-medium", "word1"),
        ("exact-long", "word1234"),
        ("folded", "ho"),
        ("miss", "zzzz"),
    ] {
        group.bench_with_input(BenchmarkId::new("top3", name), prefix, |b, prefix| {
            b.iter(|| black_box(engine.suggest(black_box(prefix))).len());
        });
    }
    group.finish();
}

fn bench_learning(c: &mut Criterion) {
    let mut engine = populated();
    c.bench_function("suggestions/learn/existing", |b| {
        b.iter(|| black_box(engine.learn(black_box("word1234"))))
    });

    // The eviction path, on a lexicon already full of promoted words. Each
    // iteration learns one new word twice: the first call evicts a promoted word
    // to take its slot, the second promotes the newcomer so the next iteration
    // evicts a promoted word in turn. One promoted eviction per iteration is
    // exactly the case that used to rebuild both tries on the spot.
    let mut engine = populated();
    let mut token = String::with_capacity(16);
    let mut counter = 0u32;
    c.bench_function("suggestions/learn/evicting", |b| {
        b.iter(|| {
            counter += 1;
            token.clear();
            write!(token, "evict{counter:07}").unwrap();
            engine.learn(black_box(&token));
            black_box(engine.learn(black_box(&token)))
        })
    });
}

fn persistent() -> (TempDir, SuggestionEngine) {
    let directory = tempfile::tempdir().unwrap();
    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    for index in 0..5_000 {
        let word = format!("word{index:04}");
        engine.learn(&word);
        engine.learn(&word);
    }
    engine.compact().unwrap();
    (directory, engine)
}

fn bench_persistence(c: &mut Criterion) {
    let (directory, mut engine) = persistent();
    c.bench_function("suggestions/persistence/open_snapshot", |b| {
        b.iter(|| {
            black_box(
                SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap(),
            )
        })
    });

    for index in 0..100 {
        engine.learn(&format!("journal{index:03}"));
    }
    engine.flush().unwrap();
    c.bench_function("suggestions/persistence/open_with_journal", |b| {
        b.iter(|| {
            black_box(
                SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap(),
            )
        })
    });

    let mut group = c.benchmark_group("suggestions/persistence/write");
    group.sample_size(20);
    group.bench_function("flush_256", |b| {
        b.iter_batched(
            || {
                let directory = tempfile::tempdir().unwrap();
                let mut engine =
                    SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
                for index in 0..256 {
                    engine.learn(&format!("pending{index:03}"));
                }
                (directory, engine)
            },
            |(_directory, mut engine)| {
                engine.flush().unwrap();
                black_box(())
            },
            BatchSize::SmallInput,
        )
    });
    group.bench_function("compact_5000", |b| {
        b.iter_batched(
            persistent,
            |(_directory, mut engine)| black_box(engine.compact().unwrap()),
            BatchSize::LargeInput,
        )
    });
    group.finish();
}

criterion_group!(benches, bench_lookup, bench_learning, bench_persistence);
criterion_main!(benches);
