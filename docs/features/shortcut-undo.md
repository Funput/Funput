# Bỏ gõ tắt vừa bung

## Vấn đề

Chữ tắt ngắn bung cả khi người dùng muốn đúng chữ vừa gõ. Cài `a` → `anh` rồi gõ `A ` thì ra `Anh `, và không có cách lấy lại `A ` mà vẫn giữ dấu cách (issue #616). Trước tính năng này, Backspace ngay sau khi bung chỉ xóa dấu cách rồi mở lại từ cuối của nội dung thay thế.

## Hành vi

Ngay sau khi một chữ tắt bung, **Backspace một lần** — hoặc chạm chip `↩` đứng đầu thanh gợi ý — trả lại chữ đang hiện trước khi bung, kèm đúng ký tự ranh giới:

- `vn ` bung thành `việt nam `, Backspace ra `vn `, chip `↩ vn`. Backspace thứ hai xóa dấu cách như mọi từ khác.
- Chữ trả lại là chữ mà ranh giới sẽ ghi nếu không có gõ tắt. Thường là chữ đã ghép (`ddc` đang hiện `đc` thì ra `đc `), nhưng một từ mà English restore sẽ trả về phím gốc thì quay lại đúng phím gốc (`text`, không phải `tẽt`).
- Hoạt động cả khi bàn phím ở tiếng Anh có bật gõ tắt, và không phụ thuộc gợi ý cá nhân hay tự sửa lỗi gõ.
- Nhịp này chết khi gõ phím khác, đổi ô, di con trỏ, hoặc khi tài liệu không còn kết thúc bằng phần đã bung. Khi đó Backspace xóa một ký tự như thường.

**Lệnh bỏ qua một lần.** Sau khi hoàn tác, chữ tắt vừa từ chối được giữ nguyên cho **từ kế tiếp** nếu đúng là chuỗi đó: không bung, không bị sửa lỗi gõ, English restore vẫn chạy. Từ nào kết thúc trước cũng làm hết hạn lệnh này, nên một lần hoàn tác không làm chữ tắt mất tác dụng ở câu sau. Dấu cách thừa không kết thúc từ nào nên không tiêu lệnh. Mọi Backspace không phải cú hoàn tác cũng hủy lệnh: xóa hết `vn ` rồi gõ lại thì bung bình thường.

## Hợp đồng engine

Trạng thái nằm trong `Session::shortcut_undo: Option<Box<ShortcutUndo>>` ([shortcut_undo.rs](../../crates/funput-engine/src/compose/boundary/shortcut_undo.rs)). Boxed và `None` cho tới khi có một lần bung, nên engine rảnh chỉ tốn một con trỏ (`size_of::<Engine>()` 184 → 192).

- Bung ghi lại chữ trước khi bung (`plain_form`), chuỗi đã khớp, và cả cụm đã ghi kèm ranh giới. `Session::clear()` ở ranh giới giữ nó, vì hệ thống chủ trả lời sau khi từ đã đóng.
- Đầu mỗi `process_key` làm hết nhịp hoàn tác; lệnh bỏ qua ở lại.
- `Engine::on_backspace` **không** nuốt hoàn tác: shell desktop tự xóa ký tự sẽ không hoàn tác một lần bung mà người dùng không thấy chip.
- API: `has_shortcut_undo`, `shortcut_undo_text`, `shortcut_undo_written`, `undo_shortcut`, `dismiss_shortcut_undo`. `Engine::clear()` cũng xóa cả hai.

## Nền tảng

Hệ thống chủ phải kiểm tra tài liệu **trước** khi hỏi engine, vì hỏi là tiêu nhịp.

**Android** ([android-shortcuts.md](android-shortcuts.md)): JNI `nativeHasShortcutUndo`, `nativeShortcutUndoText`, `nativeShortcutUndoSuffix`, `nativeUndoShortcut`, `nativeDismissShortcutUndo`. `ShortcutUndo` kiểm tra hậu tố trước con trỏ rồi thay trong một batch; chip `SHORTCUT_UNDO` đứng đầu thanh gợi ý khi không có phần sửa lỗi gõ.

**iOS**: C ABI `funput_engine_shortcut_undo_text`, `funput_engine_undo_shortcut`, `funput_engine_dismiss_shortcut_undo` ([shortcut_undo.rs](../../crates/funput-ffi/src/engine/shortcut_undo.rs)). Văn bản đi ra qua callback UTF-8 như `funput_process_key_text`, không qua `FunputResult.chars` vốn cắt ở 64 codepoint. Swift:

- `KeyboardShortcutState.undo` giữ `shown` và `written` ngay lúc bung (`noteExpansion`, gọi sau `composer.process` ở ranh giới có gửi). `written` là chính kết quả của lần bung nên không cần hàm FFI đọc hậu tố.
- `undoShortcut(builder:)` chạy trong `performDeleteBackward` ngay sau `undoCorrection`: kiểm tra `contextBeforeInput` kết thúc bằng `written` và không có vùng chọn, rồi mới gọi engine. Cờ `tookBackLastEdit` chặn mở lại từ sau cú hoàn tác.
- Chip đi theo đường sửa lỗi gõ đã có: `CorrectionPreview.shortcut`, kind `shortcutUndo`, `CorrectionBarAction.undoShortcut`. Nhánh chip nằm **trước** các guard `allowsAutocorrect` và từ điển.
- `clearComposition()` xóa trạng thái này ở mọi sự kiện làm tài liệu đổi dưới chân.

## Giới hạn đã biết

- iOS chỉ giữ một cửa sổ chữ trước con trỏ (`shadowContextLimit` = 128). Nội dung bung dài hơn cửa sổ đó không khớp `written`, nên chip ẩn và Backspace xóa như thường: an toàn nhưng chưa hoàn tác được.
- macOS, Windows và Linux chưa có chip nên chưa dùng tính năng này.

## Kiểm thử

Engine: `tests/words/shortcut_undo.rs`, `tests/alloc_budget/shortcut.rs`. JNI: `funput-jni/src/engine/shortcuts/tests.rs`. C ABI: phần test của `funput-ffi/src/engine/shortcut_undo.rs`. Android: `ShortcutUndoTest`, `ShortcutUndoInstrumentedTest`. iOS: `KeyboardInputTests/Shortcuts/`, `FunputEngineTests/FunputShortcutUndoTests.swift`, và các ca chip ở `SuggestionBarComposerTests`, `PersonalSuggestionToolbarTests`. Test iOS không chạy trên PR: chạy `Scripts/test-funput-kit.sh` tại máy.
