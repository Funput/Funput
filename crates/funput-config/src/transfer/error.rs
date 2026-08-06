//! What an import reports back to the user.

/// What an import changed, for the confirmation shown to the user.
#[derive(Default)]
pub struct ImportSummary {
    pub shortcuts_added: usize,
    pub shortcuts_updated: usize,
    pub applied_platform: bool,
    /// The file was written by a newer Funput. It still imported, but fields this
    /// build does not know were dropped, so the user is told.
    pub newer_version: bool,
}

/// A user-facing failure while importing a config file.
#[derive(Debug)]
pub enum ConfigError {
    Unreadable,   // not JSON / decode failed
    Unrecognized, // decoded, but not a Funput config
}

impl std::fmt::Display for ConfigError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(match self {
            ConfigError::Unreadable => "Không đọc được tệp cấu hình (định dạng sai hoặc tệp hỏng).",
            ConfigError::Unrecognized => "Tệp này không phải cấu hình Funput hợp lệ.",
        })
    }
}

impl std::error::Error for ConfigError {}
