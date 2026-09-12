//! The second axis over the C door: which transforms are applied, and the two
//! switches they read.
//!
//! Every call here is a thin marshalling of `funput_convert`'s own commands. What is
//! worth saying is the shape, because an ABI is append-only and a shape settled
//! wrongly is settled forever:
//!
//! - **The transforms are a list the user built**, not a single choice. A press is
//!   [`funput_convert_session_apply_transform`], undo takes the last one off, and the
//!   list is read back the way every other collection on this door is read: a count
//!   in the snapshot, then one accessor per index.
//! - **The axis has a snapshot of its own.** `FunputConvertView` does not grow a tail
//!   of fields that only some hosts read, and a third axis — if one ever arrives —
//!   gets its own struct rather than lengthening a struct everyone reads.
//! - **Each switch is a named call.** The two are not a homogeneous list: one belongs
//!   to bỏ dấu and the other to title case, so a `set_option(index, on)` would be a
//!   false uniformity. A third switch costs one more export, which is what an
//!   append-only door is for.

mod menu;

pub use menu::{funput_convert_transform_count, funput_convert_transform_name};

use crate::abi::safe;

use super::FUNPUT_CONVERT_UNKNOWN;
use super::handle::{FunputConvertSession, with_mut, with_ref};

/// What the second axis is doing, as scalars.
#[repr(C)]
#[derive(Clone, Copy, Default)]
pub struct FunputConvertCasing {
    /// How many transforms are applied. Read them with
    /// [`funput_convert_session_applied_transform`].
    pub count: usize,
    /// Named for what it turns **on**, so a fresh session is all-false.
    pub keep_d: bool,
    pub flatten_caps: bool,
}

/// Press the transform at `index` in the menu. Out of range clamps, as everywhere
/// else a menu position crosses this door.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_apply_transform(
    session: *mut FunputConvertSession,
    index: usize,
) {
    unsafe { with_mut(session, |value| value.apply_transform(index)) };
}

/// Take off the last transform. A session with none is a no-op, not an error.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_undo_transform(session: *mut FunputConvertSession) {
    unsafe { with_mut(session, funput_convert::Session::undo_transform) };
}

/// Back to the document as it arrived.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_clear_transforms(
    session: *mut FunputConvertSession,
) {
    unsafe { with_mut(session, funput_convert::Session::clear_transforms) };
}

/// Keep `đ` and `Đ` instead of writing `d` and `D` (bỏ dấu).
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_set_keep_d(
    session: *mut FunputConvertSession,
    on: bool,
) {
    unsafe { with_mut(session, |value| value.set_keep_d(on)) };
}

/// Also lowercase words that are already all-caps (title case).
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_set_flatten_caps(
    session: *mut FunputConvertSession,
    on: bool,
) {
    unsafe { with_mut(session, |value| value.set_flatten_caps(on)) };
}

/// The axis as of the last refresh. Null gives the all-false snapshot.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_casing(
    session: *const FunputConvertSession,
) -> FunputConvertCasing {
    unsafe { with_ref(session, snapshot) }
}

fn snapshot(session: &funput_convert::Session) -> FunputConvertCasing {
    let view = session.view();
    FunputConvertCasing {
        count: view.transforms.len(),
        keep_d: view.keep_d,
        flatten_caps: view.flatten_caps,
    }
}

/// The menu position of the `index`-th transform applied, in the order pressed, or
/// [`FUNPUT_CONVERT_UNKNOWN`] when there is no such press.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_applied_transform(
    session: *const FunputConvertSession,
    index: usize,
) -> i32 {
    safe(FUNPUT_CONVERT_UNKNOWN, || {
        unsafe { session.as_ref() }
            .and_then(|s| s.inner.view().transforms.get(index).copied())
            .and_then(|position| i32::try_from(position).ok())
            .unwrap_or(FUNPUT_CONVERT_UNKNOWN)
    })
}
