# Gõ tắt: bung bằng Enter và Tab

> **Trạng thái:** đã làm trên Windows (PR #735) và Linux (#738), cả hai chưa thử tay. Hai công tắc riêng cho Enter và Tab, đều **tắt mặc định**. macOS giữ hành vi sẵn có (luôn bung), iOS, Android chưa làm.

Nguồn: issue #582 (Windows), mục "Nút tùy chọn On/Off cho phím kích hoạt viết tắt".

## Hiện trạng

- **Windows:** Enter và Tab là `KeyKind::Flush` ([key.rs](../../crates/funput-desktop/src/key.rs)). Funput chốt từ đang gõ như đang hiện rồi cho phím đi tiếp. Engine không nhận hai phím này, nên không bung gõ tắt, không English restore, không sửa lỗi gõ.
- **Engine:** mọi khoảng trắng là ranh giới từ (`is_english_boundary`, [boundary/mod.rs](../../crates/funput-engine/src/compose/boundary/mod.rs)), kể cả `\n` và `\t`. Nền tảng nào gửi Enter vào engine dưới dạng ký tự thì gõ tắt bung.
- **iOS:** phím Return gửi `"\n"` vào luồng nhập ([KeyboardInputCoordinator+Actions.swift:64](../../platforms/ios/Packages/FunputKit/Sources/KeyboardInput/Coordinator/KeyboardInputCoordinator+Actions.swift:64)), nên có vẻ đã bung khi bấm Return. **Chưa kiểm chứng.**
- **macOS:** đã luôn bung khi bấm Enter và Tab, cả ở chế độ tiếng Anh, không có công tắc: `InputEventPolicy.isBoundary` coi `\t`, `\n`, `\r` là ranh giới, `commitBoundary` commit nội dung bung rồi để app nhận phím. Giữ nguyên, không đổi lặng lẽ.
- **Linux:** trước #738 không bung: Enter, Tab là `NonText` (`classify.cpp`), chỉ commit từ đang gõ. Nay có hai công tắc như Windows (xem Đã làm (Linux)).
- **Android:** chưa kiểm tra.

## Người dùng xin gì

- Mặc định vẫn là Space và dấu câu.
- Thêm công tắc riêng cho Enter và Tab.
- Khi Enter/Tab bung gõ tắt thì **vẫn cho phím đó đi tiếp** (`ct` Enter ra `cơm tấm` rồi xuống dòng hoặc gửi), không như Unikey nuốt Enter để bung, bắt người dùng bấm Enter lần nữa.

## Quyết định

- **Hai công tắc riêng** trong phần Gõ tắt:
  - "Bung gõ tắt khi bấm Enter"
  - "Bung gõ tắt khi bấm Tab"
- **Cả hai tắt mặc định.**
  - Enter trong app chat là gửi tin. Space bung nhầm còn Backspace để hoàn tác, Enter bung nhầm thì tin đã đi.
  - Tab trong IDE, terminal, ô tìm kiếm là gợi ý tự động hoặc thụt dòng. Bung ngay trước Tab phá gợi ý của app.
- **Bung xong vẫn gửi phím đi tiếp.** Không làm như Unikey.
- **Space và dấu câu giữ nguyên:** luôn bung khi bật gõ tắt. Không thêm công tắc cho chúng, không làm ô tự nhập ký tự kích hoạt.
- **Chỉ thêm gõ tắt.** Enter và Tab vẫn không chạy English restore và sửa lỗi gõ như hiện nay. Đổi chuyện đó là đổi cách gõ, bàn riêng nếu cần.

## Hành vi chi tiết

| Phím | Bung khi bật công tắc |
|---|---|
| Enter | Có |
| Shift+Enter | Có (xuống dòng trong app chat) |
| Ctrl+Enter, Alt+Enter | Không (phím tắt của app) |
| Tab | Có |
| Shift+Tab | Không (lùi ô, bỏ thụt dòng) |
| Ctrl+Tab, Alt+Tab | Không (đã là phím tắt, `Flush` như cũ) |

- Smart case áp dụng như với Space ([shortcut-case-and-repair.md](shortcut-case-and-repair.md)).
- **Ctrl để chốt** ([ctrl-seal-word.md](ctrl-seal-word.md)) vẫn chặn được: `ct` Ctrl Enter ra `ct` rồi xuống dòng.
- **Không có hoàn tác bằng Backspace** sau khi Enter/Tab bung: lúc đó đã xuống dòng, đã gửi, hoặc đã sang ô khác. Engine không ghi nhịp hoàn tác cho lần bung này.
- Từ đang gõ không khớp chữ tắt nào thì Enter/Tab hoạt động như hiện nay.

## Engine

Cần một cách bung gõ tắt **mà không chạy phần còn lại của ranh giới**, đã làm là `Engine::expand_shortcut()`:

- Từ đang gõ khớp chữ tắt thì trả kết quả bung (xóa chữ đang hiện, ghi nội dung bung) **không kèm** ký tự ranh giới. Nền tảng tự gửi phím Enter/Tab thật, vì phím thật mới mang nghĩa "gửi", "sang ô khác" của app.
- Không khớp thì trả về không làm gì. Nền tảng chốt từ như hiện nay.
- Không ghi nhịp hoàn tác gõ tắt. Không chạy English restore, sửa lỗi gõ.

Không gửi `\n` hay `\t` vào `process_key` như iOS: ký tự đó không phải phím Enter/Tab thật với app, và sẽ kéo theo English restore với sửa lỗi gõ.

## Nền tảng

### Windows (làm trước)

Ở nhánh `Flush` của Enter/Tab, khi công tắc tương ứng bật:

1. Hỏi engine `expand_shortcut`. Không khớp thì làm như hiện nay.
2. Khớp thì **nuốt** phím Enter/Tab, inject phần xóa và nội dung bung, rồi inject lại đúng phím Enter/Tab (giữ Shift nếu có).
3. Cập nhật `CommittedTail` như sau một `Flush` của Enter (`Caret::LineStart`) hoặc Tab (`Caret::Unknown`).

Chỗ dễ vỡ: ô có gợi ý tự động (thanh địa chỉ Firefox, ô tìm kiếm). Enter có thể chọn gợi ý trước khi chữ bung xong. Phải thử trên app thật.

### macOS, Linux

macOS đã bung sẵn khi bấm Enter/Tab (xem Hiện trạng). Muốn có công tắc như Windows thì mặc định phải **bật** ở macOS để không lặng lẽ bỏ hành vi đang có; chưa chốt.

Linux: cùng cách Windows, trước khi Enter/Tab commit vùng đang gõ, hỏi `expand_shortcut`, commit nội dung bung, rồi để phím đi tiếp. Đã làm, xem bên dưới.

### iOS, Android

**Kiểm tra trước:** hiện tại Return trên điện thoại có bung gõ tắt không. Sau đó chọn một trong hai:

- Thêm cùng hai công tắc (Tab không có trên bàn phím ảo, chỉ cần Enter).
- Ghi rõ đây là khác biệt có chủ ý: trên điện thoại Return thường là nút "Gửi", "Tìm" của app.

Nếu iOS đang bung khi Return qua đường `"\n"`, đó là hành vi hiện có, không được đổi lặng lẽ.

## Việc cần làm trước khi code

- Kiểm tra hành vi Return trên iOS và Android, ghi kết quả vào tài liệu này.
- Kiểm tra macOS và Linux hiện xử lý Enter/Tab thế nào khi đang gõ dở.

## Đã làm (Windows)

- Engine: `Engine::expand_shortcut()` (`engine/shortcut.rs`), dùng chung phần khớp chữ tắt với `expansion()` ở ranh giới (`compose/boundary/shortcut.rs`, `rewrite`). Trả nội dung bung không kèm ký tự ranh giới, không ghi nhịp hoàn tác, tôn trọng lần bỏ qua sau khi người dùng vừa hoàn tác. Khớp thì chốt từ.
- Cấu hình: `shortcutOnEnter`, `shortcutOnTab` trong `funput-config`, thiếu thì là `false`, mang theo khi xuất/nhập. Không nằm trong `EngineConfig`: hook đọc thẳng từ `Settings`.
- `funput-desktop`: `KeyKind::Trigger(TriggerKey)` cho Enter (cả Shift+Enter) và Tab không Shift; Ctrl, Alt, Win cùng phím vẫn là `Flush`. `ShellState::trigger_key` hỏi engine khi công tắc bật, rồi luôn `caret_moved` như `Flush` cũ.
- Windows: hook nuốt Enter/Tab khi có nội dung bung, inject nội dung bung rồi gửi lại đúng phím đó (`inject::send_key`, gắn `INJECT_TAG`). Shift đang giữ thật nên Shift+Enter đi ra vẫn là Shift+Enter. Enter ở bàn phím số được gửi lại như Enter thường.
- Cài đặt: hai dòng trong trang Gõ tắt, ngay dưới "Gõ tắt cả khi ở chế độ tiếng Anh".
- Danh sách gợi ý của app (#712): từ ở caret bắt đầu bằng `@`, `#`, `:` hay `~` (kể cả khi đã chọn mục bằng mũi tên lên/xuống) thì Enter và Tab là phím của danh sách, không bung gõ tắt: `@ct` Enter giữ nguyên `@ct`.

## Đã làm (Linux)

- FFI: `funput_engine_expand_shortcut` (`crates/funput-ffi/src/engine/shortcuts/expand.rs`), trả nội dung bung qua callback UTF-8 như `funput_engine_undo_shortcut`, vì `FunputResult.chars` cắt ở `CHARS_CAP`. Các hàm gõ tắt của FFI gom vào `engine/shortcuts/` (bảng, `undo.rs`, `expand.rs`).
- Composer: `KeyKind::Trigger` cho Enter (cả Shift+Enter và Enter bàn phím số) và Tab không Shift; Ctrl, Alt, Super cùng phím vẫn là `Shortcut`, Shift+Tab (kể cả `ISO_Left_Tab`) vẫn là `NonText`. `Composer::onTriggerKey` (`compose/composer/keys/trigger.cpp`) hỏi engine khi công tắc bật. Preedit: commit nội dung bung thay cho vùng gạch chân rồi cho phím đi tiếp. Gõ thẳng: sửa từ trong tài liệu (`planFromResult`) rồi cho phím đi tiếp. Không khớp thì commit như cũ. Enter vẫn bật viết hoa đầu câu cho dòng mới.
- Cài đặt: `shortcutOnEnter`, `shortcutOnTab` trong `settings.json` (cùng khoá với Windows), addon chỉ đọc, thiếu thì `false`; settings-gtk thêm hai dòng dưới "Gõ tắt cả khi ở chế độ tiếng Anh", mang theo khi xuất/nhập cấu hình.

## Kiểm thử

- Engine (`tests/words/expand_shortcut.rs`): khớp và không khớp; không ghi nhịp hoàn tác; smart case; chữ vừa hoàn tác giữ nguyên; `500k` không bung; tắt gõ tắt; chế độ tiếng Anh theo công tắc gõ tắt tiếng Anh. Ctrl chốt từ chưa làm nên chưa có ca đó.
- `funput-desktop`: `key.rs` phủ bảng hành vi (Enter, Shift+Enter, Ctrl/Alt+Enter, Tab, Shift/Ctrl/Alt+Tab); `src/shell/tests.rs` qua `ShellState` thật: mặc định tắt, bật từng phím, hai công tắc độc lập, từ không phải chữ tắt, chế độ tiếng Anh, lưu cài đặt.
- Windows thật: Notepad, Word, Chrome, thanh địa chỉ Firefox, Zalo, Messenger (Enter gửi), Excel (Tab sang ô), VS Code (Tab gợi ý).
- Linux (`common/tests`): `classify_tests.cpp` phủ bảng phím; `keys/trigger_tests.cpp` phủ công tắc tắt, preedit, gõ thẳng, từng công tắc riêng, Shift+Tab và Ctrl+Enter, từ không phải chữ tắt, chế độ tiếng Anh, viết hoa đầu dòng, Backspace sau lần bung; `settings/shortcut_keys_tests.cpp` phủ đọc, mặc định, `save()` không ghi đè. settings-gtk: model và xuất/nhập.
- Linux thật (fcitx5 và ibus): trình soạn thảo, thanh địa chỉ trình duyệt, app chat (Enter gửi), terminal (Tab).
