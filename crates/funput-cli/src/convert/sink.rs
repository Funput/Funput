//! Writing the converted document out.

use std::io::Write;

use crate::cli::CliError;

/// Write `bytes` to standard output, untouched.
///
/// Bytes, not a string, and `print!` would be wrong here. Converting to TCVN3 or
/// VNI-Windows produces one byte per Vietnamese letter — that is what a `.VnTime`
/// document holds — and printing it as text would write two bytes for each and
/// produce a file Word cannot read back.
///
/// A closed pipe is not an error. `funput convert … | head` closes the pipe as soon
/// as it has enough, and reporting that as a failure would make the exit code lie.
pub(super) fn write(bytes: &[u8]) -> Result<(), CliError> {
    let mut out = std::io::stdout().lock();
    match out.write_all(bytes).and_then(|()| out.flush()) {
        Err(e) if e.kind() == std::io::ErrorKind::BrokenPipe => Ok(()),
        other => Ok(other?),
    }
}
