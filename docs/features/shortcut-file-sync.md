# Gõ tắt: quản lý bảng bằng file txt

> **Trạng thái: chưa chốt.** Đây là đề xuất và các phương án, chờ quyết định. Chưa có code.

Nguồn: issue #582 (Windows), mục "Về file viết tắt".

## Người dùng xin gì

Quản lý bảng gõ tắt giống Unikey:

- Funput dùng một file `.txt` mặc định, mỗi dòng dạng `ct:cơm tấm`. Sửa trong app thì ghi luôn vào file.
- Chọn file khác thì file đó thay file mặc định, hiện đường dẫn ở tab Dữ liệu, sửa trong app cũng ghi vào file đó.
- Khi chọn file, hỏi 3 lựa chọn: gộp với bảng hiện tại, xóa bảng hiện tại và ghi đè, hoặc hủy.
- Nút "Cập nhật" để nạp lại khi người dùng tự sửa file. Mặc định ghi đè bảng trong app.

Cách làm của người dùng: hơn 100 chữ tắt, quản lý trong Excel, chép sang file txt rồi bấm nạp lại. Nhu cầu thật là **giữ danh sách gõ tắt ở một dạng tự sửa được bằng công cụ của mình**, và nạp lại nhanh.

## Hiện trạng

- Bảng gõ tắt nằm trong file cấu hình JSON của Funput, cùng mọi cài đặt khác. Đây là bản gốc duy nhất.
- Windows có nút "Nhập từ UniKey" ([config.rs](../../platforms/windows/src/shared/commands/config.rs)). Nút này đọc `ukmacro.txt` qua `unikey::read_macro_file` ([unikey.rs](../../crates/funput-config/src/unikey.rs)), tự đoán bảng mã, từ chối UTF-8 hỏng, rồi **gộp** bằng `transfer::apply`: chữ tắt trùng lấy nội dung trong file, chữ tắt không có trong file vẫn giữ.
- Vì vậy cách làm của người dùng **đã chạy được bằng cách nhập lại**: thêm và sửa đều cập nhật. Chỉ có **xóa** là không: xóa một dòng trong file thì chữ tắt đó vẫn còn trong Funput.
- Chưa có cách xuất riêng bảng gõ tắt ra txt. Chỉ xuất được toàn bộ cấu hình dạng JSON.

## Phương án

### A. Nhập lại có chế độ Thay, cộng với xuất txt (đề xuất)

Bảng trong app vẫn là bản gốc. File txt chỉ là cửa nhập và cửa xuất.

- **Hộp thoại khi nhập:** *Gộp* (như hiện tại), *Thay toàn bộ bảng*, *Hủy*. Với Thay, hộp thoại nói rõ con số, ví dụ "Sẽ xóa 12 chữ tắt không có trong tệp".
- **Nhớ đường dẫn file đã nhập**, hiện ở tab Dữ liệu, kèm nút **"Nhập lại"** dùng lại chế độ đã chọn lần trước. Đây là nút "Cập nhật" người dùng muốn.
- **Xuất bảng gõ tắt ra txt** dạng `ct:cơm tấm`, UTF-8. Vòng Excel khép kín: xuất, sửa, nhập lại bằng Thay.

Ưu điểm: một bản gốc, không bao giờ ghi ra file của người dùng ngoài lúc họ bấm Xuất, dùng lại toàn bộ đường nhập hiện có. Nhược điểm: phải bấm "Nhập lại" sau khi sửa file, và sửa trong app không tự ra file.

### B. Liên kết một chiều

File txt người dùng chọn trở thành bản gốc trong lúc liên kết.

- Funput tự nạp lại khi file đổi (theo dõi file), hoặc khi bấm "Nhập lại". Nạp luôn theo kiểu Thay.
- Trong lúc liên kết, bảng trong app **chỉ đọc**, hiện "Đang dùng tệp X, sửa trong tệp". Muốn sửa trong app thì bỏ liên kết. Bảng giữ nguyên lần nạp cuối.
- Không bao giờ ghi ngược ra file.
- Nạp lỗi (bảng mã lạ, file đang bị khóa, file biến mất) thì giữ bảng cũ và báo, không xóa gì.

Ưu điểm: không phải bấm nạp lại, vẫn chỉ một bản gốc tại một thời điểm. Nhược điểm: thêm phần theo dõi file và trạng thái chỉ đọc, chỉ làm được trên desktop.

### C. Đồng bộ hai chiều như Unikey (không đề xuất)

Đúng như người dùng xin: file là nơi lưu, sửa trong app thì ghi ra file, sửa file thì nạp vào app.

## Vì sao không chọn C

1. **Định dạng quá nghèo để làm nơi lưu.** Một dòng `ct:cơm tấm` không chứa được nội dung nhiều dòng, mơ hồ khi chữ tắt có `:`, dễ mất khoảng trắng đầu và cuối, không có chỗ cho thông tin khác. Android đã gắn `id` cho từng dòng để gộp khi đồng bộ ([android-shortcuts.md](android-shortcuts.md)). Lưu bằng txt là khóa mình vào đúng những gì Unikey có.
2. **Excel dễ làm hỏng file.** "Save as Text" mặc định ghi ANSI hoặc UTF-16 và tách cột bằng Tab, tiếng Việt dễ thành `?`. Excel còn khóa file khi đang mở nên ghi ngược ra sẽ lỗi. Nếu file là nơi lưu, một lần lưu sai là mất bảng. Nếu file chỉ là cửa nhập, Funput đọc thử trước và từ chối khi thấy lạ.
3. **Hai bản gốc sẽ lệch nhau.** Sửa cả hai bên cùng lúc thì bên nào thắng, file bị xóa, đổi tên hay nằm trong OneDrive đang đồng bộ dở thì sao. Mỗi câu hỏi là một nhánh code và một loại lỗi khó tái hiện.
4. **Bảng mã của Unikey.** `ukmacro.txt` thường ở bảng mã cũ. Ghi ngược ra thì hoặc phải ghi lại đúng bảng mã đã đoán, hoặc lặng lẽ đổi file sang UTF-8 mà người dùng không biết.
5. **Không đi được sang nền tảng khác.** Gõ tắt có trên macOS, Linux, iOS và Android. Điện thoại không có đường dẫn file để liên kết, mô hình dữ liệu sẽ lệch giữa các nền tảng.
6. **Phục vụ số ít bằng rủi ro của số đông.** Đa số người dùng không mở file bao giờ. Đổi nơi lưu thì ai cũng gánh rủi ro ở điểm 2 và 3.

Điểm 1 và 5 cũng là giới hạn của B, nhưng B tránh được điểm 2, 3 và 4 vì không bao giờ ghi ra file.

## Đề xuất

Làm **A** trước. Nó phủ gần hết nhu cầu của người dùng có bảng lớn với ít rủi ro nhất. Nếu sau đó có thêm người xin không phải bấm "Nhập lại", làm thêm **B** trên nền A (đường nhập Thay và phần nhớ đường dẫn dùng lại được). Không làm **C**.

## Việc cần chốt

- Chọn A, B, hay A rồi B.
- Xuất txt: chỉ UTF-8, hay có lựa chọn ghi bảng mã cũ để Unikey đọc lại được.
- Định dạng xuất: chỉ `ct:cơm tấm`, hay cho thêm dạng tách bằng Tab để dán thẳng từ Excel.
- Có làm cùng lúc trên macOS và Linux không, hay Windows trước vì issue đến từ Windows.
