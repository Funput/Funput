//! Fetching the update feed and the new executable.
//!
//! The manifest lives at a fixed `releases/latest/download/…` URL — a redirect to
//! the asset rather than the REST API, so there is no rate limit and no token.

use std::io::Read;
use std::time::Duration;

use super::{Error, Manifest, Result, FEED_URL, MAX_DOWNLOAD_BYTES};

/// end-to-end flow can be tested against a local manifest before tagging.
fn feed_url() -> String {
    #[cfg(debug_assertions)]
    if let Ok(url) = std::env::var("FUNPUT_UPDATE_FEED") {
        return url;
    }
    FEED_URL.to_string()
}

/// A shared HTTP agent with sane timeouts. Built per call (cheap) so the update
/// thread owns it and nothing lingers on the main app.
fn agent() -> ureq::Agent {
    ureq::Agent::config_builder()
        .timeout_connect(Some(Duration::from_secs(15)))
        .timeout_recv_response(Some(Duration::from_secs(60)))
        .build()
        .into()
}

/// Fetch and parse the update manifest from the GitHub Release feed.
pub fn fetch_manifest() -> Result<Manifest> {
    let mut resp = agent()
        .get(&feed_url())
        .call()
        .map_err(|e| Error::Network(e.to_string()))?;
    // `read_to_string` caps the body at ureq's default 10MB — ample for a small
    // JSON manifest and a guard against a runaway feed.
    let body = resp
        .body_mut()
        .read_to_string()
        .map_err(|e| Error::Network(e.to_string()))?;
    serde_json::from_str(&body).map_err(|e| Error::BadManifest(e.to_string()))
}

/// Download the `.exe` bytes, enforcing the manifest's expected length.
pub fn download(url: &str, expected_len: u64) -> Result<Vec<u8>> {
    let resp = agent()
        .get(url)
        .call()
        .map_err(|e| Error::Network(e.to_string()))?;

    let mut bytes = Vec::with_capacity(expected_len.min(MAX_DOWNLOAD_BYTES) as usize);
    resp.into_body()
        .into_reader()
        .take(MAX_DOWNLOAD_BYTES)
        .read_to_end(&mut bytes)
        .map_err(|e| Error::Network(e.to_string()))?;

    let actual = bytes.len() as u64;
    if actual != expected_len {
        return Err(Error::SizeMismatch {
            expected: expected_len,
            actual,
        });
    }
    Ok(bytes)
}
