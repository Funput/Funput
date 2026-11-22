# Bỏ gõ tắt vừa bung

## Vấn đề

Chữ tắt ngắn bung cả khi người dùng muốn đúng chữ vừa gõ. Cài `a` → `anh` rồi gõ `A ` thì ra `Anh `, và không có cách lấy lại `A ` mà vẫn giữ dấu cách (issue #616). Trước tính năng này, Backspace ngay sau khi bung chỉ xóa dấu cách rồi mở lại từ cuối của nội dung thay thế.

## Hành vi

Ngay sau khi một chữ tắt bung, **Backspace một lần** — hoặc chạm chip `↩` đứng đầu thanh gợi ý — trả lại chữ đang hiện trước khi bung, kèm đúng ký tự ranh giới:

- `vn ` bung thành `việt nam `, Backspace ra `vn `, chip `↩ vn`. Backspace thứ hai xóa dấu cách như mọi từ khác.
- Chữ trả lại là chữ mà ranh giới sẽ ghi nếu không có gõ tắt. Thường là chữ đã ghép (`ddc` đang hiện `đc` thì ra `đc `), nhưng một từ mà English restore sẽ trả về phím gốc thì quay lại đúng phím gốc (`text`, không phải `tẽt`).
- Hoạt động cả khi bàn phím ở tiếng Anh có bật gõ tắt, và không phụ thuộc gợi ý cá nhân hay tự sửa lỗi gõ.
- Nhịp này chết khi gõ phím khác, đổi ô, di con trỏ, hoặc khi tài liệu không còn kết thúc bằng phần đã bung. Khi đó Backspace xóa một ký tự như thường.

**Quay lại sau khi gõ thêm** (#782, Windows và macOS). Gõ tiếp qua phần đã bung rồi xóa lùi về **đúng** cuối phần đó thì Backspace lại hoàn tác như trên: `ct␣` → `cơm tấm␣` → gõ `này rất ngon` → xóa lùi về `cơm tấm␣` → ⌫ ra `ct␣`. Backspace thêm một lần mở lại `ct` để sửa thành chữ tắt khác. Chỉ tính vị trí của chính lần bung đó: cùng đoạn chữ gõ tay ở chỗ khác không được hoàn tác. Click, di con trỏ, đổi ô hay bật/tắt tiếng Việt đều làm quên lần bung.

**Lệnh bỏ qua một lần.** Sau khi hoàn tác, chữ tắt vừa từ chối được giữ nguyên cho **từ kế tiếp** nếu đúng là chuỗi đó: không bung, không bị sửa lỗi gõ, English restore vẫn chạy. Từ nào kết thúc trước cũng làm hết hạn lệnh này, nên một lần hoàn tác không làm chữ tắt mất tác dụng ở câu sau. Dấu cách thừa không kết thúc từ nào nên không tiêu lệnh. Lệnh thuộc về đúng chữ vừa được đặt lại. Xóa dấu cách để mở lại nguyên chữ đó rồi bấm Space thì vẫn giữ nguyên. Hễ Backspace xóa vào chữ đó thì lệnh hết hạn: xóa hết `vn ` rồi gõ lại thì bung bình thường (#742).

## Hợp đồng engine

Trạng thái nằm trong `Session::shortcut_undo: Option<Box<ShortcutUndo>>` ([shortcut_undo.rs](../../crates/funput-engine/src/compose/boundary/shortcut_undo.rs)). Boxed và `None` cho tới khi có một lần bung, nên engine rảnh chỉ tốn một con trỏ (`size_of::<Engine>()` 184 → 192).

- Bung ghi lại chữ trước khi bung (`plain_form`), chuỗi đã khớp, và cả cụm đã ghi kèm ranh giới. `Session::clear()` ở ranh giới giữ nó, vì hệ thống chủ trả lời sau khi từ đã đóng.
- Đầu mỗi `process_key` làm hết nhịp hoàn tác, nhưng engine vẫn nhớ lần bung; lệnh bỏ qua ở lại. `rearm_shortcut_undo` bật lại nhịp cho lần bung đã nhớ khi không có chữ đang gõ. Chỉ hệ thống chủ biết con trỏ đã quay về đúng cuối lần bung (không phải cùng đoạn chữ ở chỗ khác), nên nó kiểm tra rồi mới gọi. Lần bung mới thay lần cũ; hoàn tác, `dismiss_shortcut_undo` và `Engine::clear()` làm quên nó.
- Lệnh bỏ qua nằm ở [shortcut_undo/refusal.rs](../../crates/funput-engine/src/compose/boundary/shortcut_undo/refusal.rs), giữ cả chuỗi đã khớp lẫn chữ đã đặt lại. Engine tự hủy lệnh, hệ thống chủ không phải gọi gì thêm: `on_backspace` xóa một ký tự của chữ đang gõ thì hủy, `adopt` mở lại một chữ khác chữ đã đặt lại thì hủy (kể cả khi `adopt` từ chối chữ đó, như `đc` của chữ tắt `ddc`).
- `Engine::on_backspace` **không** nuốt hoàn tác: chỉ hệ thống chủ biết phần đã bung còn nằm ngay trước con trỏ hay không, nên nó kiểm tra trước rồi tự gọi `undo_shortcut`.
- API: `has_shortcut_undo`, `shortcut_undo_text`, `shortcut_undo_written`, `undo_shortcut`, `rearm_shortcut_undo`, `dismiss_shortcut_undo`. `Engine::clear()` cũng xóa cả hai. C ABI: `funput_engine_rearm_shortcut_undo`.

## Nền tảng

Hệ thống chủ phải kiểm tra tài liệu **trước** khi hỏi engine, vì hỏi là tiêu nhịp.

**Android** ([android-shortcuts.md](android-shortcuts.md)): JNI `nativeHasShortcutUndo`, `nativeShortcutUndoText`, `nativeShortcutUndoSuffix`, `nativeUndoShortcut`, `nativeDismissShortcutUndo`. `ShortcutUndo` kiểm tra hậu tố trước con trỏ rồi thay trong một batch; chip `SHORTCUT_UNDO` đứng đầu thanh gợi ý khi không có phần sửa lỗi gõ.

**iOS**: C ABI `funput_engine_shortcut_undo_text`, `funput_engine_undo_shortcut`, `funput_engine_dismiss_shortcut_undo` ([shortcut_undo.rs](../../crates/funput-ffi/src/engine/shortcut_undo.rs)). Văn bản đi ra qua callback UTF-8 như `funput_process_key_text`, không qua `FunputResult.chars` vốn cắt ở 64 codepoint. Swift:

- `KeyboardShortcutState.undo` giữ `shown` và `written` ngay lúc bung (`noteExpansion`, gọi sau `composer.process` ở ranh giới có gửi). `written` là chính kết quả của lần bung nên không cần hàm FFI đọc hậu tố.
- `undoShortcut(builder:)` chạy trong `performDeleteBackward` ngay sau `undoCorrection`: kiểm tra `contextBeforeInput` kết thúc bằng `written` và không có vùng chọn, rồi mới gọi engine. Cờ `tookBackLastEdit` chặn mở lại từ sau cú hoàn tác.
- Chip đi theo đường sửa lỗi gõ đã có: `CorrectionPreview.shortcut`, kind `shortcutUndo`, `CorrectionBarAction.undoShortcut`. Nhánh chip nằm **trước** các guard `allowsAutocorrect` và từ điển.
- `clearComposition()` xóa trạng thái này ở mọi sự kiện làm tài liệu đổi dưới chân.

**Windows** ([shortcut-case-and-repair.md](shortcut-case-and-repair.md) §2): không có chip, Backspace ngay sau khi bung là hoàn tác, giống Android. `funput_desktop::undo_shortcut` kiểm tra bóng chữ đã gõ (`CommittedTail`) còn kết thúc bằng `written` rồi mới gọi engine, và sửa bóng chữ theo kết quả. `ShellState::on_backspace` trả kết quả đó cho hook, hook nuốt Backspace và inject. Mọi sự kiện gọi `reset_composition` (click, phím mũi tên, Enter, đổi focus, bật/tắt tiếng Việt) xóa luôn nhịp hoàn tác qua `Engine::clear()`.

- Quay lại sau khi gõ thêm: `track_expansion` (gọi trong `ShellState::process_key`, sau `after_key`) ghi độ dài bóng chữ ngay sau lần bung (`CommittedTail::mark_expansion_end`). Bóng chữ là đúng văn bản trước con trỏ, nên khi Backspace thấy không có chữ đang gõ và độ dài quay về đúng mốc (`at_expansion_end`), `undo_shortcut` gọi `rearm_shortcut_undo` rồi đi tiếp đường kiểm tra `written` như trên. Mốc dời theo khi bóng chữ cắt đầu (`retone/expansion.rs`), bị bỏ khi vết cắt chạm tới nó, khi `clear` và khi đã hoàn tác.

**macOS** (#737): không có chip, Backspace ngay sau khi bung là hoàn tác, như Windows. Mã nằm ở `platforms/macos/Funput/IME/ShortcutUndo/`.

- Phần đã bung lấy từ engine qua `funput_engine_shortcut_undo_written` (C ABI, cùng khuôn `_undo_text`), nên host không giữ bản sao. Với Enter và Tab, engine ghi kèm `\n`/`\t` mà tài liệu không có, nên kiểm tra trượt và Backspace xóa như thường.
- `ShortcutUndoDocument.range` chỉ trả vùng thay khi client hỗ trợ document access, có con trỏ không vùng chọn và không marked text (`Retone.caret`), và đúng `written` nằm ngay trước con trỏ. Mọi lần đọc là round trip sang app, nên chỉ chạy khi Backspace gặp nhịp đang chờ.
- `ShortcutUndoSession` ghi số click lúc nhịp được bật (`keyHandled`, chạy sau mỗi phím) và bỏ nhịp nếu đã click: chữ đã commit không có marked text nên IMKit không báo click. Bộ đếm chỉ được đọc khi có nhịp.
- Nhánh Backspace với buffer rỗng (`FunputInputController.handle`) thử hoàn tác trước `reopenPreviousWord`; chế độ tiếng Anh có gõ tắt cũng vậy. Không phụ thuộc công tắc retone. Kiểm tra trượt thì `dismissShortcutUndo` rồi để app xóa.
- Quay lại sau khi gõ thêm: lúc bung, `keyHandled(by:on:)` ghi cả vị trí con trỏ (UTF-16, `ShortcutUndoDocument.caret`), một lần đọc tài liệu cho mỗi lần bung. Backspace sau đó, khi chưa có nhịp, đọc con trỏ; đúng vị trí đã ghi và chưa click thì `rearmShortcutUndo` rồi đi tiếp như trên. Con trỏ ở chỗ khác thì Backspace xóa như thường và lần bung vẫn được nhớ. Client không cho biết con trỏ thì không bật lại.

**Linux** (#737, fcitx5 và ibus dùng chung composer): không có chip, Backspace ngay sau khi bung là hoàn tác. Mã nằm ở `platforms/linux/common/compose/composer/keys/shortcut_undo.cpp`.

- Chỉ ở chế độ gõ trực tiếp (không preedit). Shell tự bật chế độ này cho client báo surrounding text; preedit là đúng những client không tin được một lệnh xóa trong tài liệu, và ở đó ranh giới đã gọi `Engine::clear()`.
- Tin tài liệu bằng chính bằng chứng mà Backspace ngoài từ đòi: journal đã đồng bộ (`inSync`), không có vùng chọn, và **tài liệu dự đoán** kết thúc bằng `funput_engine_shortcut_undo_written`. Kết quả là một `Effect::Replace` như mọi sửa chữa khác; shell không phải sửa gì. Đặt `backspaceJournaled = false` để shell không mở lại từ sau cú hoàn tác.
- Ranh giới đọc phần bung qua `funput_process_key_text` (`Handle::processText`), không qua `FunputResult.chars`, ở cả hai chế độ. Trước đây nội dung dài hơn 64 ký tự bị cắt.
- Enter và Tab trên Linux không bung gõ tắt (chúng là phím `NonText`), nên không có ca hoàn tác nào cho chúng.
- Chưa có phần quay lại sau khi gõ thêm (#782); Android và iOS cũng chưa.

## Giới hạn đã biết

- iOS chỉ giữ một cửa sổ chữ trước con trỏ (`shadowContextLimit` = 128). Nội dung bung dài hơn cửa sổ đó không khớp `written`, nên chip ẩn và Backspace xóa như thường: an toàn nhưng chưa hoàn tác được.
- Windows: nội dung bung dài hơn bóng chữ (256 byte) không khớp `written`, nên Backspace xóa như thường. Gõ thêm đủ dài để bóng chữ cắt mất lần bung thì xóa lùi về cũng không hoàn tác được.
- macOS: app không cho đọc văn bản trước con trỏ (Warp, Google Docs) thì không hoàn tác được, Backspace xóa như thường.
- Linux: client ở chế độ preedit (chưa báo surrounding text, nằm trong danh sách hidden-preedit, sau cầu gnome-shell khi chắc chắn mất lệnh xóa, hay đã bị bắt bỏ lệnh xóa) không hoàn tác được, Backspace xóa như thường. Client trả lời trễ cũng vậy.

## Kiểm thử

Engine: `tests/words/shortcut_undo.rs`, `tests/alloc_budget/shortcut.rs`. JNI: `funput-jni/src/engine/shortcuts/tests.rs`. C ABI: phần test của `funput-ffi/src/engine/shortcut_undo.rs`. Windows: `funput-desktop/tests/retone.rs`, `src/shell/tests.rs`, `src/retone/tests.rs`. Android: `ShortcutUndoTest`, `ShortcutUndoInstrumentedTest`. iOS: `KeyboardInputTests/Shortcuts/`, `FunputEngineTests/FunputShortcutUndoTests.swift`, và các ca chip ở `SuggestionBarComposerTests`, `PersonalSuggestionToolbarTests`. Test iOS không chạy trên PR: chạy `Scripts/test-funput-kit.sh` tại máy. macOS: `FunputTests/ShortcutUndo/` (`xcodebuild … test` tại máy, CI không chạy Swift). Linux: `platforms/linux/common/tests/compose/composer/keys/shortcut_undo_tests.cpp` và `boundary_tests.cpp` (job CI `linux-common`).
