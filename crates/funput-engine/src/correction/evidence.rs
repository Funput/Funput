//! What is known about how the word was typed, as the generators read it.
//!
//! A generator never looks at the touch log itself. It asks this view which keys a
//! given key could have been instead, and what each of them costs. Where the host
//! reported where the finger landed, the answer is the host's own alternates priced
//! by the touch model; where it did not — a key without a report, a key no finger
//! pressed — the answer is the static [`layout`](crate::correction::layout) at a
//! flat cost. The generators cannot tell the two apart, which is the point.

use funput_core::InputMethod;

use crate::correction::layout;
use crate::correction::score::substitution_cost;
use crate::correction::touch::{StoredTouch, TouchLog, from_q8};

/// The most alternatives one key can have. A key on a grid has at most six keys
/// within reach in one or two steps, and a host reports fewer than that.
pub(crate) const MAX_NEIGHBOURS: usize = 6;

/// The keys one key could have been, each with what the substitution costs in
/// nats. A fixed array, so asking never allocates.
#[derive(Debug, Clone, Copy)]
pub(crate) struct Neighbours {
    keys: [(char, f32); MAX_NEIGHBOURS],
    len: usize,
}

impl Neighbours {
    pub(crate) const NONE: Self = Self {
        keys: [('\0', 0.0); MAX_NEIGHBOURS],
        len: 0,
    };

    pub(crate) fn push(&mut self, key: char, cost: f32) {
        if self.len < MAX_NEIGHBOURS {
            self.keys[self.len] = (key, cost);
            self.len += 1;
        }
    }

    pub(crate) fn iter(&self) -> impl Iterator<Item = (char, f32)> + '_ {
        self.keys[..self.len].iter().copied()
    }
}

/// The evidence behind a word: the touch log when it still describes the raw keys,
/// and the method, which decides which keys are modifiers and whether digits are
/// keys at all.
///
/// Touch evidence is indexed by the *typed* key, not by a position in a variant: a
/// variant that moved or dropped keys names, per position, the typed key whose
/// evidence it inherits (see `generate::Variant::origin`).
pub(crate) struct Evidence<'a> {
    touch: Option<&'a [StoredTouch]>,
    method: InputMethod,
}

impl<'a> Evidence<'a> {
    /// The log is used only when it lines up with `keys` key for key; a log the
    /// engine stopped trusting (a reverted modifier, an adopted word) counts as no
    /// touch at all, so the whole word falls back to the layout rather than half of it.
    pub(crate) fn new(log: &'a TouchLog, keys: &str, method: InputMethod) -> Self {
        Self {
            touch: log.matches(keys).then(|| log.entries()),
            method,
        }
    }

    /// No touch evidence at all: the layout speaks for every key. The seam for a
    /// caller that never had a touch log — a glide decoder feeding `find_with`.
    #[cfg_attr(not(test), allow(dead_code))]
    pub(crate) fn none(method: InputMethod) -> Self {
        Self {
            touch: None,
            method,
        }
    }

    pub(crate) fn method(&self) -> InputMethod {
        self.method
    }

    /// Whether every key of the word carries a touch report — the condition for the
    /// word as typed to stand as a rival to the candidates. A finger square on the
    /// keys it hit is evidence for the typed word; a key nobody saw land is not.
    pub(crate) fn has_full_touch(&self) -> bool {
        self.touch
            .is_some_and(|touch| !touch.is_empty() && touch.iter().all(|entry| entry.reported))
    }

    /// The keys `key` could have been, nearest first. With a touch reported for the
    /// typed key `origin`, the host's own alternates at what the touch model says
    /// each costs; otherwise the layout's neighbours of `key` at the flat cost.
    pub(crate) fn neighbours(&self, origin: Option<u8>, key: char) -> Neighbours {
        let reported = origin
            .and_then(|index| self.touch?.get(usize::from(index)))
            .filter(|entry| entry.reported);
        let Some(entry) = reported else {
            return layout::neighbours(key, self.method == InputMethod::Vni);
        };
        let mut neighbours = Neighbours::NONE;
        let typed = from_q8(entry.typed_distance);
        for alternate in entry.alternates() {
            let cost = substitution_cost(typed, from_q8(alternate.distance));
            neighbours.push(alternate.key, cost);
        }
        neighbours
    }
}
