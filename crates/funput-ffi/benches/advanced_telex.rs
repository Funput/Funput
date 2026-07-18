use std::hint::black_box;

use criterion::{BenchmarkId, Criterion, Throughput, criterion_group, criterion_main};
use funput_ffi::{
    CHARS_CAP, FunputEngine, funput_buffer, funput_clear, funput_engine_free, funput_engine_new,
    funput_process_char, funput_set_method,
};

const CANONICAL: &str = "uw uwf tuw mow truwowngf nguwowif uwngf.";
const FULL: &str = "w wf t[ m] tr[]ngf ng[]if wngf.";

fn run(engine: *mut FunputEngine, input: &str, out: &mut [u32; CHARS_CAP]) {
    unsafe { funput_clear(engine) };
    for key in black_box(input).chars() {
        let result = unsafe { funput_process_char(engine, key as u32) };
        let count = unsafe { funput_buffer(engine, out.as_mut_ptr(), CHARS_CAP) };
        black_box((result.action, result.count, count));
    }
}

fn bench(c: &mut Criterion) {
    let mut group = c.benchmark_group("advanced_telex");
    for (name, input, method) in [("canonical", CANONICAL, 0u8), ("full", FULL, 2u8)] {
        let engine = funput_engine_new();
        unsafe { funput_set_method(engine, method) };
        let mut out = [0; CHARS_CAP];
        group.throughput(Throughput::Elements(input.chars().count() as u64));
        group.bench_function(BenchmarkId::new("ffi", name), |b| {
            b.iter(|| run(engine, input, &mut out))
        });
        unsafe { funput_engine_free(engine) };
    }
    group.finish();
}

criterion_group!(benches, bench);
criterion_main!(benches);
