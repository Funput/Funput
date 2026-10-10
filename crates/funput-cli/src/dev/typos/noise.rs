//! A keyboard to miss: where the keys are, and a finger that lands near one.
//!
//! The geometry is a plain QWERTY grid measured in **key pitches** — the gap between
//! two neighbouring key centres — which is the same unit the engine scores touches
//! in, so nothing here has to know about screen sizes. Rows are offset the way a real
//! keyboard staggers them, because that stagger is what makes `g`/`h` a likelier slip
//! than `g`/`y`.

use funput_engine::KeyTouch;

/// Letter rows, and the digit row VNI needs for its tone keys.
const ROWS: [(&str, f32, f32); 4] = [
    ("1234567890", 0.0, 0.0),
    ("qwertyuiop", 0.0, 1.0),
    ("asdfghjkl", 0.25, 2.0),
    ("zxcvbnm", 0.75, 3.0),
];

/// How many neighbours a touch reports, matching what the engine accepts.
const ALTERNATES: usize = 3;
/// Past this a neighbour is not a plausible reading of the same touch, and offering
/// it only costs the search time. A real keyboard should prune the same way.
const PLAUSIBLE: f32 = 1.2;

fn centre(key: char) -> Option<(f32, f32)> {
    let key = key.to_ascii_lowercase();
    ROWS.iter().find_map(|(row, offset, y)| {
        let index = row.find(key)?;
        Some((index as f32 + offset, *y))
    })
}

fn distance(from: (f32, f32), to: (f32, f32)) -> f32 {
    ((from.0 - to.0).powi(2) + (from.1 - to.1).powi(2)).sqrt()
}

/// Every key, sorted by how far its centre is from `point`.
fn by_distance(point: (f32, f32)) -> Vec<(char, f32)> {
    let mut keys: Vec<(char, f32)> = ROWS
        .iter()
        .flat_map(|(row, offset, y)| {
            row.chars()
                .enumerate()
                .map(move |(index, key)| (key, distance(point, (index as f32 + offset, *y))))
        })
        .collect();
    keys.sort_by(|left, right| left.1.total_cmp(&right.1));
    keys
}

/// A finger aiming at `key` and landing `noise` pitches off, on average.
///
/// `noise` is how far the finger really drifts, which is **not** the σ the engine
/// scores with: that one is its belief about the spread, and the two are free to
/// differ. Measuring them apart is the only way to see what happens when the belief
/// is wrong.
///
/// Returns the key it actually hit — which is the whole point: when the nearest
/// centre is not the one aimed at, the user has just made the mistake this feature
/// exists to repair. A key that is not on the grid (a space, a bracket) is reported
/// as hit exactly, with no neighbours.
pub(super) fn aim(key: char, noise: f32, rng: &mut Rng) -> (char, KeyTouch) {
    let Some(centre) = centre(key) else {
        return (key, KeyTouch::new(key, 0.0));
    };
    let point = (
        centre.0 + noise * rng.gaussian(),
        centre.1 + noise * rng.gaussian(),
    );
    let ranked = by_distance(point);
    let (hit, hit_distance) = ranked[0];
    let hit = match_case(key, hit);
    let mut touch = KeyTouch::new(hit, hit_distance);
    for &(alternate, distance) in ranked[1..=ALTERNATES].iter() {
        if distance <= PLAUSIBLE {
            touch = touch.with_alternate(match_case(key, alternate), distance);
        }
    }
    (hit, touch)
}

fn match_case(aimed: char, hit: char) -> char {
    if aimed.is_uppercase() {
        hit.to_ascii_uppercase()
    } else {
        hit
    }
}

/// xorshift64*, so a run is reproducible from its seed. The workspace carries no
/// random-number dependency and this is not cryptography.
pub(super) struct Rng {
    state: u64,
}

impl Rng {
    pub(super) fn new(seed: u64) -> Self {
        Self {
            state: seed | 1, // a zero state would only ever produce zero
        }
    }

    fn next_u64(&mut self) -> u64 {
        self.state ^= self.state >> 12;
        self.state ^= self.state << 25;
        self.state ^= self.state >> 27;
        self.state.wrapping_mul(0x2545_F491_4F6C_DD1D)
    }

    /// A uniform sample in (0, 1]; never zero, which `ln` below could not take.
    fn uniform(&mut self) -> f32 {
        ((self.next_u64() >> 40) as f32 + 1.0) / 16_777_216.0
    }

    /// One standard normal sample, Box–Muller.
    pub(super) fn gaussian(&mut self) -> f32 {
        let (u1, u2) = (self.uniform(), self.uniform());
        (-2.0 * u1.ln()).sqrt() * (std::f32::consts::TAU * u2).cos()
    }
}
