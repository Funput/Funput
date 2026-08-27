//! The three things a button does, as opposed to everything the window shows.
//!
//! Each one asks [`Session::conversion`] the same question the panes ask, so what is
//! copied, what is saved and what is on screen cannot disagree about which document
//! is being converted. That rule used to exist in three places in the Windows shell
//! and was wrong in one of them.

use funput_core::charset;

use super::Session;

impl Session {
    /// The whole converted document, for the clipboard — **not** the capped preview,
    /// which would hand over a document with its tail quietly missing.
    pub fn result_text(&self) -> Option<String> {
        self.conversion()
            .map(|(text, from, to)| charset::render(&charset::read(text, from), to).text)
    }

    /// The bytes to save. A legacy target stores one byte per letter, and writing the
    /// same characters as UTF-8 would produce a file `.VnTime` cannot read back.
    pub fn save_bytes(&self) -> Option<Vec<u8>> {
        self.conversion()
            .map(|(text, from, to)| charset::render(&charset::read(text, from), to).bytes)
    }
}
