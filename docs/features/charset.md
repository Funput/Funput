# Chuyển mã (bảng mã tiếng Việt)

## Trạng thái

Thiết kế. Tài liệu này chốt mô hình trước khi có code; nó là PR đầu tiên của tính
năng và được review riêng.

Tên kỹ thuật là `funput_core::charset`, nằm sau cargo feature `charset` **mặc định
tắt** — bản bàn phím iOS/Android không bao giờ biên dịch nó.

## Mục tiêu

Chuyển **văn bản có sẵn** giữa các bảng mã tiếng Việt thông dụng. Bốn bảng mã:

- **Unicode dựng sẵn** — trục quy chiếu, và thứ mà mọi hệ thống hiện đại dùng.
- **TCVN3 (ABC)** — bảng mã của văn bản Nhà Nước cũ, đi kèm font `.VnTime`.
- **VNI-Windows** — đi kèm font `VNI-Times`.
- **Unicode tổ hợp** — theo quy ước UniKey, xem mục riêng bên dưới.

Ngoài nhu cầu người dùng, tính năng này có một khách hàng nội bộ: import bảng gõ
tắt UniKey hiện dừng ở `MacroError::UnknownEncoding` và bảo người dùng đi cài
UniKey để dùng được Funput.

## Không thuộc phạm vi

**Gõ thẳng ra bảng mã cũ.** Hệ sinh thái đã chuyển sang Unicode; nhu cầu còn lại là
cứu văn bản cũ, không phải soạn văn bản mới ở bảng mã cũ. Nếu đổi ý sau này, bảng
mã là một tầng mã hoá ở ranh giới inject và dùng lại được toàn bộ module này.

Cũng ngoài phạm vi: VIQR, VISCII, VPS, BK HCM · quản lý font · hotkey toàn cục ·
nghe clipboard tự động.

## Đầu vào là chuỗi Unicode, không phải byte

Đây là chỗ dễ thiết kế sai ngay từ đầu.

Khi người dùng copy một đoạn TCVN3 từ Word, clipboard **không chứa byte TCVN3**.
Word lưu mỗi ký tự thành một code point Unicode trong khoảng `U+0020..U+00FF`, và
font `.VnTime` mới là thứ vẽ ra glyph tiếng Việt. "Chuyển mã" ở đây thực chất là
*diễn giải lại những code point đó như byte TCVN3, rồi giải mã ra Unicode thật*.

Nên lõi làm việc trên `char`, và byte chỉ là một adapter ở hai đầu:

- `convert(&str, from, to) -> Conversion` — đường clipboard, `String → String`.
- `decode_bytes(&[u8], from) -> Conversion` — đường tệp, khi đầu vào thật sự là byte.

Latin-1 `byte ↔ char` là toàn phần và không mất mát theo cả hai chiều
(`char::from(u8)` / `c as u32 <= 0xFF`), nên adapter chỉ vài dòng. Chiều ngược lại —
lõi theo byte — sẽ buộc codec tổ hợp phải tự xử lý chuỗi UTF-8 nhiều byte trong khi
các codec cũ thì không, và bất đối xứng đó sẽ rò vào chỗ điều phối.

## Mô hình trục

Mọi phép chuyển đi qua Unicode dựng sẵn:

```text
TCVN3 ──decode──► Atom ──encode──► VNI-Windows
```

Chi phí thêm bảng mã thứ N là **2 ánh xạ**, không phải N. Với bốn bảng mã đó là 6
ánh xạ thay vì 12 cặp có hướng — và cái giá phải trả là mọi phép chuyển giữa hai
bảng mã cũ đều phải diễn đạt được qua Unicode. Với tiếng Việt điều đó luôn đúng:
Unicode biểu diễn được mọi chữ mà ba bảng mã kia biểu diễn được.

## Mô hình `Atom`

`Atom` là đơn vị trung gian — một "chữ cái tiếng Việt" đã tách khỏi mọi cách mã hoá:

```rust
enum Atom {
    /// Nguyên âm: chỉ số họ và thanh điệu dùng chung với VOWEL_FAMILIES.
    Vowel { family: u8, tone: u8, upper: bool },
    /// đ / Đ — phụ âm duy nhất mà cả ba bảng mã cũ đều gán mã riêng.
    Stroke { upper: bool },
    /// Mọi thứ khác: phụ âm ASCII, chữ số, dấu câu, ký tự nước ngoài.
    Passthrough(char),
}
```

`family` là chỉ số vào `VOWEL_FAMILIES` (12 họ), `tone` là `0` cho không dấu và
`1..=5` cho sắc, huyền, hỏi, ngã, nặng — **đúng thứ tự của `VOWEL_FAMILIES`**.

Ba biến thể đó đủ cho cả bốn bảng mã. Kiểm chứng trên giấy:

| Chữ | Unicode dựng sẵn | Atom | TCVN3 | VNI-Windows | Unicode tổ hợp |
|---|---|---|---|---|---|
| `a` | `a` | `Vowel{0, 0}` | `a` | `a` | `a` |
| `ầ` | `ầ` | `Vowel{2, 2}` | 1 byte vùng cao | `a` + 1 byte dấu | `â` + `U+0300` |
| `ự` | `ự` | `Vowel{10, 5}` | 1 byte vùng cao | `u` + 1 byte dấu | `ư` + `U+0323` |
| `Ề` | `Ề` | `Vowel{4, 2, upper}` | **không biểu diễn được** | `E` + 1 byte dấu | `Ê` + `U+0300` |
| `đ` | `đ` | `Stroke{}` | 1 byte | 1 byte | `đ` |
| `ng` | `ng` | 2× `Passthrough` | `ng` | `ng` | `ng` |
| `₫` | `₫` | `Passthrough` | **không biểu diễn được** | **không biểu diễn được** | `₫` |

Nguyên âm ASCII thường (`a e i o u y`) đi qua nhánh `Vowel` với `tone = 0`, không
qua `Passthrough` — vì VNI cần chúng làm chữ nền cho byte dấu đi sau, nên chúng phải
mang theo chỉ số họ.

## Bảng mã dùng chung chỉ số với `VOWEL_FAMILIES`

Cả ba bảng mã cũ đều gán mã cho đúng một kho chữ: 12 họ nguyên âm × 6 ô, cộng `đ`.
Nên mỗi bảng là `[[u8; 6]; 12]` — **12 dòng dữ liệu, không phải 67** — và chỉ số
của nó trùng với `VOWEL_FAMILIES` ở `unicode/vowels/table.rs`.

Điều đó cho một dòng canh trong mỗi tệp bảng:

```rust
const _: () = assert!(TCVN3_BYTES.len() == vowels::VOWEL_FAMILY_COUNT);
```

Ai đó thêm họ nguyên âm thứ 13 sẽ thành **lỗi build**, chứ không thành mã hoá sai
âm thầm. Đây là lý do chính khiến bảng mã sống trong `funput-core` thay vì một crate
riêng: `VOWEL_FAMILIES` là nguồn sự thật duy nhất về chữ cái tiếng Việt, và một bản
sao của nó ở nơi khác sẽ lệch đi.

Thứ tự thanh điệu nội bộ của TCVN3 và VNI **khác** thứ tự của `VOWEL_FAMILIES`. Viết
mảng theo thứ tự của `VOWEL_FAMILIES` và để việc sắp lại trong **chú thích**, không
trong code — chỉ số dùng chung mới là thứ giữ hai bảng khỏi trôi khỏi nhau.

### Nguồn số liệu byte

Tài liệu này cố tình **không liệt kê giá trị byte**. Chúng phải lấy từ nguồn tra cứu
được và đối chiếu chéo lúc hiện thực, không lấy từ trí nhớ — một byte chép sai sẽ
tạo ra mojibake mà mắt thường không bắt được trên văn bản dài.

Thứ chứng minh bảng đúng là **test quét toàn bộ** trong `src/`: với mọi bộ ba
`(family, index, upper)`, `encode` rồi `decode` phải là identity, **và** tập không
biểu diễn được phải đúng bằng tập đã tuyên bố (với TCVN3: nguyên âm hoa có dấu; với
VNI: rỗng). Chính khẳng định phủ định đó mới bắt được một byte lệch.

## Chữ hoa TCVN3

TCVN3 chỉ mã hoá **chữ thường**. Chữ hoa có dấu được vẽ bằng một font riêng
(`.VnTimeH`) ánh xạ đúng những byte chữ thường ấy sang glyph hoa. Bảng mã không mang
thông tin hoa/thường.

Hệ quả theo hai chiều:

- **Unicode → TCVN3**: `Ề` không biểu diễn được. Ghi ra byte chữ thường, đếm vào
  `unmapped`. Người dùng phải tự đổi sang font `H` cho phần đó.
- **TCVN3 → Unicode** (chiều chính, để cứu file cũ): kết quả **luôn là chữ thường**.
  Không có cách nào biết chữ gốc là hoa hay thường.

v1 **giữ chữ thường và không đoán**. API là `convert(text, from, to)` thuần, không
tham số tuỳ chọn. Nếu về sau cần "đoán viết hoa đầu câu" — văn bản Nhà Nước hay có
tiêu đề IN HOA — thì thêm `convert_with(text, from, to, &Options)`; đó là bổ sung
thuần, không phá API đã có.

Đây là giới hạn của bảng mã, không phải lỗi của công cụ. UI phải nói thẳng điều đó,
vì người dùng sẽ báo nó như một lỗi.

## Quy ước "Unicode tổ hợp"

Cụm từ này có hai cách hiểu, và chọn sai thì lệch khỏi thứ người dùng Việt Nam thật
sự gặp.

UniKey **chỉ tách thanh điệu**, giữ `ă â ê ô ơ ư` dựng sẵn. Đây **không phải** NFD
chuẩn, vốn tách cả hình dạng nguyên âm.

v1 **rộng rãi khi nhận, nghiêm ngặt khi xuất**:

- **Decode** nhận cả hai — dấu thanh rời (`U+0301` sắc, `U+0300` huyền, `U+0309` hỏi,
  `U+0303` ngã, `U+0323` nặng) **và** dấu hình dạng rời (`U+0302` mũ, `U+0306` trăng,
  `U+031B` móc). Nhờ vậy văn bản NFD thật — từ hệ tệp macOS, từ vài web form — cũng
  xử lý được.
- **Encode** luôn xuất theo quy ước UniKey: nguyên âm có hình dạng dựng sẵn, dấu
  thanh rời.

## Ngữ nghĩa `unmapped`

```rust
struct Conversion { text: String, unmapped: usize }
```

`unmapped` đếm số ký tự **không đi qua nguyên vẹn**. Một ký tự bị đếm vì một trong
hai lý do, và chỉ đếm một lần:

- **Bảng mã đích không viết được nó** — `Ề` trong TCVN3, hay `₫` trong bất kỳ bảng
  mã cũ nào.
- **Bảng mã nguồn chưa từng định nghĩa nó**, nên việc đọc chỉ là phỏng đoán.

Lý do thứ hai đáng giá hơn vẻ ngoài của nó. Đọc nhầm bảng mã thì hầu hết đơn vị đều
không nhận ra được, nên một con số gần bằng độ dài văn bản chính là **tín hiệu rõ
nhất rằng người dùng chọn sai bảng mã nguồn** — thứ mà một `unmapped` chỉ đếm phía
đích sẽ im lặng bỏ qua.

Bỏ qua nó còn giấu một chuyện khó chịu hơn. Byte `0xC2` không nằm trong TCVN3, nên
nó đi thẳng qua thành code point `U+00C2` — nhưng `Â` *là* chữ tiếng Việt, và mã
TCVN3 của nó là `0xA2`. Vòng `TCVN3 → Unicode → TCVN3` do đó **đổi byte**. Không thể
tránh, vì cùng một code point vừa là "byte lạ" vừa là "chữ thật"; nhưng đếm nó thì
biến một thay đổi âm thầm thành một con số nhìn thấy được.

Hợp đồng của `encode` là: **luôn ghi ra thứ gì đó**, và trả `false` khi thứ vừa ghi
không chính xác. Không có trường hợp nào ký tự biến mất.

Dạng gần nhất theo thứ tự ưu tiên:

1. Cùng chữ, mất thông tin hoa/thường — `Ề` → byte TCVN3 của `ề`.
2. Không có gì gần: giữ nguyên ký tự Unicode gốc — `₫` đi thẳng vào đầu ra.

Lý do chọn "ghi dạng gần nhất" thay vì bỏ: đầu ra được dán vào một tài liệu font cũ.
Một chữ thường hiển thị đúng thì người dùng sửa được; một ký tự Unicode thô nằm giữa
văn bản `.VnTime` là rác câm. Và `unmapped` cho họ biết chính xác còn bao nhiêu chỗ
cần để mắt tới.

## Hợp đồng mở rộng

Mỗi bảng mã là một module codec cung cấp đúng hai hàm:

```rust
/// Giải mã một đơn vị nguồn tại con trỏ. Trả về atom, số char đã tiêu (>= 1, để
/// driver luôn tiến), và `recognized` — false khi bảng mã này không định nghĩa
/// đơn vị đó. Giải mã là toàn phần: không nhận ra thì ký tự vẫn đi qua nguyên vẹn.
pub(super) fn decode(cur: &Cursor<'_>) -> Decoded;

/// Ghi biểu diễn của `atom`. Trả false khi thứ vừa ghi không chính xác.
pub(super) fn encode(atom: Atom, out: &mut String) -> bool;
```

Điều phối bằng `match Charset` trong `codecs/mod.rs`, **không dùng trait**:
`funput-core` hiện có 0 trait và điều phối toàn bộ bằng match trên enum
(`input_method/mod.rs` là tiền lệ trực tiếp). Lợi ích thật ở đây là **exhaustiveness**
— thêm một biến thể `Charset` sinh ra lỗi biên dịch chỉ đúng hai nhánh còn thiếu.
`#[non_exhaustive]` không ảnh hưởng match trong cùng crate, nên bảo đảm đó vẫn còn.

`Charset::Unicode` không cần module riêng — nó là nhánh identity của cả hai chiều.

Thêm một bảng mã nghĩa là: một biến thể `Charset`, một thư mục codec, hai nhánh
match. Trục, driver và `detect` không đổi.

## API công khai

```rust
#[non_exhaustive] pub enum Charset { Unicode, /* … */ }
#[non_exhaustive] pub struct Conversion { pub text: String, pub unmapped: usize }

pub fn convert(text: &str, from: Charset, to: Charset) -> Conversion;
pub fn decode_bytes(bytes: &[u8], from: Charset) -> Conversion;
```

`detect(text) -> Option<Charset>` đến ở PR sau. Nó chấm điểm bằng cách thử giải mã
theo từng bảng mã rồi đếm xem kết quả cho ra bao nhiêu âm tiết tiếng Việt hợp lệ,
dùng `is_complete_syllable` sẵn có — giải mã sai bảng mã cho ra rác, và rác thì trượt
bộ kiểm tra chính tả gần như hoàn toàn. Dưới ngưỡng chắc chắn thì trả `None` thay vì
đoán bừa.

Module nằm trong không gian tên riêng (`funput_core::charset::…`), **không** re-export
ra gốc crate: khối "API FROZEN (Phase 8)" ở `lib.rs` liệt kê bề mặt cho
`funput-engine`, và danh sách đó phải tiếp tục đúng cho bản build mặc định.

## Thứ tự hiện thực

Mỗi mục một PR:

1. Tài liệu này.
2. Khung core + **TCVN3**. Phải kèm một bảng mã thật, vì CI chạy `-D warnings` và một
   trục chỉ có codec identity là dead code. TCVN3 được chọn vì nó dùng tới cả adapter
   Latin-1 lẫn đường `unmapped`, tức kiểm chứng nhiều phần khung nhất.
3. VNI-Windows → 4. Unicode tổ hợp → 5. `detect()`.
6. `funput-config` (sửa import UniKey) → 7. `funput convert` → 8. UI Windows →
   9. UI Linux GTK.
