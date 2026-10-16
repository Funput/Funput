//! What is known about how the word was typed, as the generators read it.
//!
//! A generator never looks at the touch log itself. It asks this view which keys a
//! given typed key could have been instead, and what each of them costs — so the
//! source of that answer (the host's touch report today, a static layout for a key
//! that arrived without one later) can change without a generator noticing.

use crate::correction::score::substitution_cost;
use crate::correction::touch::{StoredTouch, TouchLog, from_q8};

/// The most alternatives one key can have. A key on a grid has at most six keys
/// within reach in one or two steps, and a host reports fewer than that.
pub(crate) const MAX_NEIGHBOURS: usize = 6;

/// The keys one typed key could have been, each with what the substitution costs
/// in nats. A fixed array, so asking never allocates.
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

    fn push(&mut self, key: char, cost: f32) {
        if self.len < MAX_NEIGHBOURS {
            self.keys[self.len] = (key, cost);
            self.len += 1;
        }
    }

    pub(crate) fn iter(&self) -> impl Iterator<Item = (char, f32)> + '_ {
        self.keys[..self.len].iter().copied()
    }
}

/// The evidence behind a word: which keys were typed, and where the fingers landed.
///
/// Indexed by the *typed* key, not by a position in a variant: a variant that moved
/// or dropped keys names, per position, the typed key whose evidence it inherits
/// (see `generate::Variant::origin`).
pub(crate) struct Evidence<'a> {
    touch: &'a [StoredTouch],
}

impl<'a> Evidence<'a> {
    pub(crate) fn new(log: &'a TouchLog) -> Self {
        Self {
            touch: log.entries(),
        }
    }

    /// Whether every key of the word carries a touch report — the condition for the
    /// word as typed to stand as a rival to the candidates. The gate only lets such
    /// words through today, so this is always true for a word that is searched.
    pub(crate) fn has_full_touch(&self) -> bool {
        !self.touch.is_empty()
    }

    /// The keys the typed key `origin` could have been, nearest first as the host
    /// reported them. Nothing for a position that inherits no typed key.
    pub(crate) fn neighbours(&self, origin: Option<u8>) -> Neighbours {
        let mut neighbours = Neighbours::NONE;
        let Some(entry) = origin.and_then(|index| self.touch.get(usize::from(index))) else {
            return neighbours;
        };
        let typed = from_q8(entry.typed_distance);
        for alternate in entry.alternates() {
            let cost = substitution_cost(typed, from_q8(alternate.distance));
            neighbours.push(alternate.key, cost);
        }
        neighbours
    }
}
