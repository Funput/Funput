use std::time::Instant;

use funput_suggestions::{SuggestionConfig, SuggestionEngine};

const SAMPLES: usize = 100_000;

fn main() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    for index in 0..5_000 {
        let word = format!("word{index:04}");
        engine.learn(&word);
        engine.learn(&word);
    }

    let mut samples = Vec::with_capacity(SAMPLES);
    let started = Instant::now();
    for index in 0..SAMPLES {
        let prefix = if index % 2 == 0 { "word1" } else { "missing" };
        let sample = Instant::now();
        std::hint::black_box(engine.suggest(std::hint::black_box(prefix)));
        samples.push(sample.elapsed().as_nanos());
    }
    let elapsed = started.elapsed();
    samples.sort_unstable();
    let percentile = |value: usize| samples[(SAMPLES * value / 100).min(SAMPLES - 1)];
    println!("samples={SAMPLES}");
    println!("p50_ns={}", percentile(50));
    println!("p95_ns={}", percentile(95));
    println!("p99_ns={}", percentile(99));
    println!(
        "queries_per_second={:.0}",
        SAMPLES as f64 / elapsed.as_secs_f64()
    );
    println!(
        "estimated_heap_bytes={}",
        engine.stats().estimated_heap_bytes
    );
}
