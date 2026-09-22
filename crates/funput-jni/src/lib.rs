//! JNI boundary between Android and the shared Funput engine.
//!
//! # Layout
//!
//! Two independent JNI surfaces over shared plumbing, mirroring `funput-ffi`:
//!
//! - `engine/` — composition (`FunputNative_native*`): key input, rendered text,
//!   configuration, and ID-based engine ownership.
//! - `suggestion/` — the personal-suggestion store (`PersonalSuggestionNative*`),
//!   independent of composition.
//! - `sentence.rs` — the shared sentence-boundary rules, stateless and so taking no
//!   engine handle.
//! - `abi/` — the shared panic/`Outcome` guard and Java-string marshalling.
//!
//! Exports link by `#[no_mangle]` symbol name, so there is no re-export surface to
//! maintain — each submodule only needs to be compiled in.

mod abi;
mod engine;
mod sentence;
mod suggestion;
