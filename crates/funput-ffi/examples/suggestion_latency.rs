use std::time::Instant;

use funput_ffi::{
    funput_suggestion_engine_free, funput_suggestion_engine_new_in_memory, funput_suggestion_learn,
    funput_suggestion_query,
};

const SAMPLES: usize = 100_000;

fn main() {
    let engine = funput_suggestion_engine_new_in_memory();
    for index in 0..5_000 {
        let word: Vec<u32> = format!("word{index:04}").chars().map(u32::from).collect();
        unsafe {
            funput_suggestion_learn(engine, word.as_ptr(), word.len());
            funput_suggestion_learn(engine, word.as_ptr(), word.len());
        }
    }
    let prefix: Vec<u32> = "word1".chars().map(u32::from).collect();
    let mut samples = Vec::with_capacity(SAMPLES);
    let started = Instant::now();
    for _ in 0..SAMPLES {
        let sample = Instant::now();
        std::hint::black_box(unsafe {
            funput_suggestion_query(engine, prefix.as_ptr(), prefix.len())
        });
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
    unsafe { funput_suggestion_engine_free(engine) };
}
