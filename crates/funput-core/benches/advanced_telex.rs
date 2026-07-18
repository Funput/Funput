use std::hint::black_box;

use criterion::{BenchmarkId, Criterion, Throughput, criterion_group, criterion_main};
use funput_core::{InputMethod, ToneStyle, apply_checked};

const CANONICAL: &str = "uw uwf tuw mow truwowngf nguwowif uwngf";
const FULL: &str = "w wf t[ m] tr[]ngf ng[]if wngf";

fn run(input: &str, method: InputMethod) -> String {
    let mut buffer = String::new();
    for key in black_box(input).chars() {
        if key == ' ' {
            buffer.clear();
        } else {
            buffer = apply_checked(&buffer, key, method, ToneStyle::Traditional, false).text;
        }
    }
    buffer
}

fn bench(c: &mut Criterion) {
    let mut group = c.benchmark_group("advanced_telex");
    for (name, input, method) in [
        ("canonical", CANONICAL, InputMethod::Telex),
        ("full", FULL, InputMethod::TelexAdvanced),
    ] {
        group.throughput(Throughput::Elements(input.chars().count() as u64));
        group.bench_function(BenchmarkId::new("core", name), |b| {
            b.iter(|| run(input, method))
        });
    }
    group.finish();
}

criterion_group!(benches, bench);
criterion_main!(benches);
