# Hiện thực Gõ tắt trên Android tương đồng iOS

## Tóm tắt

- Port đầy đủ trải nghiệm iOS: bật/tắt, tìm kiếm, thêm/sửa/xóa, tự nhận diện hoa/thường và hoạt động khi dùng tiếng Anh.
- Giữ Rust `funput-engine` là nguồn chân lý duy nhất cho matching, smart case và expansion; Android chỉ đảm nhiệm lưu trữ, lifecycle, UI và chỉnh sửa `InputConnection`.
- Dùng Material 3 cho control Android nhưng giữ cùng mô hình màn hình, nội dung, validation và trạng thái lỗi của iOS.
- Không thêm import/export, đồng bộ hai nền tảng hoặc thay đổi hành vi gõ hiện hữu ngoài Gõ tắt.

## Kiến trúc và dữ liệu

- Thêm module Android `:shortcut-store`, được `app` và `ime` phụ thuộc trực tiếp, để UI không phụ thuộc implementation của bàn phím và có thể tái sử dụng cho import/sync sau này.
- Public domain contract gồm:
  - `TextShortcut(id: UUID, trigger: String, expansion: String)`.
  - `ShortcutLibrary(schemaVersion, entries, isEnabled, smartCase, inEnglish)`.
  - Interface lưu trữ bất đồng bộ để production và test có thể thay thế implementation.
  - Kiểu lỗi phân biệt unavailable, read, write, invalid data, unsupported version và duplicate trigger.
- Lưu tài liệu riêng tại `filesDir/Shortcuts/shortcuts.json`, dùng schema version 1 và field tương thích tài liệu iOS. Dữ liệu nằm trong vùng private có Android Auto Backup, độc lập với DataStore cài đặt.
- Ghi file bằng temp file, flush/fsync và atomic replace; serialize truy cập theo đường dẫn. Trước khi ghi phải đọc/kiểm tra file hiện tại để không ghi đè dữ liệu hỏng hoặc schema mới hơn.
- File chưa tồn tại trả về thư viện mặc định: ba tùy chọn đều bật. Giữ nguyên thứ tự, UUID, Unicode, nội dung nhiều dòng và khoảng trắng người dùng nhập.
- Trigger và expansion chỉ không được rỗng sau khi trim. Duplicate trigger so sánh chính xác, phân biệt hoa/thường như iOS; `vn` và `VN` có thể cùng tồn tại.
- Model màn hình serialize mọi mutation, chặn thao tác chồng lấn, rollback toggle khi lưu thất bại, giữ snapshot/draft/query khi lỗi và khóa ghi cho tới khi reload thành công.

## UI/UX Android

- Thêm navigation row “Gõ tắt” sau “Kiểu đặt dấu” trong nhóm “Gõ tiếng Việt”; thêm destination `SHORTCUTS` độ sâu 1 và hỗ trợ back/predictive back hiện có.
- Màn hình chính gồm:
  - Top app bar với Back, Tuỳ chọn và Thêm.
  - Công tắc “Bật gõ tắt”, giải thích cách kích hoạt và ghi chú thay đổi có hiệu lực khi mở lại bàn phím.
  - Ô tìm kiếm không phân biệt hoa/thường trên trigger và expansion.
  - Danh sách giữ thứ tự đã lưu, số lượng kết quả, trạng thái rỗng/không tìm thấy/loading/lỗi và Retry.
  - Swipe action xóa nhưng vẫn bắt buộc dialog xác nhận; chạm row để sửa.
- Editor dùng Material modal bottom sheet toàn chiều cao:
  - Trigger một dòng, tắt autocorrect/auto-capitalization.
  - Expansion nhiều dòng.
  - Hiển thị duplicate inline; Save chỉ bật khi draft hợp lệ và khác biệt được phép.
  - Xóa khi đang sửa phải xác nhận.
  - Back, swipe hoặc chạm ngoài khi có thay đổi phải hỏi bỏ thay đổi; không cho dismiss trong lúc đang lưu.
- Tuỳ chọn dùng bottom sheet với hai switch “Tự nhận diện hoa/thường” và “Gõ tắt khi dùng tiếng Anh”; lưu ngay, rollback và báo lỗi nếu thất bại.
- Dùng string resources, test tags ổn định, semantic label/hint, vùng chạm tối thiểu Material và layout phù hợp font lớn, màn hình hẹp, bàn phím đang mở.

## Tích hợp IME và Rust

- Mở rộng `funput-jni` và `VietnameseEngine` với các thao tác clear/add shortcut và ba option hiện đã có trong Rust; không sao chép thuật toán matching sang Kotlin.
- Mỗi `onStartInput`:
  - Hủy generation load cũ, xóa bảng shortcut khỏi engine và tạm tắt Gõ tắt để không mang dữ liệu của activation trước.
  - Đọc snapshot trên `Dispatchers.IO`.
  - Chỉ nhận kết quả thuộc activation mới nhất; lỗi đọc giữ Gõ tắt tắt và chỉ log loại lỗi, không log nội dung người dùng.
- Nếu snapshot đến giữa một từ, giữ nó pending và chỉ cài vào engine sau boundary/reset tiếp theo. Không thay đổi bảng shortcut giữa lúc người dùng đang gõ.
- Tách rõ:
  - `usesVietnameseComposition`: hành vi Telex/VNI hiện tại.
  - `usesEngine`: composition tiếng Việt hoặc Gõ tắt tiếng Anh đang bật và có entry.
- Ở chế độ tiếng Anh, giữ cách nhập trực tiếp và luồng gợi ý hiện tại; Rust chỉ theo dõi raw word. Khi Rust trả expansion:
  - Xác minh text trước con trỏ thực sự kết thúc bằng trigger đang theo dõi.
  - Xóa đúng độ dài UTF-16 trong batch edit rồi commit expansion kèm boundary.
  - Nếu context không an toàn hoặc không đọc được, không xóa nội dung; giữ trigger, chèn boundary và reset state.
- Chỉ chạy Gõ tắt trong editor `TEXT` và `SEARCH`, không chạy trong password, PIN, number, phone, email hoặc URL.
- Cursor/selection thay đổi, chuyển ngôn ngữ, focus mới, external insertion, kết thúc input và reset composition đều kết thúc trigger hiện tại. Backspace phải đồng bộ cả document và state Rust.
- Expansion không được làm hệ thống gợi ý học nhầm trigger hoặc giữ prefix cũ; ngoài trường hợp đó hành vi học/gợi ý hiện tại không đổi.
- Thay đổi từ app chỉ áp dụng ở lần mở/activation bàn phím tiếp theo, đúng thông báo và lifecycle của iOS.

## Chất lượng, kiểm thử và tài liệu

- Test module lưu trữ: default, round-trip schema, Unicode dài/nhiều dòng, order/UUID/options, duplicate/invalid data, unsupported/corrupt file, atomic-write failure, concurrent store instances và bảo toàn file cũ.
- Test model: CRUD, search, case-sensitive duplicate, serialized writes, rollback, retry sau lỗi đọc, draft isolation và option persistence.
- Compose tests: row nằm đúng nhóm, navigation/back, loading/error/empty/search, CRUD, duplicate validation, discard/delete confirmation, option toggles, lỗi lưu, accessibility semantics và font lớn.
- JNI/IME tests:
  - Cài/xóa bảng và ba option đi đúng tới Rust.
  - Telex, VNI, Telex nâng cao, tiếng Anh bật/tắt, smart case lower/title/upper/exact.
  - Space và punctuation boundary, Unicode/emoji/multiline expansion.
  - Password và các editor không hợp lệ không expansion.
  - Context không an toàn không xóa nhầm.
  - Late load, stale generation, activation mới, cursor/language/backspace/external text reset đúng state.
  - Gợi ý không học trigger và không bị thay đổi ngoài expansion.
- Connected tests bao phủ luồng Settings → Gõ tắt → thêm/sửa/xóa/tùy chọn và một luồng nhập thực tế qua IME.
- Thêm các source root mới của `shortcut-store`, `app/ui/shortcuts` và `ime/shortcuts` vào `check-kotlin-layout.sh`; chia subpackage để mọi thư mục tối đa 5 file Kotlin và mọi file tối đa 150 dòng.
- Cập nhật README/architecture diagram, mô tả schema, lifecycle “áp dụng khi mở lại bàn phím”, privacy và điểm mở rộng cho store/import trong tương lai.
- Gate cuối:
  - `cargo test -p funput-engine -p funput-jni`
  - `./gradlew testDebugUnitTest`
  - `./gradlew :app:connectedDebugAndroidTest :ime:connectedDebugAndroidTest`
  - `./gradlew assembleDebug lintDebug`
  - `scripts/check-kotlin-loc.sh`
  - `scripts/check-kotlin-layout.sh`

## Giả định đã chốt

- “Tương đồng iOS” bao gồm toàn bộ CRUD, search và ba tùy chọn hiện có, nhưng dùng component Material 3.
- Android và iOS dùng cùng schema logic nhưng không chia sẻ file vật lý; chưa có import/export hoặc cloud sync.
- Không migration vì Android chưa từng phát hành dữ liệu Gõ tắt.
- Không sửa semantics trong `funput-engine`; nếu test Android phát hiện sai lệch engine thật sự thì tách thành fix dùng chung và bổ sung regression test Rust.
