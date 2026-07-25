//! Composition JNI surface: the `FunputNative_native*` entry points and the
//! ID-based ownership they run on.
//!
//! - [`registry`] — ID → `Engine` ownership (`create` / `destroy` / `with_mut`);
//!   the JNI analogue of an opaque handle, so a stale or repeated destroy is a
//!   safe no-op.
//! - [`lifecycle`] — `nativeCreate` / `nativeDestroy` / `nativeClear`.
//! - [`composition`] — `nativeProcess` / `nativeBoundary` / `nativeBackspace`.
//! - [`settings`] — `nativeConfigure` (the durable options in one call) and
//!   `nativeSetEnabled` (runtime VI/EN).
//!
//! Exports link by `#[no_mangle]` symbol name, so their module location is
//! invisible to the Android runtime.

mod composition;
mod lifecycle;
mod registry;
mod settings;
