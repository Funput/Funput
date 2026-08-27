//! Getting the bytes — from a file, or from standard input.
//!
//! Only the fetching. Working out what characters those bytes hold and what charset
//! spells them is [`funput_core::charset::document`]'s, and deliberately not this
//! module's: the Windows and Linux shells ask the same question and must get the same
//! answer, and this crate is the wrong place for logic they would have to copy.

use std::io::Read;
use std::path::Path;

use funput_core::charset::document::{self, Document};

use crate::cli::CliError;

/// Read `path`, or standard input when it is `None`, and say what it holds.
pub(super) fn read(path: Option<&Path>) -> Result<Document, CliError> {
    let bytes = match path {
        Some(path) => std::fs::read(path)
            .map_err(|e| CliError::Msg(format!("cannot read {}: {e}", path.display())))?,
        None => {
            let mut buffer = Vec::new();
            std::io::stdin().read_to_end(&mut buffer)?;
            buffer
        }
    };
    document::read(bytes).map_err(|e| CliError::Msg(e.to_string()))
}
