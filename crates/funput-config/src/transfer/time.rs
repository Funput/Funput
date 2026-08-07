//! A UTC ISO-8601 timestamp without a date crate.

use std::time::{SystemTime, UNIX_EPOCH};

/// `yyyy-MM-ddTHH:mm:ssZ` for the document's `exportedAt`.
///
/// Days → civil date is Howard Hinnant's algorithm; pulling in `chrono` for one
/// formatted string is not worth the dependency in a keyboard app.
pub fn iso8601_now() -> String {
    let secs = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
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
mod tests {
    use super::*;

    #[test]
    fn formats_a_fixed_width_utc_timestamp() {
        let stamp = iso8601_now();
        assert_eq!(stamp.len(), 20, "{stamp}");
        assert!(stamp.ends_with('Z'), "{stamp}");
        assert_eq!(stamp.as_bytes()[10], b'T', "{stamp}");
        // Sanity: this code cannot have run before it was written.
        assert!(stamp.as_str() > "2026-01-01T00:00:00Z", "{stamp}");
    }
}
