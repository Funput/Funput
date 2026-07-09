//! Keep the PTY's window size in sync with the real terminal.

use std::thread;

use portable_pty::PtySize;

#[cfg(unix)]
pub(super) fn spawn_resize_thread(master: Box<dyn portable_pty::MasterPty + Send>) {
    use signal_hook::consts::SIGWINCH;
    use signal_hook::iterator::Signals;

    thread::spawn(move || {
        let Ok(mut signals) = Signals::new([SIGWINCH]) else {
            return;
        };
        for _ in signals.forever() {
            if let Ok((cols, rows)) = crossterm::terminal::size() {
                let _ = master.resize(PtySize {
                    rows,
                    cols,
                    pixel_width: 0,
                    pixel_height: 0,
                });
            }
        }
    });
}

// Windows (and any non-Unix) has no SIGWINCH, so poll the terminal size and push
// changes to the PTY. `crossterm::size` + `master.resize` are cross-platform, so this
// needs no OS-specific code; the ~120ms tick is imperceptible for window resizes.
#[cfg(not(unix))]
pub(super) fn spawn_resize_thread(master: Box<dyn portable_pty::MasterPty + Send>) {
    use std::time::Duration;

    thread::spawn(move || {
        let mut last = crossterm::terminal::size().unwrap_or((80, 24));
        loop {
            thread::sleep(Duration::from_millis(120));
            let Ok((cols, rows)) = crossterm::terminal::size() else {
                continue;
            };
            if (cols, rows) != last {
                last = (cols, rows);
                let _ = master.resize(PtySize {
                    rows,
                    cols,
                    pixel_width: 0,
                    pixel_height: 0,
                });
            }
        }
    });
}
