use crate::compose::{diff, flip};
use crate::{Engine, ImeResult};

impl Engine {
    /// Synchronize engine state after a Backspace passed through to the host app.
    pub fn on_backspace(&mut self) -> ImeResult {
        if !self.session.enabled {
            return ImeResult::none();
        }
        self.session.buffer.pop();
        self.session.keys = self.session.buffer.clone();
        ImeResult::none()
    }

    /// Flip the live word between its Vietnamese and raw-keystroke forms.
    pub fn flip_composing(&mut self) -> ImeResult {
        match flip::flip(
            &self.session.buffer,
            &self.session.keys,
            &self.session.vn_form,
        ) {
            Some((new_buffer, override_)) => {
                let (backspace, output) = diff::diff(&self.session.buffer, &new_buffer);
                self.session.buffer = new_buffer;
                self.session.restore_override = Some(override_);
                ImeResult::send(backspace, output)
            }
            None => ImeResult::none(),
        }
    }
}
