//! Tracks a press-and-release gesture of the bare modifier keys.
//!
//! A hotkey made only of modifiers cannot fire on keydown: Ctrl+Shift would trip
//! the instant Shift goes down, stealing it from Ctrl+Shift+T. Windows' own
//! input-language switcher instead waits for a modifier to come back up, and
//! abandons the gesture if any other key was pressed while they were held. This
//! reproduces that rule, so Alt+Shift+Tab still reaches the focused app.

use funput_desktop::Mods;

/// The only three shapes of key event this cares about.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Event {
    ModifierDown,
    ModifierUp,
    /// Any non-modifier keydown — it turns the gesture into a real shortcut.
    OtherDown,
}

#[derive(Debug, Default)]
pub struct Watcher {
    /// Every modifier seen down since the gesture began. The live state at the
    /// moment of a release is already missing the key being released, so the
    /// gesture has to be remembered rather than read back.
    peak: Mods,
    /// The gesture can no longer fire: another key intervened, or it fired.
    spent: bool,
}

impl Watcher {
    /// Feed one event, `held` being the modifier state *including* this event.
    /// Returns the modifier set that just completed as a bare-modifier gesture;
    /// the caller decides whether that set is a hotkey it cares about.
    pub fn feed(&mut self, event: Event, held: Mods) -> Option<Mods> {
        match event {
            Event::OtherDown => {
                self.interrupt();
                None
            }
            Event::ModifierDown => {
                if !any(self.peak) {
                    self.spent = false; // a fresh gesture starts clean
                }
                self.peak = union(self.peak, held);
                None
            }
            Event::ModifierUp => {
                let fired = (!self.spent && any(self.peak)).then_some(self.peak);
                // A gesture fires once, on the first release; the remaining
                // modifiers coming up must not repeat it.
                self.spent = true;
                if !any(held) {
                    *self = Self::default();
                }
                fired
            }
        }
    }

    /// Something other than a modifier key happened while they were held, so the
    /// user was performing a shortcut rather than tapping a bare pair.
    pub fn interrupt(&mut self) {
        self.spent = true;
    }
}

fn any(m: Mods) -> bool {
    m.ctrl || m.alt || m.shift || m.win
}

fn union(a: Mods, b: Mods) -> Mods {
    Mods {
        ctrl: a.ctrl || b.ctrl,
        alt: a.alt || b.alt,
        win: a.win || b.win,
        shift: a.shift || b.shift,
    }
}

#[cfg(test)]
mod tests;
