//! One-off profiling harness for the keystroke pipeline: counts heap allocations
//! per keystroke and times `process_char` against its sub-parts, to see where the
//! ~1.5 µs goes before optimizing. Not a test/bench — run it directly:
//!
//!   cargo run --release --example profile -p funput-engine

use std::alloc::{GlobalAlloc, Layout, System};
use std::hint::black_box;
use std::sync::atomic::{AtomicUsize, Ordering::Relaxed};
use std::time::Instant;

use funput_core::{InputMethod, ToneStyle, apply_checked, is_definitely_invalid};
use funput_engine::Engine;

// --- counting global allocator (alloc + realloc count as heap ops) ----------
struct Counting;
static ALLOCS: AtomicUsize = AtomicUsize::new(0);
unsafe impl GlobalAlloc for Counting {
    unsafe fn alloc(&self, l: Layout) -> *mut u8 {
        ALLOCS.fetch_add(1, Relaxed);
        unsafe { System.alloc(l) }
    }
    unsafe fn dealloc(&self, p: *mut u8, l: Layout) {
        unsafe { System.dealloc(p, l) }
    }
    unsafe fn realloc(&self, p: *mut u8, l: Layout, n: usize) -> *mut u8 {
        ALLOCS.fetch_add(1, Relaxed);
        unsafe { System.realloc(p, l, n) }
    }
}
#[global_allocator]
static GLOBAL: Counting = Counting;

const TELEX: &str =
    "Tooi yeeu tieesng Vieejt. Hoom nay troiwf nuwowcs ddepj. Ban cos khoeer khoong?";

fn allocs() -> usize {
    ALLOCS.load(Relaxed)
}
fn is_boundary(k: char) -> bool {
    k == ' ' || k.is_ascii_punctuation()
}

/// Median-of-a-few wall-clock timing (ns) for one call to `f`.
fn time(reps: usize, mut f: impl FnMut()) -> f64 {
    for _ in 0..reps / 10 {
        f();
    } // warm up
    let t = Instant::now();
    for _ in 0..reps {
        f();
    }
    t.elapsed().as_nanos() as f64 / reps as f64
}

fn main() {
    let keys: Vec<char> = TELEX.chars().collect();
    let n = keys.len();
    let letters = keys.iter().filter(|k| !is_boundary(**k)).count();
    let reps = 4000usize;

    // --- allocations: process_char allocs = (construct + type) − construct ---
    let b = allocs();
    for _ in 0..reps {
        let mut e = Engine::new();
        e.set_method(InputMethod::Telex);
        black_box(&e);
    }
    let ctor = allocs() - b;

    let b = allocs();
    for _ in 0..reps {
        let mut e = Engine::new();
        e.set_method(InputMethod::Telex);
        for &k in &keys {
            black_box(e.process_char(black_box(k)));
        }
    }
    let proc = (allocs() - b) - ctor;

    println!("=== allocations (heap ops) ===");
    println!(
        "Engine::new()+set_method : {:.2} / engine",
        ctor as f64 / reps as f64
    );
    println!(
        "process_char             : {:.3} allocs/key   ({} keys, {} composing)",
        proc as f64 / (reps * n) as f64,
        n,
        letters
    );

    // --- timing (ns / keystroke) --------------------------------------------
    let t = 30_000usize;

    let paragraph_ns = time(t, || {
        let mut e = Engine::new();
        e.set_method(InputMethod::Telex);
        for &k in &keys {
            black_box(e.process_char(black_box(k)));
        }
    }) / n as f64;

    let compose_ns = time(t, || {
        let mut e = Engine::new();
        e.set_method(InputMethod::Telex);
        for &k in &keys {
            if is_boundary(k) {
                e.clear();
            } else {
                black_box(e.process_char(black_box(k)));
            }
        }
    }) / letters as f64;

    let apply_ns = time(t, || {
        let mut buf = String::new();
        for &k in &keys {
            if is_boundary(k) {
                buf.clear();
            } else {
                buf =
                    apply_checked(&buf, k, InputMethod::Telex, ToneStyle::Traditional, false).text;
            }
        }
        black_box(&buf);
    }) / letters as f64;

    let composed = [
        "tôi", "yêu", "tiếng", "việt", "đẹp", "nước", "trời", "không",
    ];
    let invalid_ns = time(t, || {
        for b in composed {
            black_box(is_definitely_invalid(black_box(b)));
        }
    }) / composed.len() as f64;

    println!("\n=== timing (ns / keystroke) ===");
    println!("core apply_checked         : {apply_ns:6.1}  (per composing key)");
    println!("is_definitely_invalid      : {invalid_ns:6.1}  (per call; pipeline runs it 1×/key)");
    println!("process_char, compose only : {compose_ns:6.1}  (full pipeline, boundaries excluded)");
    println!("process_char, paragraph    : {paragraph_ns:6.1}  (avg incl. boundary keys)");
    println!(
        "\npipeline overhead / compose key = {:.1} ns  (compose_only − apply_checked)",
        compose_ns - apply_ns
    );
}
