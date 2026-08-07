//! Export and import of the shared config document.

use std::path::Path;

use funput_config::transfer::{self, ConfigError, ImportSummary, Source};

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
