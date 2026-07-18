mod document;
mod mapping;
mod transfer;

use std::fs;
use std::path::Path;
use std::time::{SystemTime, UNIX_EPOCH};

use crate::settings::Settings;

pub use document::ImportSummary;
use document::{ConfigDocument, SCHEMA_ID};
use transfer::{apply, to_document};

#[derive(Debug)]
pub enum ConfigError {
    Unreadable,
    Unrecognized,
}

impl std::fmt::Display for ConfigError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(match self {
            Self::Unreadable => "Không đọc được tệp cấu hình (định dạng sai hoặc tệp hỏng).",
            Self::Unrecognized => "Tệp này không phải cấu hình Funput hợp lệ.",
        })
    }
}

impl std::error::Error for ConfigError {}

pub fn export_to(path: &Path, settings: &Settings) -> std::io::Result<()> {
    let json =
        serde_json::to_string_pretty(&to_document(settings)).map_err(std::io::Error::other)?;
    fs::write(path, json)
}

pub fn import_file(path: &Path, settings: &mut Settings) -> Result<ImportSummary, ConfigError> {
    let text = fs::read_to_string(path).map_err(|_| ConfigError::Unreadable)?;
    let doc: ConfigDocument = serde_json::from_str(&text).map_err(|_| ConfigError::Unreadable)?;
    if doc.schema != SCHEMA_ID {
        return Err(ConfigError::Unrecognized);
    }
    Ok(apply(settings, &doc))
}

pub fn today_stamp() -> String {
    iso8601_now()[..10].to_string()
}

fn iso8601_now() -> String {
    let secs = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|duration| duration.as_secs())
        .unwrap_or(0) as i64;
    let days = secs.div_euclid(86_400);
    let tod = secs.rem_euclid(86_400);
    let (hour, min, sec) = (tod / 3600, (tod % 3600) / 60, tod % 60);
    let z = days + 719_468;
    let era = if z >= 0 { z } else { z - 146_096 } / 146_097;
    let doe = z - era * 146_097;
    let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365;
    let year = yoe + era * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let day = doy - (153 * mp + 2) / 5 + 1;
    let month = if mp < 10 { mp + 3 } else { mp - 9 };
    let year = year + i64::from(month <= 2);
    format!("{year:04}-{month:02}-{day:02}T{hour:02}:{min:02}:{sec:02}Z")
}

#[cfg(test)]
mod tests;
