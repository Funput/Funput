//! The two UTF-16 buffers `CreateProcessW` is handed: the command line and the
//! environment block. Split out because building them is string work with its own
//! rules, and none of it has anything to say about process lifetime.

use std::ffi::OsStr;
use std::os::windows::ffi::OsStrExt;
use std::path::Path;

/// `"<exe>" <arg>` as a mutable buffer — `CreateProcessW` is allowed to write into
/// the command line it is given.
///
/// `argv[0]` has to be there, and the install path can contain spaces, so it is
/// quoted. The arguments are fixed literals (`--control-center` and friends), so
/// nothing beyond that needs escaping.
pub(super) fn command_line(exe: &Path, arg: &str) -> Vec<u16> {
    let quote = u16::from(b'"');
    let mut line = vec![quote];
    line.extend(exe.as_os_str().encode_wide());
    line.extend([quote, u16::from(b' ')]);
    line.extend(arg.encode_utf16());
    line.push(0);
    line
}

/// This process's environment with `extra` layered on top, in the
/// `KEY=VALUE\0…\0\0` block `CREATE_UNICODE_ENVIRONMENT` expects.
///
/// Order is not significant — the child reads variables with
/// `GetEnvironmentVariable`, which scans the block — but a name appearing twice
/// would be, so anything `extra` overrides is dropped first. Windows compares
/// names case-insensitively, hence the folded match.
pub(super) fn environment(extra: &[(&str, &str)]) -> Vec<u16> {
    let mut block = Vec::new();
    for (key, value) in std::env::vars_os() {
        if extra.iter().any(|(name, _)| key.eq_ignore_ascii_case(name)) {
            continue;
        }
        push_var(&mut block, &key, &value);
    }
    for (key, value) in extra {
        push_var(&mut block, OsStr::new(key), OsStr::new(value));
    }
    if block.is_empty() {
        block.push(0); // an empty block is still two nulls, not one
    }
    block.push(0);
    block
}

fn push_var(block: &mut Vec<u16>, key: &OsStr, value: &OsStr) {
    block.extend(key.encode_wide());
    block.push(u16::from(b'='));
    block.extend(value.encode_wide());
    block.push(0);
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Read a block back as `KEY=VALUE` entries.
    fn entries(block: &[u16]) -> Vec<String> {
        String::from_utf16_lossy(block)
            .split('\0')
            .filter(|s| !s.is_empty())
            .map(str::to_string)
            .collect()
    }

    #[test]
    fn extra_vars_are_appended_and_the_block_is_double_terminated() {
        let block = environment(&[("FUNPUT_TRAY_X", "12")]);
        assert!(entries(&block).contains(&"FUNPUT_TRAY_X=12".to_string()));
        assert_eq!(&block[block.len() - 2..], &[0, 0]);
    }

    #[test]
    fn an_extra_var_never_appears_twice() {
        // Whatever the parent holds, the child must see one value for a name —
        // `GetEnvironmentVariable` takes the first hit and would shadow ours.
        let name = std::env::vars_os()
            .next()
            .map(|(k, _)| k.to_string_lossy().into_owned())
            .expect("the test process has an environment");
        let block = environment(&[(name.as_str(), "ours")]);
        let hits = entries(&block)
            .into_iter()
            .filter(|e| {
                e.split_once('=')
                    .is_some_and(|(k, _)| k.eq_ignore_ascii_case(&name))
            })
            .collect::<Vec<_>>();
        assert_eq!(hits, vec![format!("{name}=ours")]);
    }

    #[test]
    fn the_exe_path_is_quoted_so_spaces_survive() {
        let line = command_line(
            Path::new(r"C:\Program Files\Funput.exe"),
            "--control-center",
        );
        assert_eq!(
            String::from_utf16_lossy(&line),
            "\"C:\\Program Files\\Funput.exe\" --control-center\0"
        );
    }
}
