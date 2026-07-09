//! `funput-term install` — wire the wrapper into the user's shell so Vietnamese
//! input is "always on".
//!
//! Emits an idempotent, marked block of shell aliases (each `name` runs
//! `funput-term -- cmd`). By default it just prints the block (safe to inspect);
//! `--write` appends it to the detected shell rc file, skipping if the marker is
//! already present. The [`snippet`] builder is pure, so it is unit-tested directly.

use std::io::{self, Write};
use std::path::PathBuf;

mod snippet;

use snippet::MARKER_START;
pub use snippet::{parse_alias, snippet};

/// Supported shells, distinguished only by alias syntax and rc-file location.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Shell {
    Bash,
    Zsh,
    Fish,
}

impl Shell {
    /// Match a shell by name or `$SHELL` path basename (`/bin/zsh` → `Zsh`).
    pub fn from_name(name: &str) -> Option<Self> {
        match name.rsplit('/').next().unwrap_or(name) {
            "bash" => Some(Shell::Bash),
            "zsh" => Some(Shell::Zsh),
            "fish" => Some(Shell::Fish),
            _ => None,
        }
    }

    /// Detect from `$SHELL`, defaulting to bash when unknown.
    pub fn detect() -> Self {
        std::env::var("SHELL")
            .ok()
            .and_then(|s| Shell::from_name(&s))
            .unwrap_or(Shell::Bash)
    }
}

/// The rc file a `--write` should append to for `shell`.
pub fn rc_path(shell: Shell) -> Option<PathBuf> {
    let home = dirs::home_dir()?;
    Some(match shell {
        Shell::Bash => home.join(".bashrc"),
        Shell::Zsh => home.join(".zshrc"),
        Shell::Fish => home.join(".config").join("fish").join("config.fish"),
    })
}

/// Run the `install` subcommand: print the snippet, and when `write` is set append
/// it to the shell rc file unless our marker is already there.
pub fn run(shell: Shell, aliases: &[(String, String)], write: bool) -> io::Result<()> {
    let block = snippet(shell, aliases);

    if !write {
        print!("{block}");
        return Ok(());
    }

    let path = rc_path(shell)
        .ok_or_else(|| io::Error::other("cannot determine home directory for rc file"))?;
    let existing = std::fs::read_to_string(&path).unwrap_or_default();
    if existing.contains(MARKER_START) {
        println!("funput term: already installed in {}", path.display());
        return Ok(());
    }

    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent)?;
    }
    let mut file = std::fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(&path)?;
    // Separate from any preceding content.
    writeln!(file, "\n{block}")?;
    println!("funput term: installed in {}", path.display());
    println!("Restart your shell or run `source {}`.", path.display());
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn shell_detection_from_path() {
        assert_eq!(Shell::from_name("/bin/zsh"), Some(Shell::Zsh));
        assert_eq!(Shell::from_name("fish"), Some(Shell::Fish));
        assert_eq!(Shell::from_name("/usr/bin/tcsh"), None);
    }
}
