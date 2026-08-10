//! Tracks a press-and-release gesture of the bare modifier keys.
//!
//! A hotkey made only of modifiers cannot fire on keydown: Ctrl+Shift would trip
//! the instant Shift goes down, stealing it from Ctrl+Shift+T. Windows' own
//! input-language switcher instead waits for a modifier to come back up, and
//! abandons the gesture if any other key was pressed while they were held. This
//! reproduces that rule, so Alt+Shift+Tab still reaches the focused app.
//!
//! It also reproduces the repeat: keeping Ctrl down and tapping Shift over and
//! over toggles once per tap, the way holding Alt and tapping Shift cycles
//! Windows' input languages. A gesture that was *interrupted* does not repeat —
//! releasing and re-pressing Shift in the middle of a Ctrl+Shift+arrow selection
//! is still that selection, not a hotkey.

use funput_desktop::Mods;

/// The only three shapes of key event this cares about.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Event {
    /// A modifier key going down or up, carrying *which* modifier it is.
    Modifier { bit: Mods, down: bool },
    /// Any non-modifier keydown — it turns the gesture into a real shortcut.
    OtherDown,
}

#[derive(Debug, Default)]
pub struct Watcher {
    /// The modifiers still down, counted from the hook's own events.
    ///
    /// Deliberately *not* read back from `GetAsyncKeyState`: that snapshot lags
    /// the hook by an event (see [`super::rules::mods_with`]), so releasing
    /// Ctrl+Shift together could hand the second keyup a snapshot in which the
    /// first key is still down. The gesture then never saw "everything is up",
    /// stayed `spent`, and every later press was swallowed — the toggle worked
    /// once or twice and then went dead until Funput was restarted.
    down: Mods,
    /// Every modifier seen down since the gesture began. The live state at the
    /// moment of a release is already missing the key being released, so the
    /// gesture has to be remembered rather than read back.
    peak: Mods,
    /// Why the gesture can no longer fire, while it cannot.
    spent: Option<Spent>,
}

/// What ended the gesture. The two are not interchangeable: one re-arms while the
/// remaining modifiers stay down, the other does not.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum Spent {
    Fired,
    /// A non-modifier key or a mouse action turned it into a real shortcut.
    Interrupted,
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
            Event::Modifier { bit, down: true } => {
                let others = without(held, bit);
                // A fresh gesture: either we saw everything come up, or the OS
                // says nothing else is down. The second test is the recovery
                // path — a keyup the hook never got (focus stolen by an elevated
                // window, a locked session) would otherwise leave `down` stuck
                // forever, and with it the gesture.
                if !any(self.down) || !any(others) {
                    *self = Self::default();
                    // Modifiers already held when this gesture began still count
                    // towards it, so Ctrl+Shift is rejected as Ctrl+Alt+Shift.
                    self.down = others;
                    self.peak = others;
                } else if self.spent == Some(Spent::Fired) {
                    // Ctrl is still down and Shift has just been pressed again:
                    // that is the next toggle, not a leftover of the last one.
                    // The new gesture is whatever is held right now, so a tap
                    // that adds a third modifier is still judged on its own.
                    self.spent = None;
                    self.peak = self.down;
                }
                self.down = union(self.down, bit);
                self.peak = union(self.peak, bit);
                None
            }
            Event::Modifier { bit, down: false } => {
                self.down = without(self.down, bit);
                let fired = (self.spent.is_none() && any(self.peak)).then_some(self.peak);
                // A gesture fires once, on the first release; the remaining
                // modifiers coming up must not repeat it.
                self.spent = Some(match fired {
                    Some(_) => Spent::Fired,
                    // Not `Fired`: an interrupted gesture stays interrupted, so
                    // re-pressing a modifier inside a shortcut cannot revive it.
                    None => self.spent.unwrap_or(Spent::Interrupted),
                });
                if !any(self.down) {
                    *self = Self::default();
                }
                fired
            }
        }
    }

    /// Something other than a modifier key happened while they were held, so the
    /// user was performing a shortcut rather than tapping a bare pair.
    pub fn interrupt(&mut self) {
        self.spent = Some(Spent::Interrupted);
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

/// `a` with everything set in `b` cleared.
fn without(a: Mods, b: Mods) -> Mods {
    Mods {
        ctrl: a.ctrl && !b.ctrl,
        alt: a.alt && !b.alt,
        win: a.win && !b.win,
        shift: a.shift && !b.shift,
    }
}

#[cfg(test)]
mod tests;
