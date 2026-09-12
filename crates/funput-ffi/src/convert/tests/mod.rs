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

/// Menu positions for the transforms these tests press, found the way a host finds
/// them — by reading the menu, not by writing the list down again.
fn position(label: &str) -> usize {
    (0..funput_convert_transform_count())
        .find(|&index| unsafe {
            text(|out, cap| funput_convert_transform_name(index, out, cap)) == label
        })
        .unwrap_or_else(|| panic!("no transform called {label}"))
}

/// The whole axis over the door: press, read back, undo, clear — and the result text
/// changing at each step, because that is the only proof the presses reached core.
#[test]
fn transforms_apply_in_order_and_come_back_off_one_at_a_time() {
    let session = funput_convert_session_new();
    let input: Vec<u32> = "GỬI VỀ TP. HCM".chars().map(u32::from).collect();
    let (lower, title) = (position("chữ thường"), position("Viết Hoa Đầu Mỗi Từ"));
    unsafe {
        funput_convert_session_set_input(session, input.as_ptr(), input.len());
        funput_convert_session_pick_source(session, 0);
        funput_convert_session_apply_transform(session, lower);
        funput_convert_session_apply_transform(session, title);
        funput_convert_session_refresh(session);

        let casing = funput_convert_session_casing(session);
        assert_eq!(casing.count, 2);
        assert!(!casing.keep_d && !casing.flatten_caps, "fresh session");
        assert_eq!(
            funput_convert_session_applied_transform(session, 0),
            i32::try_from(lower).unwrap()
        );
        assert_eq!(
            funput_convert_session_applied_transform(session, 1),
            i32::try_from(title).unwrap()
        );
        assert_eq!(
            text(|out, cap| funput_convert_session_result_text(session, out, cap)),
            "Gửi Về Tp. Hcm"
        );

        funput_convert_session_undo_transform(session);
        funput_convert_session_refresh(session);
        assert_eq!(funput_convert_session_casing(session).count, 1);
        assert_eq!(
            text(|out, cap| funput_convert_session_result_text(session, out, cap)),
            "gửi về tp. hcm"
        );

        funput_convert_session_clear_transforms(session);
        funput_convert_session_refresh(session);
        assert_eq!(funput_convert_session_casing(session).count, 0);
        assert_eq!(
            text(|out, cap| funput_convert_session_result_text(session, out, cap)),
            "GỬI VỀ TP. HCM"
        );
        funput_convert_session_free(session);
    }
}

/// A switch has to reach core, not just the snapshot.
#[test]
fn a_switch_changes_what_the_door_hands_back() {
    let session = funput_convert_session_new();
    let input: Vec<u32> = "đẹp".chars().map(u32::from).collect();
    let bo_dau = position("Bỏ dấu tiếng Việt");
    unsafe {
        funput_convert_session_set_input(session, input.as_ptr(), input.len());
        funput_convert_session_pick_source(session, 0);
        funput_convert_session_apply_transform(session, bo_dau);
        funput_convert_session_refresh(session);
        assert_eq!(
            text(|out, cap| funput_convert_session_result_text(session, out, cap)),
            "dep"
        );

        funput_convert_session_set_keep_d(session, true);
        funput_convert_session_refresh(session);
        assert!(funput_convert_session_casing(session).keep_d);
        assert_eq!(
            text(|out, cap| funput_convert_session_result_text(session, out, cap)),
            "đep"
        );
        funput_convert_session_free(session);
    }
}

/// The menu is the host's only list, so it has to be complete, named, and sized the
/// way every other text on this door is sized.
#[test]
fn the_transform_menu_is_complete_and_sizes_like_the_rest_of_the_door() {
    assert_eq!(
        funput_convert_transform_count(),
        funput_convert::casing::ALL.len(),
        "the door and the table disagree about how many transforms there are"
    );
    unsafe {
        for index in 0..funput_convert_transform_count() {
            let needed = funput_convert_transform_name(index, ptr::null_mut(), 0);
            assert!(needed > 0, "transform {index} has no name");

            // One short of enough writes nothing and still reports the length.
            let mut small = vec![0u32; needed - 1];
            assert_eq!(
                funput_convert_transform_name(index, small.as_mut_ptr(), small.len()),
                needed
            );
            assert!(small.iter().all(|&slot| slot == 0), "wrote a partial name");

            assert_eq!(
                text(|out, cap| funput_convert_transform_name(index, out, cap))
                    .chars()
                    .count(),
                needed
            );
        }
        assert_eq!(funput_convert_transform_name(999, ptr::null_mut(), 0), 0);
    }
}

/// Out of range and never-pressed both answer, rather than panicking or inventing.
#[test]
fn an_empty_session_answers_every_question_about_the_axis() {
    let session = funput_convert_session_new();
    unsafe {
        let casing = funput_convert_session_casing(session);
        assert_eq!(casing.count, 0);
        assert_eq!(
            funput_convert_session_applied_transform(session, 0),
            FUNPUT_CONVERT_UNKNOWN
        );
        assert_eq!(
            funput_convert_session_applied_transform(session, 999),
            FUNPUT_CONVERT_UNKNOWN
        );
        // Null is a no-op everywhere else on this door, and here too.
        funput_convert_session_apply_transform(ptr::null_mut(), 0);
        funput_convert_session_undo_transform(ptr::null_mut());
        funput_convert_session_clear_transforms(ptr::null_mut());
        funput_convert_session_set_keep_d(ptr::null_mut(), true);
        funput_convert_session_set_flatten_caps(ptr::null_mut(), true);
        assert_eq!(funput_convert_session_casing(ptr::null()).count, 0);
        assert_eq!(
            funput_convert_session_applied_transform(ptr::null(), 0),
            FUNPUT_CONVERT_UNKNOWN
        );
        funput_convert_session_free(session);
    }
}
