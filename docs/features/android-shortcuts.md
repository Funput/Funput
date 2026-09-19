# Gõ tắt trên Android

Android có cùng mô hình Gõ tắt với iOS: bật/tắt, tìm kiếm, thêm/sửa/xoá, smart case
và mở rộng khi bàn phím đang ở tiếng Anh. UI dùng Material 3; thuật toán matching và
expansion vẫn chỉ nằm trong `funput-engine` dùng chung.

## Dữ liệu và quyền riêng tư

`:shortcut-store` là ranh giới domain độc lập để app cài đặt và IME cùng đọc một tài
liệu. Production lưu tại `filesDir/Shortcuts/shortcuts.json`, thuộc sandbox riêng của
Funput và được Android Auto Backup quản lý như các file app thông thường. Không có mạng,
analytics, import/export hoặc đồng bộ chéo nền tảng.

Schema version 1 tương thích logic với tài liệu iOS:

```json
{
  "schemaVersion": 1,
  "entries": [{ "id": "UUID", "trigger": "vn", "expansion": "việt nam" }],
  "isEnabled": true,
  "smartCase": true,
  "inEnglish": true
}
```

Thứ tự, UUID, Unicode, xuống dòng và khoảng trắng trong nội dung được giữ nguyên. Chỉ
trigger/nội dung rỗng sau trim bị từ chối; duplicate so sánh chính xác và phân biệt
hoa/thường. Store serialize theo đường dẫn, ghi temp + fsync + atomic replace và luôn
kiểm tra file hiện tại trước khi ghi, nên không ghi đè dữ liệu hỏng hoặc schema mới hơn.

## Lifecycle IME

Mỗi `onStartInput` huỷ load cũ, xoá bảng khỏi Rust và tạm tắt Gõ tắt. Snapshot mới được
đọc trên IO và chỉ nhận nếu còn đúng activation. Nếu kết quả đến giữa một từ, IME đợi
boundary/reset kế tiếp mới cài bảng. Vì vậy thay đổi trong app có hiệu lực khi người dùng
mở lại bàn phím, đúng thông báo trên màn hình.

Chỉ editor TEXT và SEARCH được dùng Gõ tắt. Password, PIN, number, phone, email và URL
không chạy. Trong tiếng Anh, ký tự vẫn được commit trực tiếp; khi Rust trả expansion,
IME kiểm tra lại suffix trước con trỏ rồi mới xoá đúng số UTF-16 code unit trong batch edit.
Nếu context không an toàn, trigger được giữ nguyên và chỉ boundary được chèn.

## Điểm mở rộng

UI chỉ phụ thuộc `ShortcutsStoring`, không phụ thuộc implementation bàn phím. Một store
khác cho import, export hoặc sync sau này có thể triển khai contract này, còn validation,
model versioned và lifecycle activation không phải thay đổi. Mọi migration schema tương
lai phải được thêm rõ ràng; bản hiện tại chủ động từ chối version chưa hỗ trợ.
