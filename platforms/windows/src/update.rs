//! Auto-update, mirroring the macOS Sparkle setup as closely as Windows allows.
//!
//! The release pipeline publishes a small JSON manifest (`funput-windows.json`)
//! next to the `.exe` on the GitHub Release, signed with the **same Ed25519 key
//! Sparkle uses for macOS**. We fetch the manifest from a fixed
//! `releases/latest/download/…` URL (a redirect to the asset, not the REST API,
//! so there is no rate limit), compare versions, then — if newer — download the
//! new `.exe`, verify its signature against the embedded public key, swap the
//! running executable in place, and relaunch.
//!
//! Checks are manual-only (parity with macOS `SUEnableAutomaticChecks=false`):
//! nothing here runs unless the user clicks "Kiểm tra cập nhật…".
//!
//! Because Funput on Windows is just a tray process (not a system-loaded input
//! method like on macOS), the relaunch needs no log-out — we spawn the new
//! binary and exit. And since our own process writes the new file, it carries no
//! Mark-of-the-Web, so the relaunch does not trip SmartScreen.

use std::io::Read;
use std::time::Duration;

use base64::engine::general_purpose::STANDARD as BASE64;
use base64::Engine;
use ed25519_dalek::{Signature, Verifier, VerifyingKey};
use serde::Deserialize;

/// Ed25519 public key used to verify update signatures. This MUST stay identical
/// to `SUPublicEDKey` in the macOS `Info.plist` — both platforms are signed with
/// the one `SPARKLE_ED_PRIVATE_KEY` secret in CI.
const PUBLIC_ED_KEY: &str = "wDWk569Lmn9WmjPn1ZwHMTp/KW+nfaaNIvtrYSV9nHU=";

/// Fixed update feed. The `latest/download` path always redirects to the asset on
/// the most recent (non-prerelease) GitHub Release.
const FEED_URL: &str = "https://github.com/Funput/Funput/releases/latest/download/funput-windows.json";

/// The version this build reports — the single source of truth stamped from the
/// release tag into `Cargo.toml`.
const CURRENT_VERSION: &str = env!("CARGO_PKG_VERSION");

/// Upper bound on the downloaded `.exe` size, so a bogus feed cannot make us read
/// unboundedly. The real binary is a few tens of MB.
const MAX_DOWNLOAD_BYTES: u64 = 200 * 1024 * 1024;

/// One parsed entry of the update feed. Unknown fields (e.g. `pub_date`) are
/// ignored by serde.
#[derive(Debug, Clone, Deserialize)]
pub struct Manifest {
    /// Marketing version of the available build, e.g. `1.2026.2`.
    pub version: String,
    /// Direct download URL of the new `.exe` on the GitHub Release.
    pub url: String,
    /// Base64 Ed25519 signature of the `.exe` bytes (Sparkle `sign_update` output).
    pub ed_signature: String,
    /// Expected size of the `.exe` in bytes; a cheap integrity pre-check.
    pub length: u64,
    /// Optional link to the release notes (shown as "Xem thay đổi").
    #[serde(default)]
    pub notes_url: Option<String>,
}

/// Errors surfaced to the About pane. `Display` is the user-facing Vietnamese
/// text — the whole UI is Vietnamese.
#[derive(Debug)]
pub enum Error {
    Network(String),
    BadManifest(String),
    SizeMismatch { expected: u64, actual: u64 },
    BadSignature,
    Replace(String),
}

impl std::fmt::Display for Error {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Error::Network(_) => write!(f, "Không kết nối được máy chủ cập nhật."),
            Error::BadManifest(_) => write!(f, "Dữ liệu cập nhật không hợp lệ."),
            Error::SizeMismatch { .. } => write!(f, "Tải bản cập nhật bị lỗi, vui lòng thử lại."),
            Error::BadSignature => write!(f, "Chữ ký bản cập nhật không hợp lệ — đã huỷ."),
            Error::Replace(_) => write!(f, "Không thay được tệp chương trình. Hãy tải thủ công."),
        }
    }
}

impl std::error::Error for Error {}

pub type Result<T> = std::result::Result<T, Error>;

/// The feed URL, allowing a debug-build override (`FUNPUT_UPDATE_FEED`) so the
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
    ureq::AgentBuilder::new()
        .timeout_connect(Duration::from_secs(15))
        .timeout_read(Duration::from_secs(60))
        .build()
}

/// Fetch and parse the update manifest from the GitHub Release feed.
pub fn fetch_manifest() -> Result<Manifest> {
    let resp = agent()
        .get(&feed_url())
        .call()
        .map_err(|e| Error::Network(e.to_string()))?;
    // `into_string` is core ureq (no extra feature), with a built-in size cap.
    let body = resp
        .into_string()
        .map_err(|e| Error::Network(e.to_string()))?;
    serde_json::from_str(&body).map_err(|e| Error::BadManifest(e.to_string()))
}

/// Whether `candidate` is a strictly newer version than this build.
pub fn is_newer(candidate: &str) -> bool {
    version_is_newer(candidate, CURRENT_VERSION)
}

/// The marketing version this build reports.
pub fn current_version() -> &'static str {
    CURRENT_VERSION
}

/// Pure version comparison, split out so it can be unit-tested without a build.
/// Treats unparseable versions as "not newer" (fail safe — never offers junk).
fn version_is_newer(candidate: &str, current: &str) -> bool {
    match (
        semver::Version::parse(candidate),
        semver::Version::parse(current),
    ) {
        (Ok(c), Ok(cur)) => c > cur,
        _ => false,
    }
}

/// Download the `.exe` bytes, enforcing the manifest's expected length.
pub fn download(url: &str, expected_len: u64) -> Result<Vec<u8>> {
    let resp = agent()
        .get(url)
        .call()
        .map_err(|e| Error::Network(e.to_string()))?;

    let mut bytes = Vec::with_capacity(expected_len.min(MAX_DOWNLOAD_BYTES) as usize);
    resp.into_reader()
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

/// Verify the downloaded bytes against the embedded public key. Sparkle signs the
/// raw file with Ed25519 (libsodium), which is byte-compatible with `ed25519-dalek`.
pub fn verify(bytes: &[u8], ed_signature: &str) -> Result<()> {
    verify_with_key(bytes, ed_signature, PUBLIC_ED_KEY)
}

/// Inner verify that takes the public key explicitly, so tests can use their own
/// keypair instead of the production one.
fn verify_with_key(bytes: &[u8], ed_signature: &str, public_key_b64: &str) -> Result<()> {
    let key_bytes = BASE64
        .decode(public_key_b64)
        .ok()
        .and_then(|b| <[u8; 32]>::try_from(b).ok())
        .ok_or(Error::BadSignature)?;
    let verifying_key = VerifyingKey::from_bytes(&key_bytes).map_err(|_| Error::BadSignature)?;

    let sig_bytes = BASE64.decode(ed_signature).map_err(|_| Error::BadSignature)?;
    let signature = Signature::from_slice(&sig_bytes).map_err(|_| Error::BadSignature)?;

    verifying_key
        .verify(bytes, &signature)
        .map_err(|_| Error::BadSignature)
}

/// Write the verified bytes to a temp file and swap them in for the running
/// executable. After this returns, `current_exe()` points at the new build.
pub fn stage_and_replace(bytes: &[u8]) -> Result<()> {
    let staged = std::env::temp_dir().join("Funput-update.exe");
    std::fs::write(&staged, bytes).map_err(|e| Error::Replace(e.to_string()))?;
    let result = self_replace::self_replace(&staged).map_err(|e| Error::Replace(e.to_string()));
    // Best-effort cleanup; the swap already copied the bytes into place.
    let _ = std::fs::remove_file(&staged);
    result
}

/// Relaunch the (now updated) executable and exit this process. Never returns.
pub fn relaunch() -> ! {
    if let Ok(exe) = std::env::current_exe() {
        let _ = std::process::Command::new(exe).spawn();
    }
    std::process::exit(0);
}

#[cfg(test)]
mod tests {
    use super::*;
    use ed25519_dalek::{Signer, SigningKey};

    #[test]
    fn newer_version_detected() {
        assert!(version_is_newer("1.2026.2", "1.2026.1"));
        assert!(version_is_newer("2.0.0", "1.2026.1"));
    }

    #[test]
    fn same_or_older_is_not_newer() {
        assert!(!version_is_newer("1.2026.1", "1.2026.1"));
        assert!(!version_is_newer("1.2026.0", "1.2026.1"));
    }

    #[test]
    fn unparseable_version_is_not_newer() {
        assert!(!version_is_newer("not-a-version", "1.2026.1"));
        assert!(!version_is_newer("1.2026.2", "garbage"));
    }

    #[test]
    fn manifest_parses_and_ignores_unknown_fields() {
        let json = r#"{
            "version": "1.2026.2",
            "url": "https://example.com/Funput-1.2026.2.exe",
            "ed_signature": "AAAA",
            "length": 1234,
            "pub_date": "2026-06-26T10:00:00Z",
            "notes_url": "https://example.com/notes"
        }"#;
        let m: Manifest = serde_json::from_str(json).expect("parse");
        assert_eq!(m.version, "1.2026.2");
        assert_eq!(m.length, 1234);
        assert_eq!(m.notes_url.as_deref(), Some("https://example.com/notes"));
    }

    #[test]
    fn verify_accepts_good_signature_and_rejects_tampering() {
        // Deterministic keypair (no RNG dependency) standing in for the CI key.
        let signing = SigningKey::from_bytes(&[7u8; 32]);
        let pubkey_b64 = BASE64.encode(signing.verifying_key().to_bytes());
        let payload = b"funput release bytes";
        let sig_b64 = BASE64.encode(signing.sign(payload).to_bytes());

        assert!(verify_with_key(payload, &sig_b64, &pubkey_b64).is_ok());
        // Flip one byte of the payload → verification must fail.
        assert!(verify_with_key(b"funput release bytez", &sig_b64, &pubkey_b64).is_err());
        // Garbage signature → fail, not panic.
        assert!(verify_with_key(payload, "!!!notbase64!!!", &pubkey_b64).is_err());
    }
}
