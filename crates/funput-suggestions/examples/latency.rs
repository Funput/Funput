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

    report("", &engine, ["word1", "missing"]);
    println!(
        "estimated_heap_bytes={}",
        engine.stats().estimated_heap_bytes
    );

    // The same engine with the shipped word list attached, alternating a heavy
    // prefix the lexicon answers with one nobody does.
    #[cfg(feature = "lexicon-build")]
    {
        use std::io::Write;

        let tsv = include_str!("../data/lexicon/en.tsv");
        let bytes = funput_suggestions::lexicon_build::encode(tsv).unwrap();
        let mut file = tempfile::NamedTempFile::new().unwrap();
        file.write_all(&bytes).unwrap();
        engine.attach_lexicon(file.path()).unwrap();
        report("lexicon_", &engine, ["th", "zzq"]);
        println!(
            "lexicon_estimated_heap_bytes={}",
            engine.stats().estimated_heap_bytes
        );
    }
}

/// Alternates the two prefixes and prints the percentiles under `label`.
fn report(label: &str, engine: &SuggestionEngine, prefixes: [&str; 2]) {
    let mut samples = Vec::with_capacity(SAMPLES);
    let started = Instant::now();
    for index in 0..SAMPLES {
        let prefix = prefixes[index % 2];
        let sample = Instant::now();
        std::hint::black_box(engine.suggest(std::hint::black_box(prefix)));
        samples.push(sample.elapsed().as_nanos());
    }
    let elapsed = started.elapsed();
    samples.sort_unstable();
    let percentile = |value: usize| samples[(SAMPLES * value / 100).min(SAMPLES - 1)];
    println!("{label}samples={SAMPLES}");
    println!("{label}p50_ns={}", percentile(50));
    println!("{label}p95_ns={}", percentile(95));
    println!("{label}p99_ns={}", percentile(99));
    println!(
        "{label}queries_per_second={:.0}",
        SAMPLES as f64 / elapsed.as_secs_f64()
    );
}
