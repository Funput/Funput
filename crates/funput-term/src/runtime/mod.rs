//! Interposer orchestration: spawn the child in a PTY and shuttle bytes both
//! ways, composing Vietnamese on the input path.

use std::io;
use std::sync::Arc;
use std::thread;

use funput_core::InputMethod;
use portable_pty::{CommandBuilder, PtySize, native_pty_system};

use crate::config::TermConfig;
use crate::terminal::{RawModeGuard, set_cursor_cue, set_title};

mod driver;
mod inject;
mod input;
mod output;
mod resize;
mod state;

use driver::forward_input;
use output::forward_output;
use resize::spawn_resize_thread;
use state::SharedState;

pub use driver::Status;

/// Run options resolved from config, env, and the command line.
pub struct Options {
    pub config: TermConfig,
    pub command: Vec<String>,
}

fn pty_err<E: std::fmt::Display>(e: E) -> io::Error {
    io::Error::other(e.to_string())
}

/// Spawn `opts.command` in a PTY and run the interposer until the child exits.
/// Returns the child's exit code.
pub fn run(opts: Options) -> io::Result<i32> {
    // `command[0]` is the program to exec; refuse an empty command rather than
    // index-panic (the CLI always fills a default, but this is a public entry).
    let program = opts
        .command
        .first()
        .ok_or_else(|| io::Error::other("no command to run"))?;

    let (cols, rows) = crossterm::terminal::size().unwrap_or((80, 24));
    let size = PtySize {
        rows,
        cols,
        pixel_width: 0,
        pixel_height: 0,
    };

    // Enter raw mode first: if there is no real terminal this fails fast, before
    // we spawn a child we'd have to clean up. Restored on drop.
    let _raw = RawModeGuard::enter()?;

    let pair = native_pty_system().openpty(size).map_err(pty_err)?;

    let mut cmd = CommandBuilder::new(program);
    for arg in &opts.command[1..] {
        cmd.arg(arg);
    }
    if let Ok(cwd) = std::env::current_dir() {
        cmd.cwd(cwd);
    }
    for (key, value) in std::env::vars() {
        cmd.env(key, value);
    }

    let mut child = pair.slave.spawn_command(cmd).map_err(pty_err)?;
    drop(pair.slave);

    let writer = pair.master.take_writer().map_err(pty_err)?;
    let reader = pair.master.try_clone_reader().map_err(pty_err)?;

    let state = Arc::new(SharedState::new(opts.config.enabled));

    spawn_resize_thread(pair.master);

    // Reflect the initial composition state in the title and cursor cue.
    let vi_color = opts.config.vi_cursor_color.clone();
    update_indicators(
        Status {
            enabled: opts.config.enabled,
            method: opts.config.method,
        },
        &vi_color,
    );

    // Child -> terminal (own thread; ends at EOF when the child exits).
    let state_out = Arc::clone(&state);
    let output = thread::spawn(move || {
        let _ = forward_output(reader, io::stdout(), &state_out);
    });

    // Terminal -> child (detached; blocks on stdin until the process exits).
    let state_in = Arc::clone(&state);
    let config = opts.config;
    let status_color = vi_color.clone();
    thread::spawn(move || {
        let _ = forward_input(io::stdin(), writer, &config, &state_in, |status| {
            update_indicators(status, &status_color);
        });
    });

    let status = child.wait().map_err(pty_err)?;
    let _ = output.join();

    // Restore the user's default cursor — never leave it recolored after exit.
    let _ = set_cursor_cue(&mut io::stdout(), false, &vi_color);

    Ok(status.exit_code() as i32)
}

/// Update both VI/EN indicators (window title + cursor color) to match `status`.
/// The title also shows the active method so a live Telex↔VNI switch is visible.
fn update_indicators(status: Status, vi_color: &str) {
    let mut out = io::stdout();
    let vi_en = if status.enabled { "VI" } else { "EN" };
    let method = match status.method {
        InputMethod::Telex => "Telex",
        InputMethod::Vni => "VNI",
    };
    let _ = set_title(&mut out, &format!("Funput · {vi_en} · {method}"));
    let _ = set_cursor_cue(&mut out, status.enabled, vi_color);
}
