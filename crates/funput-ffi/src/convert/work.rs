use std::path::PathBuf;
use std::ptr;

use funput_convert::{Job, Scan};

use crate::abi::{safe, write_text};

use super::handle::{FunputConvertSession, with_ref};

pub struct FunputConvertScan {
    paths: Vec<PathBuf>,
    result: Option<Scan>,
}
pub struct FunputConvertJob {
    inner: Option<Job>,
    report: String,
}

#[unsafe(no_mangle)]
pub extern "C" fn funput_convert_scan_new() -> *mut FunputConvertScan {
    safe(ptr::null_mut(), || {
        Box::into_raw(Box::new(FunputConvertScan {
            paths: Vec::new(),
            result: None,
        }))
    })
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_scan_add_path(
    scan: *mut FunputConvertScan,
    path: *const u8,
    len: usize,
) -> bool {
    safe(false, || {
        let Some(scan) = (unsafe { scan.as_mut() }) else {
            return false;
        };
        if path.is_null() {
            return false;
        }
        let bytes = unsafe { std::slice::from_raw_parts(path, len) };
        let Ok(path) = std::str::from_utf8(bytes) else {
            return false;
        };
        scan.paths.push(PathBuf::from(path));
        true
    })
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_scan_run(scan: *mut FunputConvertScan) -> bool {
    safe(false, || {
        unsafe { scan.as_mut() }.is_some_and(|value| {
            value.result = Some(funput_convert::scan(&value.paths));
            true
        })
    })
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_session_adopt_scan(
    session: *mut FunputConvertSession,
    scan: *mut FunputConvertScan,
) -> bool {
    safe(false, || {
        let (Some(session), Some(scan)) = (unsafe { session.as_mut() }, unsafe { scan.as_mut() })
        else {
            return false;
        };
        let Some(result) = scan.result.take() else {
            return false;
        };
        session.inner.adopt(result);
        true
    })
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_scan_free(scan: *mut FunputConvertScan) {
    safe((), || {
        if !scan.is_null() {
            drop(unsafe { Box::from_raw(scan) })
        }
    });
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_job_new(
    session: *const FunputConvertSession,
) -> *mut FunputConvertJob {
    unsafe {
        with_ref(session, |value| {
            Box::into_raw(Box::new(FunputConvertJob {
                inner: Some(value.batch_job()),
                report: String::new(),
            }))
        })
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_job_run(job: *mut FunputConvertJob) -> bool {
    safe(false, || {
        let Some(job) = (unsafe { job.as_mut() }) else {
            return false;
        };
        let Some(work) = job.inner.take() else {
            return false;
        };
        job.report = funput_convert::report(&work.run());
        true
    })
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_job_report(
    job: *const FunputConvertJob,
    out: *mut u32,
    cap: usize,
) -> usize {
    safe(0, || {
        unsafe { job.as_ref() }.map_or(0, |value| unsafe { write_text(&value.report, out, cap) })
    })
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_convert_job_free(job: *mut FunputConvertJob) {
    safe((), || {
        if !job.is_null() {
            drop(unsafe { Box::from_raw(job) })
        }
    });
}
