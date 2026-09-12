//! The buttons of the second axis.
//!
//! Commands, not queries: each one changes what the window will produce, and none
//! of them touches the document. What the user pasted stays exactly as they pasted
//! it — see [`crate::casing::Casing`] for why that matters — and the list of
//! presses is replayed on every [`Session::refresh`].

use crate::casing;

use super::Session;

impl Session {
    /// A transform button was pressed, by menu position.
    pub fn apply_transform(&mut self, index: usize) {
        self.casing.push(casing::at(index));
    }

    /// Undo, which takes off the last transform rather than all of them: pressing a
    /// third one by mistake should not cost the two before it.
    pub fn undo_transform(&mut self) {
        self.casing.undo();
    }

    /// Back to the document as it arrived.
    pub fn clear_transforms(&mut self) {
        self.casing.clear();
    }

    /// Keep `đ` and `Đ` instead of writing `d` and `D`.
    ///
    /// Named for the switch rather than for the field it sets, so that **off is the
    /// default** — the same way `funput case --keep-d` reads, and the reason
    /// [`crate::View`] can keep deriving `Default`.
    pub fn set_keep_d(&mut self, on: bool) {
        let mut options = self.casing.options();
        options.d_to_ascii = !on;
        self.casing.set_options(options);
    }

    /// Also lowercase words that are already all-caps, so `TP. HCM` becomes
    /// `Tp. Hcm` under title case.
    pub fn set_flatten_caps(&mut self, on: bool) {
        let mut options = self.casing.options();
        options.keep_all_caps = !on;
        self.casing.set_options(options);
    }
}
