//! The transform menu: what a shell offers, and what to call each entry.
//!
//! The same contract as the charset menu in [`crate::charset`], for the same reason.
//! A transform is an **index into `funput_convert::casing::ALL`**, never a name the
//! host spells for itself: a host writing its own list would silently miss a
//! transform added later, and these two calls are between them enough to build the
//! menu without one.
//!
//! **That list is append-only, and it is what makes the index an identity.** A host
//! may store the index — as a remembered last choice, or as the list of what the
//! user has applied — so reordering would quietly turn a saved `CHỮ HOA` into
//! `chữ thường`.

use funput_convert::casing;

use crate::abi::{safe, write_text};

/// How many transforms there are. Valid indices run
/// `0..funput_convert_transform_count()`.
#[unsafe(no_mangle)]
pub extern "C" fn funput_convert_transform_count() -> usize {
    casing::ALL.len()
}

/// Write the label of the transform at `index` into `out` as UTF-32, returning its
/// length in codepoints. An index out of range gives 0.
///
/// The label comes from `funput-convert` so that three platforms' menus cannot drift
/// apart, and it is interface text — Vietnamese, like the loss warning beside it.
///
/// Sizing works as everywhere else on this door: the length comes back whether or
/// not it fit, and nothing is written unless all of it fits.
///
/// # Safety
/// `out` must point to at least `cap` writable `u32` values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_transform_name(
    index: usize,
    out: *mut u32,
    cap: usize,
) -> usize {
    safe(0, || {
        let Some(&transform) = casing::ALL.get(index) else {
            return 0;
        };
        unsafe { write_text(casing::name(transform), out, cap) }
    })
}
