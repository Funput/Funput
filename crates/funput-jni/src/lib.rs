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
//! - `abi/` — the shared panic/`Outcome` guard and Java-string marshalling.
//!
//! Exports link by `#[no_mangle]` symbol name, so there is no re-export surface to
//! maintain — each submodule only needs to be compiled in.

mod abi;
mod engine;
mod suggestion;
