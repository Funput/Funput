use std::ptr;

use funput_convert::Session;

use crate::abi::safe;

pub struct FunputConvertSession {
    pub(crate) inner: Session,
}

#[unsafe(no_mangle)]
pub extern "C" fn funput_convert_session_new() -> *mut FunputConvertSession {
    safe(ptr::null_mut(), || {
        Box::into_raw(Box::new(FunputConvertSession {
            inner: Session::new(),
        }))
    })
}

/// # Safety
/// `session` must be a live handle returned by `funput_convert_session_new`, or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_free(session: *mut FunputConvertSession) {
    safe((), || {
        if !session.is_null() {
            drop(unsafe { Box::from_raw(session) });
        }
    });
}

pub(crate) unsafe fn with_ref<R: Default>(
    session: *const FunputConvertSession,
    op: impl FnOnce(&Session) -> R,
) -> R {
    safe(R::default(), || {
        unsafe { session.as_ref() }.map_or_else(R::default, |value| op(&value.inner))
    })
}

pub(crate) unsafe fn with_mut<R: Default>(
    session: *mut FunputConvertSession,
    op: impl FnOnce(&mut Session) -> R,
) -> R {
    safe(R::default(), || {
        unsafe { session.as_mut() }.map_or_else(R::default, |value| op(&mut value.inner))
    })
}
