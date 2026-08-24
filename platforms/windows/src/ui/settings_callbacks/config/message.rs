//! What each notice on the Dữ liệu page says.
//!
//! Split from the callbacks next door because building a sentence and wiring a
//! Slint handler are different jobs, and only one of them is worth reading when
//! the wording turns out to be wrong.

use funput_config::transfer::ImportSummary;
use funput_core::charset::Charset;

/// Just the file name: the full path is the user's own choice from a dialog they
/// just dismissed, and spelling it out crowds the notice for no new information.
pub(super) fn shown_path(path: &std::path::Path) -> String {
    path.file_name()
        .map(|name| name.to_string_lossy().into_owned())
        .unwrap_or_else(|| path.display().to_string())
}

/// A macro file carries only gõ tắt, so this says nothing about typing options —
/// unlike [`import_body`], which reports a whole config document. Zero of both
/// means every row was already present with the same text.
///
/// A named charset means the file was written before Unicode and the encoding had
/// to be *guessed*. Saying which one is the only warning available: the guess can
/// be confidently wrong about an encoding Funput does not implement, and the user
/// is the only one who can look at the result and tell.
pub(super) fn unikey_body(summary: &ImportSummary, charset: Option<Charset>) -> String {
    let counts = if summary.shortcuts_added == 0 && summary.shortcuts_updated == 0 {
        "Bảng gõ tắt đã có sẵn các mục này, không có gì thay đổi.".to_string()
    } else {
        format!(
            "Gõ tắt: thêm {}, cập nhật {}.",
            summary.shortcuts_added, summary.shortcuts_updated
        )
    };
    // Only when the text was actually *reinterpreted*. A file that read as ordinary
    // Unicode had nothing guessed about it, and warning on every import teaches the
    // user to ignore the line that matters.
    match charset.filter(|&c| c != Charset::Unicode) {
        Some(other) => format!(
            "{counts}\nĐọc bằng bảng mã {} — hãy kiểm lại nếu chữ trông lạ.",
            charset_label(other)
        ),
        None => counts,
    }
}

/// What to call a charset in front of a user. `Charset` is `#[non_exhaustive]`, so
/// the wildcard is required rather than a shortcut — and a charset with no name
/// here simply goes unmentioned, which is better than a debug string.
pub(super) fn charset_label(charset: Charset) -> &'static str {
    match charset {
        Charset::Unicode => "Unicode",
        Charset::Tcvn3 => "TCVN3 (ABC)",
        Charset::VniWindows => "VNI-Windows",
        Charset::UnicodeCombining => "Unicode tổ hợp",
        _ => "khác",
    }
}

pub(super) fn import_body(summary: &ImportSummary) -> String {
    let mut lines = vec!["Đã áp các tuỳ chọn gõ.".to_string()];
    if summary.shortcuts_added > 0 || summary.shortcuts_updated > 0 {
        lines.push(format!(
            "Gõ tắt: thêm {}, cập nhật {}.",
            summary.shortcuts_added, summary.shortcuts_updated
        ));
    } else {
        lines.push("Không có gõ tắt mới.".to_string());
    }
    if summary.applied_platform {
        lines.push("Đã áp phím tắt và danh sách app bỏ qua.".to_string());
    }
    if summary.newer_version {
        lines.push("Lưu ý: tệp từ phiên bản mới hơn — một số mục có thể bị bỏ qua.".to_string());
    }
    lines.join("\n")
}
