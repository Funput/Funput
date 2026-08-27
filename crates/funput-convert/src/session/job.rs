//! A batch, lifted out of the session so it can leave the thread.
//!
//! Writing every file in a drop is I/O, and which thread that belongs on is the
//! shell's call — GTK reaches for `gio::spawn_blocking` and Slint for a thread plus
//! `invoke_from_event_loop`. A session lives on the UI thread and cannot follow, so
//! it hands out the work instead of doing it.

use funput_core::charset::Charset;

use crate::batch::{self, Entry, Outcome};

use super::{Session, at};

/// A batch, ready to be written on whatever thread the shell picks.
pub struct Job {
    entries: Vec<Entry>,
    target: Charset,
}

impl Job {
    pub fn run(self) -> Outcome {
        batch::write_all(&self.entries, self.target)
    }
}

impl Session {
    /// The batch as a unit of work that can leave this thread.
    pub fn batch_job(&self) -> Job {
        Job {
            entries: self.files.clone(),
            target: at(self.target),
        }
    }
}
