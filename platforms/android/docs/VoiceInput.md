# Nhập bằng giọng nói trên Android

**Hiện chỉ bật trong bản debug để nghiệm thu P6. Bản production chưa bật.**
Chi tiết kiểm chứng ở [P6](../../../docs/features/android-speech-acceptance/README.md).

## Cách dùng trong bản test

Cần Android 12/API 31 trở lên, dịch vụ nhận dạng on-device và model cho ngôn
ngữ đã chọn. VI dùng `vi-VN`, EN dùng `en-US`. Availability và model phụ thuộc
máy/provider; có Android mới không bảo đảm mọi locale sẵn sàng.

1. Bật Giọng nói trong tab Cài đặt của Funput. Mở một ô văn bản bình thường,
   đặt con trỏ tại một vị trí rồi tap mic cạnh Emoji.
2. Nếu cần chuẩn bị, dùng nút Chuẩn bị để cấp quyền hoặc kiểm tra/tải model.
   Tải model cần thao tác chủ động và có thể cần mạng. Sau khi cấp quyền, quay
   lại ô nhập và tap mic lần nữa; Funput không tự bắt đầu nghe.
3. Chờ trạng thái đang nghe rồi nói. Chữ xem trước chỉ nằm trong panel.
   **Dừng** yêu cầu final; dịch vụ cũng có thể tự kết thúc câu.
4. Final được trim hai đầu và chèn một lần vào ô đang nhập. Funput không tự
   thêm khoảng trắng, dấu câu hoặc viết hoa; provider có thể trả những ký tự
   đó trong kết quả. Nội dung không đi qua Telex/VNI, gõ tắt hoặc học từ.
5. **Huỷ/Back** bỏ phiên. Đổi ô nhập/con trỏ, gõ hoặc ẩn bàn phím sẽ kết thúc
   phiên; final cũ không được chèn sau đó. Tap mic lại để bắt đầu phiên mới.

Panel dùng theme bàn phím hiện tại. Ô mật khẩu/PIN/email/số/điện thoại, vùng
selection không thu gọn và editor chỉ hỗ trợ KEY_EVENT không dùng speech.
Phiên có giới hạn 60 giây, đợi ready/final có timeout; không nghe liên tục.

## Khi chưa nhận được

Kiểm tra quyền Microphone của đúng app, privacy microphone toggle Android và
trạng thái model trong Cài đặt → Giọng nói. Nếu model đang chuẩn bị, kiểm tra
lại sau; nếu không hỗ trợ, gõ bằng bàn phím. Không có fallback cloud hoặc đổi
locale tự động. Trên bản test song song, “Funput Speech Test” có quyền/model UI
riêng với app Funput Play Store; chọn đúng IME.

Lỗi dịch vụ, mic đang bận, headset hoặc quyền bị thu hồi có thể kết thúc phiên.
Sau lỗi/Huỷ, gõ thường vẫn hoạt động. Chất lượng tên riêng, số và môi trường
ồn cần kiểm chứng; báo lỗi theo thiết bị/API/provider thay vì gửi lời nói riêng.

## Bổ sung quyền riêng tư cho chức năng giọng nói — dự thảo

Funput dùng `SpeechRecognizer.createOnDeviceSpeechRecognizer` của Android,
không có đường nhận dạng generic/cloud fallback. Quyền `RECORD_AUDIO` phục vụ
phiên người dùng chủ động bắt đầu. Tính năng không tự nghe sau khi mở bàn phím,
cấp quyền, attach panel hoặc recomposition.

Funput không lưu audio hoặc transcript vào log, analytics, clipboard, storage
hay kho học từ. Preview tồn tại trong RAM của phiên và bị bỏ khi terminal;
final được gửi tới ứng dụng đang có ô nhập. Ứng dụng nhận văn bản có chính sách
lưu dữ liệu riêng. Funput không ghi âm để đo chất lượng.

Model và dịch vụ do Android/provider quản lý; tải model là thao tác riêng có
thể cần mạng. Mô tả này nói về code Funput, không thay chính sách dữ liệu của
provider hệ thống. Người dùng có thể tắt Giọng nói hoặc thu hồi quyền micro.
App manifest không có INTERNET/FGS microphone permission ở artifact đã audit;
việc này không thay thế kiểm chứng nhận dạng thực tế khi tắt mạng.

Đây là nội dung chuẩn bị trong repository; chưa cập nhật website quyền riêng
tư hoặc thông báo phát hành công khai. Chỉ publish sau nghiệm thu và theo quy
trình release riêng.

## Release notes dự thảo — chưa phát hành

- Thêm nhập bằng giọng nói VI/EN on-device trên Android 12+ khi máy có model
  tương ứng, với mic trên toolbar và panel đồng bộ theme Funput.
- Thêm chuẩn bị quyền/model trong Cài đặt; cấp quyền không tự thu âm.
- Xem trước lời nói trong panel; final chèn một lần. Dừng chờ kết quả, Huỷ bỏ
  phiên; đổi editor hoặc ẩn bàn phím không giữ phiên cũ.
- iOS, ML Kit và nhận dạng cloud chưa nằm trong bản này.

Ghi chú phát hành chỉ dùng sau khi gate P6 đạt. Không thay version hoặc deploy
từ tài liệu này.
