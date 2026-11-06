# Gõ tắt: bung bằng Enter và Tab

> **Trạng thái:** đã chốt. Hai công tắc riêng cho Enter và Tab, đều **tắt mặc định**. Chưa có code.

Nguồn: issue #582 (Windows), mục "Nút tùy chọn On/Off cho phím kích hoạt viết tắt".

## Hiện trạng

- **Windows:** Enter và Tab là `KeyKind::Flush` ([key.rs](../../crates/funput-desktop/src/key.rs)). Funput chốt từ đang gõ như đang hiện rồi cho phím đi tiếp. Engine không nhận hai phím này, nên không bung gõ tắt, không English restore, không sửa lỗi gõ.
- **Engine:** mọi khoảng trắng là ranh giới từ (`is_english_boundary`, [boundary/mod.rs](../../crates/funput-engine/src/compose/boundary/mod.rs)), kể cả `\n` và `\t`. Nền tảng nào gửi Enter vào engine dưới dạng ký tự thì gõ tắt bung.
- **iOS:** phím Return gửi `"\n"` vào luồng nhập ([KeyboardInputCoordinator+Actions.swift:64](../../platforms/ios/Packages/FunputKit/Sources/KeyboardInput/Coordinator/KeyboardInputCoordinator+Actions.swift:64)), nên có vẻ đã bung khi bấm Return. **Chưa kiểm chứng.**
- **Android, macOS, Linux:** chưa kiểm tra.

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

Cần một cách bung gõ tắt **mà không chạy phần còn lại của ranh giới**, ví dụ `Engine::expand_before(key)`:

- Từ đang gõ khớp chữ tắt thì trả kết quả bung (xóa chữ đang hiện, ghi nội dung bung) **không kèm** ký tự ranh giới. Nền tảng tự gửi phím Enter/Tab thật, vì phím thật mới mang nghĩa "gửi", "sang ô khác" của app.
- Không khớp thì trả về không làm gì. Nền tảng chốt từ như hiện nay.
- Không ghi nhịp hoàn tác gõ tắt. Không chạy English restore, sửa lỗi gõ.

Không gửi `\n` hay `\t` vào `process_key` như iOS: ký tự đó không phải phím Enter/Tab thật với app, và sẽ kéo theo English restore với sửa lỗi gõ.

## Nền tảng

### Windows (làm trước)

Ở nhánh `Flush` của Enter/Tab, khi công tắc tương ứng bật:

1. Hỏi engine `expand_before`. Không khớp thì làm như hiện nay.
2. Khớp thì **nuốt** phím Enter/Tab, inject phần xóa và nội dung bung, rồi inject lại đúng phím Enter/Tab (giữ Shift nếu có).
3. Cập nhật `CommittedTail` như sau một `Flush` của Enter (`Caret::LineStart`) hoặc Tab (`Caret::Unknown`).

Chỗ dễ vỡ: ô có gợi ý tự động (thanh địa chỉ Firefox, ô tìm kiếm). Enter có thể chọn gợi ý trước khi chữ bung xong. Phải thử trên app thật.

### macOS, Linux

Cùng cách: trước khi Enter/Tab commit vùng đang gõ, hỏi `expand_before`, commit nội dung bung, rồi để phím đi tiếp. Làm sau Windows.

### iOS, Android

**Kiểm tra trước:** hiện tại Return trên điện thoại có bung gõ tắt không. Sau đó chọn một trong hai:

- Thêm cùng hai công tắc (Tab không có trên bàn phím ảo, chỉ cần Enter).
- Ghi rõ đây là khác biệt có chủ ý: trên điện thoại Return thường là nút "Gửi", "Tìm" của app.

Nếu iOS đang bung khi Return qua đường `"\n"`, đó là hành vi hiện có, không được đổi lặng lẽ.

## Việc cần làm trước khi code

- Kiểm tra hành vi Return trên iOS và Android, ghi kết quả vào tài liệu này.
- Kiểm tra macOS và Linux hiện xử lý Enter/Tab thế nào khi đang gõ dở.

## Kiểm thử

- Engine: `expand_before` khớp và không khớp; không ghi nhịp hoàn tác; smart case; Ctrl chốt rồi `expand_before` không làm gì.
- `crates/funput-desktop/tests/`: bảng hành vi ở trên, mỗi dòng một ca; công tắc tắt thì giữ hành vi cũ.
- Windows thật: Notepad, Word, Chrome, thanh địa chỉ Firefox, Zalo, Messenger (Enter gửi), Excel (Tab sang ô), VS Code (Tab gợi ý).
