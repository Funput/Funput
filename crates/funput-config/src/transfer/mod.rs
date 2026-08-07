//! Cross-platform config interchange (Export/Import) — see
//! `platforms/CONFIG_FORMAT.md` for the format this implements.
//!
//! `preferences` and `shortcuts` are portable; anything tied to one OS lives under
//! `platform.<name>` and is applied only there. A file exported anywhere therefore
//! imports its portable half everywhere.

mod apply;
mod document;
mod error;
mod time;

use std::fs;
use std::path::Path;

use crate::settings::Settings;

pub use apply::{apply, to_document};
pub use document::{
    CURRENT_VERSION, ConfigDocument, Platform, PortableShortcut, Preferences, SCHEMA_ID, Source,
    WindowsBlock,
};
pub use error::{ConfigError, ImportSummary};
pub use time::iso8601_now;

/// Write the current settings as a pretty-printed interchange file.
pub fn export_to(path: &Path, s: &Settings, source: Source) -> std::io::Result<()> {
    let json =
        serde_json::to_string_pretty(&to_document(s, source)).map_err(std::io::Error::other)?;
    fs::write(path, json)
}

/// Read and merge a config file into `s`. `Err` on a bad or foreign file.
pub fn import_file(path: &Path, s: &mut Settings) -> Result<ImportSummary, ConfigError> {
    let text = fs::read_to_string(path).map_err(|_| ConfigError::Unreadable)?;
    let doc: ConfigDocument = serde_json::from_str(&text).map_err(|_| ConfigError::Unreadable)?;
    if doc.schema != SCHEMA_ID {
        return Err(ConfigError::Unrecognized);
    }
    Ok(apply(s, &doc))
}

/// `yyyy-MM-dd` for the default export file name.
pub fn today_stamp() -> String {
    iso8601_now()[..10].to_string()
}

#[cfg(test)]
mod tests;
