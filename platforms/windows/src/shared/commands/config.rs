//! Export and import of the shared config document, and importing a UniKey
//! gõ tắt table.

use std::path::Path;

use funput_config::transfer::{
    self, ConfigDocument, ConfigError, ImportSummary, Source, SCHEMA_ID,
};
use funput_config::unikey::{self, MacroError};
use funput_core::charset::Charset;

use crate::shared::shell;

/// Write the current settings to `path` as an interchange file.
///
/// `Source` is filled in here, not by `funput-config`: `CARGO_PKG_VERSION` has to
/// be *this* crate's version (the shipped app's), not the config crate's.
pub fn export_config(path: &Path) -> std::io::Result<()> {
    let source = Source {
        platform: "windows".to_string(),
        app_version: env!("CARGO_PKG_VERSION").to_string(),
    };
    transfer::export_to(path, &shell::snapshot(), source)
}

/// Merge a config file into the live settings (applies to the engine + persists).
pub fn import_config(path: &Path) -> Result<ImportSummary, ConfigError> {
    let mut settings = shell::snapshot();
    let summary = transfer::import_file(path, &mut settings)?;
    shell::replace_settings(settings);
    Ok(summary)
}

/// Merge a UniKey `ukmacro.txt` into the live gõ tắt table.
///
/// The parsed rows are wrapped in a document carrying nothing else, so
/// [`transfer::apply`] does the merging — same rule as importing a Funput config
/// (a repeated trigger takes the incoming expansion, rows absent from the file
/// survive) and the same counters back. Restating that here would be a second copy
/// of a rule that has to stay identical in both places.
/// The charset comes back with the summary because reading a legacy table means
/// *guessing* which one it was, and a guess the user cannot see is one they cannot
/// check. `None` means nothing had to be guessed.
pub fn import_unikey_macros(path: &Path) -> Result<(ImportSummary, Option<Charset>), MacroError> {
    let import = unikey::read_macro_file(path)?;
    let document = ConfigDocument {
        schema: SCHEMA_ID.to_string(),
        version: transfer::CURRENT_VERSION,
        exported_at: None,
        source: None,
        preferences: None,
        shortcuts: Some(import.rows),
        platform: None,
    };
    let mut settings = shell::snapshot();
    let summary = transfer::apply(&mut settings, &document);
    shell::replace_settings(settings);
    Ok((summary, import.charset))
}
