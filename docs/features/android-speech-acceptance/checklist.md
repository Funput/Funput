# Checklist P6 trên từng máy

Thực hiện riêng trên `samsung-a` và `fold-b`. L03/L04/L05 đã PASS theo người dùng
trên cả hai máy; các mục chưa có bằng chứng vẫn **PENDING**, xem report hiện tại.
EN nằm ngoài phạm vi theo yêu cầu người dùng ngày 03/10/2026. Ghi kết quả vào bản
checklist riêng trong `android-speech-spike/runs/`: ID, PASS/FAIL/NOT_RUN,
thiết bị, ngày giờ, bước đã làm và lỗi quan sát. Không cần lời nói cá nhân.
Các tests dùng fake backend đã pass chỉ bổ sung bằng chứng kỹ thuật.

## Lượt đầu ngắn

1. Chọn đúng IME test, ô TEXT A và VI. Nếu thiếu quyền, tap Chuẩn bị, cấp quyền,
   quay lại rồi tap mic lần nữa. Cấp quyền không được tự bắt đầu nghe.
2. Bật airplane, tắt Wi-Fi/dữ liệu. Tap mic, chờ Listening, đọc VI-C01:
   **Hôm nay trời mát và có một chút gió.** Bấm Dừng nếu chưa tự endpoint.
   Final chèn đúng một lần; preview biến mất; quay về bàn phím chữ.
3. Bắt đầu lại, nói một câu rồi Huỷ trước final: không chèn. Gõ Telex/VNI ngay
   sau đó vẫn bình thường. Đây chỉ là smoke, chưa thay 60 câu VI hoặc timing.

## Lifecycle và editor

| ID | Thao tác | Kết quả mong đợi |
| --- | --- | --- |
| L01 | Đang nghe, Back hoặc Huỷ | Không chèn; microphone ngừng; không tự mở panel lại |
| L02 | Đang Finalizing, Huỷ/Back | Final đến muộn không được chèn |
| L03 | Đang nghe, ẩn bàn phím hoặc ra Home | Mic ngừng; mở lại không tiếp tục phiên cũ |
| L04 | Đang nghe, khoá màn hình rồi mở khoá | Không còn phiên cũ; không chèn callback muộn |
| L05 | Đổi TEXT A → B trong khi nghe/chờ final | A/B không nhận final cũ; phiên mới cần tap mic |
| L06 | Đổi caret rồi quay lại vị trí cũ | Phiên cũ bị huỷ, không chèn dù caret giống ban đầu |
| L07 | Đổi editor/restart cùng app/field | Generation mới không nhận final cũ |
| L08 | Gõ/paste/emoji/suggestion/chuyển panel khi có phiên | Huỷ trước thao tác; không chèn vào vị trí đã thay đổi |
| L09 | Phím cứng: chữ, mũi tên, Ctrl/Alt | Huỷ trước forward, kể cả navigation không qua soft key |
| L10 | Rotate/configuration; Fold mở/đóng | Phiên kết thúc an toàn; layout phục hồi, không tự nghe |
| L11 | Kết thúc composition đang gõ rồi tap mic | Final chèn vào caret đúng, không lặp composing text |
| L12 | Chọn vùng chữ hoặc ô password/PIN/email/number/phone | Từ chối speech; không mở mic |
| L13 | Editor chỉ hỗ trợ KEY_EVENT | Chặn speech, gõ thường vẫn hoạt động |
| L14 | Stop khi chưa ready / đã ready; im lặng | Kết thúc hoặc lỗi có thể phục hồi, không chèn partial |
| L15 | Giữ phiên đến giới hạn 60 giây | Stop rồi đợi final tối đa 5 giây; không giữ mic vô hạn |
| L16 | Editor từ chối commit; final lặp/reentrant | Không retry hoặc chèn lặp; deterministic tests làm bằng chứng chính |
| L17 | Gõ sau thành công/Huỷ/lỗi | Viết hoa và engine bình thường; transcript không chạy qua gõ tắt/học từ |
| L18 | Process death khi đang nghe | Không tự resume/capture; phiên mới chỉ sau tap mic |

Agent có thể đọc AppOps của **đúng package test** sau các ca hide/lock/cancel
để đối chiếu mic đã ngừng. Timestamp/duration cũ không phải active recording;
phải đối chiếu Running và chỉ báo mic Android, tránh kết luận từ mỗi permission.
L18 dùng package test trên Fold5 nếu cần force-stop, không xoá dữ liệu app Play.

## Quyền, model và microphone

| ID | Thao tác | Kết quả mong đợi |
| --- | --- | --- |
| P01 | Từ chối/không hỏi lại quyền | Gõ thường bình thường; setup hướng dẫn phù hợp, không tự nghe |
| P02 | Quyền một lần rồi rời app | Lần sau kiểm tra lại quyền; không giả định vẫn được phép |
| P03 | Thu hồi quyền khi có phiên | Ngừng phiên; callback không chèn; có đường chuẩn bị lại |
| P04 | Tắt privacy microphone toggle | Ngừng/lỗi có thể phục hồi; không tiếp tục phiên cũ khi bật lại |
| P05 | Tắt setting giọng nói khi có phiên | Mic ngừng; toolbar phản ánh setting; gõ thường bình thường |
| P06 | EN thiếu model; chuẩn bị trong setup rồi kiểm tra lại | Ngoài phạm vi lượt nghiệm thu theo yêu cầu người dùng; chưa xác minh thực tế |
| P07 | Model pending/unsupported/Unknown, service lỗi | UI giải thích trạng thái, có kiểm tra lại; không chờ vô hạn |
| P08 | App khác đang dùng mic hoặc cuộc gọi | Báo lỗi/huỷ an toàn, gõ thường còn dùng được |
| P09 | Headset dây/Bluetooth, ngắt kết nối giữa phiên | Đo hành vi thật, không cam kết route không được kiểm chứng; không kẹt mic |
| P10 | VI thật sau chuẩn bị, airplane + Wi-Fi/data off | Smoke đã đạt cả hai máy; corpus/timing còn pending; EN ngoài phạm vi |

Không gỡ/xoá dữ liệu provider để giả lập thiếu model. API 31/33 khác biệt đã có
tests guard/classifier; hai máy hiện tại chỉ xác minh trực tiếp API 35/36.
Ghi rõ thiết bị/version giới hạn này trong report. Các nhánh pending/download
không quan sát được phải giữ NOT_RUN, không đánh dấu PASS từ code review.

## Giao diện và hồi quy thao tác

| ID | Thao tác | Kết quả mong đợi |
| --- | --- | --- |
| U01 | Có candidates; tắt Gợi ý từ | Mic cạnh Emoji vẫn hiện khi bật giọng nói |
| U02 | Toolbar hẹp, one-hand, floating | Giảm candidates/utility theo ưu tiên; hit target mic đủ dùng |
| U03 | Glass Light/Dark và theme tự tạo | Panel đồng bộ màu; chữ/controls đọc được; không đổi chiều cao |
| U04 | Font 200%, TalkBack | Không che nút; tên/trạng thái/action đúng; Back là Huỷ |
| U05 | Animation scale 0; ẩn/show panel | Orb không chuyển động khi tắt animation hoặc không visible |
| U06 | Fold5 màn ngoài/mở máy, portrait/landscape | Mic và controls còn truy cập được; không overflow |
| U07 | Clipboard, emoji, suggestions, gõ tắt, phím cứng sau Huỷ/lỗi | Giữ hành vi nhập thường; không học transcript |
| U08 | Preview/catalog fake states | Không tạo recognizer, xin quyền hoặc thu âm từ recomposition |

## Điều kiện ký nghiệm thu

Đủ hai corpus run VI rõ đạt ngưỡng cố định, báo cáo đầy đủ các nhóm VI, không
còn lỗi commit sai phiên/duplicate/mic giữ khi ẩn, và các mục checklist có bằng
chứng tương ứng. Ghi rủi ro thực tế, không suy ra PASS cho mục chưa chạy. Khi
tất cả gate đạt, chạy các cổng tự động lại nếu có sửa code, cập nhật help/privacy
và release notes, rồi mới đổi production gate. Không deploy trong P6.
