//! The personal dictionary over JNI, for the IME and the settings app in one APK.
//!
//! - `edit` — edits through a handle, draining the inbox, and posting to it.
//! - `list` — listings as parallel `String[]` and `long[]` arrays.
//! - `codes` — the `int` codes both sides agree on, and the flattening.
//!
//! The keyboard owns the store; the app reads it through a snapshot handle
//! (`nativeOpenSnapshot` in `lifecycle`) and posts edits rather than writing it.

mod codes;
mod edit;
mod list;

#[cfg(test)]
mod tests;
