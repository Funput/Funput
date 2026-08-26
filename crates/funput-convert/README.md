# funput-convert

Ruột của cửa sổ **Chuyển mã**, trừ đi phần cửa sổ.

Mọi shell desktop có công cụ chuyển mã đều cần đúng bốn việc, và không việc nào là đồ hoạ:
đọc một lô tệp được thả vào **theo từng tệp**, nói ra phép chuyển sẽ tốn gì, ghi bản sao ra
mà không bao giờ đụng vào bản gốc, và thuật lại chuyện đã xảy ra. Crate này là bốn việc đó.

```
[shell: kéo-thả, clipboard, hộp thoại] → funput-convert → funput-core::charset
              tuỳ nền tảng                    thuần              thuần
```

## Vì sao nó ở đây chứ không ở trong một shell

Cửa sổ Windows có bốn việc này trước, viết bằng Rust thuần — không một dòng Win32, không
một dòng Slint. Chép chúng sang cửa sổ GTK sẽ đặt **câu cảnh báo mất chữ**, **luật
`vanban (2).txt`** và **tên thư mục đầu ra** vào hai chỗ cùng lúc, và
`platforms/linux/README.md` đã kể sẵn chuyện xảy ra tiếp theo: một cây quyết định gõ phím
từng tồn tại hai bản, mỗi shell một, rồi trôi khỏi nhau.

Tiền lệ cho phần chuỗi là `funput_core::charset::Charset::name()` — nó sống trong lõi
chính là để một menu trên Windows và một menu trên Linux không tự đặt tên lấy rồi lệch nhau.
Hai câu trong `warning()` cùng hạng: chúng là *phát biểu về tài liệu*, không phải chrome.

Còn một lợi ích thứ hai, lớn hơn: `platforms/windows` và `platforms/linux/settings-gtk` đều
nằm **ngoài** cargo workspace, nên `cargo clippy --workspace`, `cargo test --workspace` và
`scripts/check-loc.sh` không với tới cái nào. Ở đây thì cả ba đều với tới — ở mỗi pull
request, cho **cả hai** nền tảng.

## Cái gì ở lại trong shell

Hộp thoại tệp, clipboard, drop target, và cách nền tảng đó đẩy việc ra khỏi luồng UI.
Những thứ đó khác nhau theo bản chất; không thứ nào dưới đây khác nhau.

## API

```rust
pub use funput_core::charset;   // để consumer chỉ cần MỘT dependency

// Một kiểu, một truy vấn
pub struct Session;
impl Session {
    // sáu lệnh sửa — tám luật, mỗi luật một chỗ duy nhất
    fn set_input(&mut self, text: String);
    fn set_target(&mut self, index: usize);
    fn pick_source(&mut self, index: Option<usize>);
    fn pick_row_source(&mut self, row: usize, index: usize);
    fn adopt(&mut self, scan: Scan);
    fn set_row_window(&mut self, first: usize, len: usize);
    fn reset(&mut self);

    fn refresh(&mut self);      // làm việc — shell chọn luồng
    fn view(&self) -> &View;    // đọc — không bao giờ tính

    fn batch_job(&self) -> Job;             // lô, để mang ra khỏi luồng UI
    fn result_text(&self) -> Option<String>; // chép — TOÀN VĂN, không cắt
    fn save_bytes(&self) -> Option<Vec<u8>>;
}

#[non_exhaustive] pub struct View { mode, target, source, from_file, file_name,
    input_preview: Option<String>, output_preview, warning,
    rows: Vec<Row>, rows_first, rows_total, out_dir, ready, unreadable }
#[non_exhaustive] pub struct Row { name, charset: Option<usize>, note }
#[non_exhaustive] pub struct Unreadable { name, reason }
pub enum Mode { Empty, Text, Files }

// Việc I/O, trao ra để shell chọn luồng
pub fn scan(paths: &[PathBuf]) -> Scan;
pub struct Job;  impl Job { fn run(self) -> Outcome; }

// Câu chữ
pub fn warning(cost: &Cost, from: Charset, to: Charset) -> String;
pub fn unreadable_line(files: &[Unreadable]) -> String;
pub fn capped(text: &str) -> String;

// Chỉ số bảng mã — thứ duy nhất shell cần biết về Charset
pub fn charset_names() -> Vec<&'static str>;
pub fn at(index: usize) -> Charset;
pub fn index_of(charset: Charset) -> Option<usize>;

pub const OUT_DIR: &str;
#[non_exhaustive] pub struct Outcome;
pub fn report(outcome: &Outcome) -> String;
```

**Chỉ số, không phải `Charset`. `String`, không phải `PathBuf`.** Không trường nào của
`View` là dẫn xuất và không trường nào giữ borrow — đó không phải sự ngăn nắp mà là
điều kiện để một cửa C ABI trao nó ra từng accessor một, vì không reference nào qua
được ranh giới đó.

**`refresh()` và `view()` là hai lời gọi.** Đọc mượn `&self` nên shell giữ trong
`RefCell` chỉ cần borrow hẹp; khoảnh khắc tốn kém được gọi tên nên shell đặt nó lên
đúng luồng nó muốn — GTK và Slint không chọn giống nhau. Cái giá: sửa mà quên
`refresh()` thì view cũ. Cả hai shell đều đã kết thúc mọi callback bằng một lượt vẽ
lại, và có test ghim điều đó.

## Ba điều đáng biết trước

**`Mode` do nội dung quyết định, không do một nút chuyển chế độ.** Thứ người dùng bỏ vào đã
nói sẵn đây là một đoạn văn hay một lô tệp. Và **một tệp đi theo hình dạng text, không phải
bảng một hàng**: một tệp không có gì để so sánh, nên bảng sẽ giấu mất đúng thứ người dùng
muốn xem — tiếng Việt có ra đúng không.

**`warning()` là hai câu, không phải một.** Đọc có thể hỏng (ký tự bảng mã *nguồn* không định
nghĩa — nghĩa là chọn sai nguồn, hoặc tệp đã hỏng) và ghi có thể hỏng (ký tự bảng mã *đích*
không viết được — một cái giá để chấp nhận hoặc né). Chỉ báo cái thứ hai chính là thứ từng
để một phỏng đoán nguồn sai đi qua trong im lặng. Câu phía đích **gọi tên** ký tự, tối đa
sáu chữ: một con số nói cho người ta biết có gì đó sai mà không nói phải nhìn vào đâu.

**`write_all` không bao giờ ghi đè bản gốc.** Chúng thường là bản duy nhất còn lại. Bản sao
đi vào thư mục `Đã chuyển mã` cạnh tệp nguồn, và chạy lần hai ra `vanban (2).txt`. Tệp không
đoán được bảng mã thì **bỏ qua**, không đoán bừa.

## Test

15 test colocated, chạy bằng `cargo test -p funput-convert` (và bằng
`cargo test --workspace` ở CI). Chúng ghim những phán quyết dễ mất khi viết lại: thứ tự ưu
tiên của hai câu cảnh báo, luật đặt tên chống đè, "một cấp, không đệ quy" của `collect`, và
việc `normalized` **không** được đếm là mất mát.
