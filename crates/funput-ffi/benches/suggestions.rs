use std::hint::black_box;

use criterion::{Criterion, Throughput, criterion_group, criterion_main};
use funput_ffi::{
    funput_suggestion_engine_free, funput_suggestion_engine_new_in_memory, funput_suggestion_learn,
    funput_suggestion_query,
};

fn bench(c: &mut Criterion) {
    let engine = funput_suggestion_engine_new_in_memory();
    for index in 0..5_000 {
        let word: Vec<u32> = format!("word{index:04}").chars().map(u32::from).collect();
        unsafe {
            funput_suggestion_learn(engine, word.as_ptr(), word.len());
            funput_suggestion_learn(engine, word.as_ptr(), word.len());
        }
    }
    let prefix: Vec<u32> = "word1".chars().map(u32::from).collect();
    let mut group = c.benchmark_group("suggestions_ffi");
    group.throughput(Throughput::Elements(1));
    group.bench_function("query_top3", |b| {
        b.iter(|| {
            black_box(unsafe {
                funput_suggestion_query(engine, black_box(prefix.as_ptr()), prefix.len())
            })
        })
    });
    group.finish();
    unsafe { funput_suggestion_engine_free(engine) };
}

criterion_group!(benches, bench);
criterion_main!(benches);
