//! Pure builder for the marked, idempotent shell alias block (unit-tested directly).

use super::Shell;

/// Start/end markers delimiting the block we own in the rc file, so `--write`
/// stays idempotent and the block is easy to find or remove by hand.
pub(super) const MARKER_START: &str = "# >>> funput term >>>";
pub(super) const MARKER_END: &str = "# <<< funput term <<<";

impl Shell {
    fn alias_line(self, name: &str, command: &str) -> String {
        match self {
            // POSIX-style quoting for bash/zsh; fish uses a space, not `=`.
            Shell::Bash | Shell::Zsh => format!("alias {name}='funput term -- {command}'"),
            Shell::Fish => format!("alias {name} 'funput term -- {command}'"),
        }
    }
}

/// Parse an `name=command` argument; a bare `name` aliases the command of the same
/// name (`claude` → `claude`→`funput term -- claude`).
pub fn parse_alias(arg: &str) -> (String, String) {
    match arg.split_once('=') {
        Some((name, command)) => (name.to_string(), command.to_string()),
        None => (arg.to_string(), arg.to_string()),
    }
}

/// Build the marked, idempotent shell block for `aliases`. With no aliases it
/// emits a commented example so the user can see the shape.
pub fn snippet(shell: Shell, aliases: &[(String, String)]) -> String {
    let mut out = String::new();
    out.push_str(MARKER_START);
    out.push('\n');
    out.push_str("# Funput Terminal — type Vietnamese inside terminal apps.\n");
    if aliases.is_empty() {
        out.push_str("# Example: add `--alias claude` to wrap a command, e.g.\n");
        out.push_str(&format!("#   {}\n", shell.alias_line("claude", "claude")));
        out.push_str("# Or wrap your whole shell from your terminal emulator:\n");
        out.push_str("#   funput term -- $SHELL\n");
    } else {
        for (name, command) in aliases {
            out.push_str(&shell.alias_line(name, command));
            out.push('\n');
        }
    }
    out.push_str(MARKER_END);
    out.push('\n');
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    fn aliases() -> Vec<(String, String)> {
        vec![("claude".to_string(), "claude".to_string())]
    }

    #[test]
    fn bash_zsh_use_equals_quoting() {
        let s = snippet(Shell::Zsh, &aliases());
        assert!(s.contains("alias claude='funput term -- claude'"));
        assert!(s.contains(MARKER_START) && s.contains(MARKER_END));
    }

    #[test]
    fn fish_uses_space_syntax() {
        let s = snippet(Shell::Fish, &aliases());
        assert!(s.contains("alias claude 'funput term -- claude'"));
    }

    #[test]
    fn empty_aliases_emit_commented_example() {
        let s = snippet(Shell::Bash, &[]);
        assert!(s.contains(MARKER_START) && s.contains(MARKER_END));
        // The example is commented out, so sourcing the block is a no-op.
        assert!(!s.lines().any(|l| l.starts_with("alias ")));
    }

    #[test]
    fn alias_arg_parsing() {
        assert_eq!(parse_alias("claude"), ("claude".into(), "claude".into()));
        assert_eq!(parse_alias("cc=claude"), ("cc".into(), "claude".into()));
    }
}
