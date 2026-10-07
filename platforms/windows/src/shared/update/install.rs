//! Verifying a downloaded build and swapping it in.
//!
//! The signature is checked against the *same* Ed25519 key Sparkle uses on macOS
//! before a single byte is written anywhere executable.

use std::io;
use std::path::Path;

use base64::Engine;
use base64::engine::general_purpose::STANDARD as BASE64;
use ed25519_dalek::{Signature, Verifier, VerifyingKey};

use super::{Error, PUBLIC_ED_KEY, Result, leftovers};

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

/// Swap the verified bytes in for the running executable. After this returns,
/// `current_exe()` points at the new build, and the old one waits beside it for
/// the new build to delete — see [`super::leftovers`].
///
/// Nothing is deleted here, and nothing is left to a helper process: the old file
/// is mapped by the background process as well as by this Settings window, and
/// only the next start of Funput is sure to come after both of them.
pub fn stage_and_replace(bytes: &[u8]) -> Result<()> {
    std::env::current_exe()
        .and_then(|exe| swap_in(&exe, bytes, std::process::id()))
        .map_err(|e| Error::Replace(e.to_string()))
}

/// Put `bytes` at `exe`, moving what was there to its backup name. Every step is
/// a write or a rename inside the executable's own folder, so the renames cannot
/// fail for crossing drives, and a step that fails undoes the ones before it.
fn swap_in(exe: &Path, bytes: &[u8], tag: u32) -> io::Result<()> {
    let name = exe
        .file_name()
        .and_then(|n| n.to_str())
        .ok_or_else(|| io::Error::new(io::ErrorKind::InvalidInput, "no file name"))?;
    let new = exe.with_file_name(format!("{name}.{tag}.new"));
    let old = exe.with_file_name(leftovers::backup_name(name, tag));

    let undo_new = |e: io::Error| {
        let _ = std::fs::remove_file(&new);
        e
    };
    std::fs::write(&new, bytes).map_err(undo_new)?;
    std::fs::rename(exe, &old).map_err(undo_new)?;
    std::fs::rename(&new, exe).map_err(|e| {
        let _ = std::fs::rename(&old, exe);
        undo_new(e)
    })
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

    fn scratch(tag: &str) -> std::path::PathBuf {
        let dir = std::env::temp_dir().join(format!("funput-swap-{tag}-{}", std::process::id()));
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).unwrap();
        dir
    }

    fn names_in(dir: &Path) -> Vec<String> {
        let mut names: Vec<_> = std::fs::read_dir(dir)
            .unwrap()
            .map(|e| e.unwrap().file_name().into_string().unwrap())
            .collect();
        names.sort();
        names
    }

    #[test]
    fn the_new_build_takes_the_name_and_the_old_one_steps_aside() {
        let dir = scratch("ok");
        let exe = dir.join("Funput.exe");
        std::fs::write(&exe, b"old build").unwrap();

        swap_in(&exe, b"new build", 7).unwrap();

        assert_eq!(std::fs::read(&exe).unwrap(), b"new build");
        assert_eq!(
            std::fs::read(dir.join("Funput.exe.7.old")).unwrap(),
            b"old build"
        );
        assert_eq!(names_in(&dir), ["Funput.exe", "Funput.exe.7.old"]);
        let _ = std::fs::remove_dir_all(dir);
    }

    #[test]
    fn a_failed_swap_leaves_nothing_behind() {
        let dir = scratch("fail");
        let exe = dir.join("Funput.exe"); // never created, so moving it aside fails

        assert!(swap_in(&exe, b"new build", 7).is_err());
        assert!(names_in(&dir).is_empty(), "no half-written .new left");
        let _ = std::fs::remove_dir_all(dir);
    }
}
