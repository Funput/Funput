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

pub enum Mode { Empty, Text, Files }
impl Mode { pub fn of(files: &[Entry], input: &str) -> Self; }

// Một đoạn văn bản. Phép chuyển là `charset::read` + `charset::render` của core —
// dùng chung với `funput convert`; ở đây chỉ còn câu chữ và bản cắt để hiển thị.
pub fn warning(cost: &Cost, from: Charset, to: Charset) -> String;
pub fn capped(text: &str) -> String;

// Một lô tệp
pub struct Entry { pub path: PathBuf, pub text: String,
                   pub charset: Option<Charset>, pub unmapped: usize }
pub fn collect(paths: &[PathBuf]) -> Vec<PathBuf>;
pub fn scan(paths: &[PathBuf]) -> Vec<Entry>;
pub fn measure(entries: &mut [Entry], target: Charset);
pub fn ready(entries: &[Entry]) -> usize;
pub fn out_dir_label(entries: &[Entry]) -> String;

// Ghi ra
pub const OUT_DIR: &str;        // "Đã chuyển mã"
pub struct Outcome { pub written: usize, pub skipped: usize, pub failed: usize }
pub fn write_all(entries: &[Entry], target: Charset) -> Outcome;
pub fn report(outcome: &Outcome) -> String;
```

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
