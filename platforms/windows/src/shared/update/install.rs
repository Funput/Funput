//! Verifying a downloaded build and swapping it in.
//!
//! The signature is checked against the *same* Ed25519 key Sparkle uses on macOS
//! before a single byte is written anywhere executable.

use base64::engine::general_purpose::STANDARD as BASE64;
use base64::Engine;
use ed25519_dalek::{Signature, Verifier, VerifyingKey};

use super::{Error, Result, PUBLIC_ED_KEY};

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

    let sig_bytes = BASE64
        .decode(ed_signature)
        .map_err(|_| Error::BadSignature)?;
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
