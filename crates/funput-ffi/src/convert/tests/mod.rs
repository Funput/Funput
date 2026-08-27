use std::ptr;

use tempfile::tempdir;

use crate::*;

unsafe fn text(read: impl Fn(*mut u32, usize) -> usize) -> String {
    let len = read(ptr::null_mut(), 0);
    let mut output = vec![0; len];
    assert_eq!(read(output.as_mut_ptr(), output.len()), len);
    output.into_iter().filter_map(char::from_u32).collect()
}

#[test]
fn text_session_returns_full_result_and_exact_bytes() {
    let session = funput_convert_session_new();
    let input: Vec<u32> = "Việt".chars().map(u32::from).collect();
    unsafe {
        funput_convert_session_set_input(session, input.as_ptr(), input.len());
        funput_convert_session_pick_source(session, 0);
        funput_convert_session_set_target(session, 1);
        funput_convert_session_refresh(session);
        let view = funput_convert_session_view(session);
        assert_eq!(view.mode, FUNPUT_CONVERT_MODE_TEXT);
        assert_eq!(view.source, 0);
        assert_eq!(
            text(|out, cap| funput_convert_session_result_text(session, out, cap)),
            "ViÖt"
        );
        let len = funput_convert_session_save_bytes(session, ptr::null_mut(), 0);
        let mut bytes = vec![0; len];
        funput_convert_session_save_bytes(session, bytes.as_mut_ptr(), len);
        assert_eq!(bytes, b"Vi\xD6t");
        funput_convert_session_free(session);
    }
}

#[test]
fn scan_window_uses_global_rows_and_job_is_one_shot() {
    let dir = tempdir().unwrap();
    for index in 0..3 {
        std::fs::write(dir.path().join(format!("{index}.txt")), "Việt").unwrap();
    }
    let session = funput_convert_session_new();
    let scan = funput_convert_scan_new();
    let path = dir.path().to_string_lossy();
    unsafe {
        assert!(funput_convert_scan_add_path(
            scan,
            path.as_ptr(),
            path.len()
        ));
        assert!(funput_convert_scan_run(scan));
        assert!(funput_convert_session_adopt_scan(session, scan));
        assert!(!funput_convert_session_adopt_scan(session, scan));
        funput_convert_session_set_row_window(session, 1, 1);
        funput_convert_session_refresh(session);
        let view = funput_convert_session_view(session);
        assert_eq!(view.mode, FUNPUT_CONVERT_MODE_FILES);
        assert_eq!(
            (view.rows_first, view.rows_count, view.rows_total),
            (1, 1, 3)
        );
        funput_convert_session_pick_row_source(session, 1, 0);
        let job = funput_convert_job_new(session);
        assert!(funput_convert_job_run(job));
        assert!(!funput_convert_job_run(job));
        assert!(text(|out, cap| funput_convert_job_report(job, out, cap)).contains("Đã chuyển"));
        funput_convert_job_free(job);
        funput_convert_scan_free(scan);
        funput_convert_session_free(session);
    }
}

#[test]
fn all_convert_handles_are_null_safe() {
    unsafe {
        funput_convert_session_reset(ptr::null_mut());
        funput_convert_session_refresh(ptr::null_mut());
        assert_eq!(funput_convert_session_view(ptr::null()).rows_total, 0);
        assert!(!funput_convert_scan_run(ptr::null_mut()));
        assert!(!funput_convert_job_run(ptr::null_mut()));
        funput_convert_session_free(ptr::null_mut());
        funput_convert_scan_free(ptr::null_mut());
        funput_convert_job_free(ptr::null_mut());
    }
}
