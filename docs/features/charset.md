# Chuyển mã (bảng mã tiếng Việt)

## Trạng thái

Đã xong toàn bộ danh sách ở "Thứ tự hiện thực" cuối tài liệu: khung core, cả bốn bảng
mã, `detect()`, `funput-config`, C ABI, `funput convert`, và hai cửa sổ — Windows
(Slint) và Linux (GTK4). Ruột của hai cửa sổ nằm ở **`crates/funput-convert`**, không
phải ở từng shell; xem mục 11. Còn lại là bảng mã mới, nếu có.

Tài liệu này chốt mô hình *trước* khi có code và được review riêng; nó vẫn là nơi
mọi quyết định thiết kế sống, nên mỗi bảng mã mới cập nhật lại nó trong cùng PR.

Tên kỹ thuật là `funput_core::charset`, nằm sau cargo feature `charset` **mặc định
tắt** — bản bàn phím iOS/Android không bao giờ biên dịch nó.

## Mục tiêu

Chuyển **văn bản có sẵn** giữa các bảng mã tiếng Việt thông dụng. Bốn bảng mã:

- **Unicode dựng sẵn** — trục quy chiếu, và thứ mà mọi hệ thống hiện đại dùng.
- **TCVN3 (ABC)** — bảng mã của văn bản Nhà Nước cũ, đi kèm font `.VnTime`.
- **VNI-Windows** — đi kèm font `VNI-Times`.
- **Unicode tổ hợp** — theo quy ước UniKey, xem mục riêng bên dưới.

Ngoài nhu cầu người dùng, tính năng này có một khách hàng nội bộ: import bảng gõ
tắt UniKey **từng** dừng ở `MacroError::UnknownEncoding` và bảo người dùng đi cài
UniKey để dùng được Funput. Đã sửa.

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
    Other(char),
}
```

`family` là chỉ số vào `VOWEL_FAMILIES` (12 họ), `tone` là `0` cho không dấu và
`1..=5` cho sắc, huyền, hỏi, ngã, nặng — **đúng thứ tự của `VOWEL_FAMILIES`**.

Ba biến thể đó đủ cho cả bốn bảng mã. Kiểm chứng trên giấy:

| Chữ | Unicode dựng sẵn | Atom | TCVN3 | VNI-Windows | Unicode tổ hợp |
|---|---|---|---|---|---|
| `a` | `a` | `Vowel{0, 0}` | `a` | `a` | `a` |
| `ầ` | `ầ` | `Vowel{2, 2}` | 1 byte vùng cao | `a` + 1 byte dấu | `â` + `U+0300` |
| `ự` | `ự` | `Vowel{10, 5}` | 1 byte vùng cao | 1 byte nền `ư` + 1 byte dấu | `ư` + `U+0323` |
| `Ề` | `Ề` | `Vowel{4, 2, upper}` | **không biểu diễn được** | `E` + 1 byte dấu | `Ê` + `U+0300` |
| `đ` | `đ` | `Stroke{}` | 1 byte | 1 byte | `đ` |
| `ng` | `ng` | 2× `Other` | `ng` | `ng` | `ng` |
| `₫` | `₫` | `Other` | **không biểu diễn được** | **không biểu diễn được** | `₫` |

Nguyên âm ASCII thường (`a e i o u y`) đi qua nhánh `Vowel` với `tone = 0`, không
qua `Other` — vì VNI cần chúng làm chữ nền cho byte dấu đi sau, nên chúng phải
mang theo chỉ số họ.

## Bảng mã dùng chung chỉ số với `VOWEL_FAMILIES`

Cả ba bảng mã cũ đều gán mã cho đúng một kho chữ: 12 họ nguyên âm × 6 ô, cộng `đ`.
Nên mỗi bảng là **12 dòng dữ liệu, không phải 67**, và chỉ số của nó trùng với
`VOWEL_FAMILIES` ở `unicode/vowels/table.rs`. Ô chứa gì thì tuỳ bảng mã: TCVN3 là
`[[u8; 6]; 12]` (một mã một chữ), VNI-Windows là `[[(u8, u8); 6]; 12]` (byte nền
cộng byte dấu tuỳ chọn).

Điều đó cho một dòng canh trong mỗi tệp bảng:

```rust
const _: () = assert!(TCVN3_BYTES.len() == vowels::FAMILY_COUNT);
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

Một lưu ý thực hành, học được khi làm VNI: **đừng lấy dữ liệu qua công cụ tóm tắt
văn bản.** Hai lần thử đầu cho ra bảng lệch hàng và hex bịa, tự mâu thuẫn ngay trong
cùng một câu trả lời. Cách dùng được là kéo byte thô về (`gh api` + `base64 -d`) rồi
tự ghép hai mảng bằng script — và **sinh luôn bảng Rust từ dữ liệu đó**, không chép
tay. Chi tiết xuất xứ của từng bảng nằm trong doc của `table.rs` tương ứng.

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

## VNI-Windows: một chữ không phải một ký tự

VNI viết hầu hết chữ bằng **byte nền cộng byte dấu**, nên chuyển mã làm **dịch
ranh giới ký tự**: `Ề` đi ra thành hai. Đây là bảng mã đầu tiên phá tính chất
"một chữ một ký tự" mà TCVN3 có, nên property test về số ký tự phải nói rõ nó chỉ
đúng với TCVN3.

Ba bộ dấu làm hết việc: *plain* `F9 F8 FB F5 EF`, *mũ* `E2` + `E1 E0 E5 E3 E4`
(dùng chung cho `â ê ô`, phân biệt nhờ chữ nền), *trăng* `EA` + `E9 E8 FA FC EB`
riêng cho `ă`. Hai họ `ơ`/`ư` có byte nền riêng (`F4`/`F6`) thay vì dựng trên `o`/`u`.

**Hoa = mọi byte trừ `0x20`**, không ngoại lệ. Nên bảng chỉ lưu chữ thường, và
**VNI không có khoảng trống hoa/thường nào** — nó viết được cả `Ề`, chỗ TCVN3 chịu
thua. Tập mất mát của VNI trên kho chữ là **rỗng**.

Hai ngoại lệ trong dữ liệu, là chuyện của VNI chứ không phải của bảng: họ `i` viết
mọi thanh điệu bằng **một byte**, và riêng `ỵ` cũng vậy trong một họ mà các thanh
khác thì không.

### Đọc tham lam, và vì sao nó an toàn

Giải mã lấy **khớp dài nhất**: thử cặp hai byte trước, rồi mới tới một byte. An toàn
vì **byte dấu không bao giờ là một chữ đứng riêng** — tập nền và tập dấu rời nhau,
nên một cặp đã khớp thì không thể vốn là hai chữ.

Cách làm ngược lại — xử lý chữ một byte trước — trông tương đương nhưng **không**:
`F4` vừa là chữ `ơ`, vừa là nền của cả năm dạng có dấu của nó, nên `ơ` sẽ bị lấy
riêng và mọi `ớ ờ ở ỡ ợ` gãy làm đôi.

### Hai chỗ nó từ chối đoán

**Cặp lệch hoa/thường** (`a` kèm byte dấu hoa) không phải thứ VNI sinh ra, nhưng bộ
chuyển đổi cẩu thả thì có. Đọc thành chữ, lấy case theo chữ nền, và **đếm vào
`unmapped`** — cách đọc mà con người sẽ đọc, không sinh ra nguyên âm Latin-1 ma rồi
nở thành hai byte nữa.

**Họ `i` chỉ nhận byte đơn.** Vài bộ chuyển đổi ngoài đời viết `í` thành `i` + dấu
sắc; ở đây nó đọc thành `i` rồi một dấu mồ côi, và bị đếm. Nhận cả hai cách viết sẽ
cho một chữ hai cách viết, và bài test quét toàn kho chữ mất sức bắt lỗi bảng.

### Một kiểu hỏng TCVN3 không có

Ký tự đi xuyên qua (`Other`) mà code point của nó tình cờ là một byte dấu sẽ **dính
vào chữ đứng trước** khi đọc lại: Unicode `"aø"` ra `61 F8`, đọc lại thành `à`.
Không tránh được, vì `ø` phải đi qua nguyên vẹn theo đúng quy tắc "dạng gần nhất".
`unmapped` khác 0 nên hợp đồng "chính xác thì đảo ngược được" vẫn đứng — và có một
bài test ghim nó lại như hành vi **đã biết**, thay vì để ai đó phát hiện ngoài đời.

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

### Thứ tự dấu không cố định — và một cái bẫy chí mạng

NFD chuẩn **không** luôn đặt dấu hình dạng trước. Bốn chữ đặt dấu thanh trước, vì
`U+0323` (nặng) có combining class 220 còn mũ/trăng là 230:

```text
ặ = 0061 0323 0306      ậ = 0061 0323 0302
ệ = 0065 0323 0302      ộ = 006F 0323 0302
```

Mọi trường hợp hai dấu khác đều hình-dạng-trước (`ợ` = `006F 031B 0323`). Vì chính
thứ tự đã lật, **nhận dấu theo thứ tự bất kỳ** vừa ít việc hơn vừa đúng hơn là mã
hoá lại luật combining class.

Cái bẫy: cách hiện thực hiển nhiên là gộp lần lượt từng dấu vào một `char`. Nó
**hỏng**. `apply_shape_to_vowel` gọi `base_vowel`, mà hàm đó bóc **cả thanh lẫn hình
dạng** — test của chính crate tên là `apply_shape_to_vowel_strips_tone`. Gộp NFD của
`ậ` sẽ ra `ạ` rồi `â`, mất dấu nặng. Trúng đúng bốn chữ trên, tức là `Việt`, `một`,
`cộng`, `nặng`.

Nên codec tích dấu vào **các ô rời** rồi ghép **một lần** ở cuối. Và **ô đã đầy thì
kết thúc chữ**: `apply_tone` *thay* dấu chứ không từ chối, nên nhận dấu thanh thứ hai
sẽ âm thầm nuốt mất dấu thứ nhất.

### Chữ nào là "đúng chính tả" của bảng mã này

```text
Exact khi: hình dạng (nếu có) nằm sẵn trong ký tự nền
       và  thanh điệu (nếu có) đến từ một dấu rời
```

`ấ` dựng sẵn không đạt (thanh chưa tách), NFD `a`+`0302`+`0301` cũng không (hình dạng
bị tách). Cả hai vẫn đọc đúng, vẫn được viết lại — và vào `normalized`, **không** vào
`unmapped`. Hệ quả phải biết trước: **đọc văn bản dựng sẵn thông thường bằng bảng mã
này cho `normalized` ≈ một trên mỗi chữ có dấu.** Đúng như vậy; `detect()` phải tính
đến điều đó.

### Thứ duy nhất nó không viết được

Không có gì — đây là Unicode, nên `₫` và `日` đi qua chính xác, điều mà không bảng mã
cũ nào làm được. Ngoại lệ duy nhất là **một dấu rời đi lạc**: viết nó sau một chữ thì
hai thứ dính vào nhau và văn bản đọc lại thành chữ khác. Đó là bản sao của ca `ø`
dính dấu bên VNI.

## Hai bộ đếm

```rust
struct Conversion { text: String, unmapped: usize, normalized: usize }
```

Hai chuyện khác hẳn nhau, nên hai con số. Một ký tự rơi vào **nhiều nhất một** ô, và
mất mát thắng chuẩn hoá.

**`normalized`** — hiểu chắc chắn, không mất gì, **chỉ cách viết đổi**. Đọc văn bản
dựng sẵn hay NFD bằng bảng mã tổ hợp rơi vào đây. Gộp nó vào `unmapped` sẽ khiến giao
diện báo "N ký tự không chính xác" cho một tài liệu hoàn toàn lành lặn.

**`unmapped`** — có thứ bị **mất hoặc phải đoán**. Một ký tự bị đếm vì một trong hai
lý do:

- **Bảng mã đích không viết được nó** — `Ề` trong TCVN3, hay `₫` trong bất kỳ bảng
  mã cũ nào.
- **Bảng mã nguồn chưa từng định nghĩa nó**, nên việc đọc chỉ là phỏng đoán.

Bên trong, `Decoded` mang `enum Reading { Exact, Rewritten, Unknown }` thay cho một
cờ bool — đó là thứ cho driver biết đếm vào ô nào.

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

## Hai bước, không phải một

`convert` là **một lượt**: `nguồn → Atom → đích`. Đúng cho một chuỗi, sai cho một cửa
sổ, vì hai lý do.

**Cửa sổ cho xem trước khi ghi.** Byte một tệp nhận được **không** phải ký tự khung xem
hiển thị: `encode_bytes` thu hẹp thứ không lọt vào một byte thành `?`. Cửa sổ nào xem
bằng một lời gọi và lưu bằng một lời gọi khác thì đang hiện thứ tệp sẽ không chứa. Nên
`render` trả **cả hai**, làm ra cùng lúc, và việc chúng khớp nhau là chuyện cấu trúc
chứ không phải một quy ước ai đó phải nhớ giữ.

**Cửa sổ đổi bảng mã đích liên tục.** Đọc nguồn là nửa tốn kém và nó không phụ thuộc
đích, nên tách ra: `read` một lần, `render` mỗi đích.

Việc tách còn sửa một chỗ tinh tế hơn. Một lượt `nguồn → đích` và hai bước
`nguồn → Unicode → đích` **không phải cùng một phép chuyển**: vòng qua chuỗi Unicode
thật thêm một `Atom::from_char(atom.to_char())`, vốn là identity với chữ mà bảng mã
nguồn có định nghĩa, và **không** phải với mã nó không định nghĩa. `0xC2` của TCVN3 đi
qua thành `Â`, mà mã TCVN3 thật của `Â` là `0xA2`. Ca VNI còn lệch cả độ dài: `0xFD` ra
`ý`, viết lại thành hai byte.

Trước khi có tầng này, cửa sổ Windows xem bằng đường một lượt và ghi bằng đường hai
bước — nên nó **hiển thị một đằng, lưu một nẻo**, đúng lúc người dùng cần tin vào khung
xem nhất. Có một property test trong `tests/charset/properties.rs` ghim điều duy nhất
khiến việc gộp chúng an toàn: hai đường chỉ khác nhau ở chỗ phép **đọc đã đếm**, nên
đổi đường không bao giờ dịch chuyển một ký tự mà người dùng chưa được cảnh báo.

### `Cost` mang số, không mang câu

Hai consumer cần cùng phép đo và diễn đạt khác nhau: cửa sổ **gọi tên** ký tự bằng
tiếng Việt, terminal **đếm** chúng bằng tiếng Anh. Nên `Cost` chỉ mang số và danh sách
`lost`, còn câu chữ sống ở từng consumer — `funput_convert::warning()` cho hai cửa sổ,
`funput-cli/src/convert/report.rs` cho dòng lệnh.

Hai con số về đích, cố ý tách: `lost.len()` là số chữ **riêng biệt** không viết được
(thứ một menu gọi tên), `unrepresentable` là số **lần xuất hiện** (thứ một terminal
đếm). `normalized` đếm ở phía **đọc**, vì đó là phía duy nhất thấy được nó — phép ghi
luôn xuất phát từ Unicode dựng sẵn, vốn chỉ có một cách viết.

### Máy trạng thái của cửa sổ ở một chỗ

Tám luật — kẹp chỉ số, "dán mới thì quên nguồn cũ", "người dùng chọn thắng máy đoán",
"picker rơi vào tệp đơn đang mở", chọn nguồn theo hàng, chọn `(text, from, to)`, hình
dạng theo nội dung, và chính struct trạng thái — từng tồn tại hai bản, một trong shell
Slint và một trong shell GTK. Giờ chúng ở `funput_convert::Session`, và shell chỉ đọc
một `View` gồm toàn giá trị phẳng.

Điều đó bắt được một lỗi thật: `save()` bên Windows đọc ô văn bản dán, vốn rỗng ở hình
dạng một-tệp, nên nó chuyển sai thứ — trong khi `copy` cùng tệp thì đúng. Cùng một
luật, ba chỗ viết, sai một chỗ.

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
match, một tên trong `Charset::name`, một mục trong `ALL`. Trục, driver và `detect`
không đổi.

Bốn thứ đầu do trình biên dịch đòi. Mục thứ năm thì không — thiếu nó, bảng mã mới
biên dịch sạch nhưng không bao giờ hiện ra trong bất kỳ menu nào. Nên có một khối
`const _: () = ...` dựng mặt nạ bit từ `ALL` rồi `assert!` rằng đủ mặt: quên là vỡ
build, chứ không phải vỡ giao diện.

## API công khai

```rust
#[non_exhaustive] pub enum Charset { Unicode, Tcvn3, VniWindows, UnicodeCombining }
#[non_exhaustive] pub struct Conversion { pub text: String, pub unmapped: usize, pub normalized: usize }

/// Mọi bảng mã, theo thứ tự giao diện nên bày ra. Consumer không dựng được danh sách
/// này: `Charset` là `#[non_exhaustive]`.
pub const ALL: [Charset; 4];
impl Charset {
    pub const fn name(self) -> &'static str;  // cho người đọc
    pub const fn slug(self) -> &'static str;  // cho máy đọc — là hợp đồng, không sửa lời được
}

pub fn convert(text: &str, from: Charset, to: Charset) -> Conversion;
pub fn decode_bytes(bytes: &[u8], from: Charset) -> Conversion;
pub fn encode_bytes(text: &str, to: Charset) -> (Vec<u8>, Conversion);
pub fn detect(text: &str) -> Option<Charset>;
pub fn detect_bytes(bytes: &[u8]) -> Option<Charset>;

// Tầng trên cho consumer chuyển cả tài liệu — xem "Hai bước, không phải một".
#[non_exhaustive] pub struct Pivoted { pub text: String, pub undefined: usize, pub normalized: usize }
#[non_exhaustive] pub struct Rendered { pub text: String, pub bytes: Vec<u8>, pub cost: Cost }
#[non_exhaustive] pub struct Cost {
    pub undefined: usize, pub lost: Vec<char>, pub unrepresentable: usize, pub normalized: usize,
}
impl Cost { pub fn is_clean(&self) -> bool; }

pub fn read(text: &str, from: Charset) -> Pivoted;
pub fn render(pivoted: &Pivoted, to: Charset) -> Rendered;

mod document {
    pub struct Document { pub text: String, pub charset: Option<Charset> }
    pub fn read(bytes: Vec<u8>) -> Result<Document, TruncatedUtf16>;
}
```

Hai hàm `detect` chấm điểm bằng cách thử giải mã theo từng bảng mã rồi đếm xem kết
quả cho ra bao nhiêu âm tiết tiếng Việt hợp lệ, dùng `is_complete_syllable` sẵn có —
giải mã sai bảng mã cho ra rác, và rác thì trượt bộ kiểm tra chính tả gần như hoàn
toàn. Hoà thì trả `None` thay vì đoán bừa.

**Có hai cửa vì hai câu hỏi khác nhau.** `funput-config` cầm `&[u8]`: nhánh `None` của
`encoding::decode` nghĩa là "không BOM, không phải UTF-8", mà chính điều đó là bằng
chứng mạnh **chống lại** hai bảng mã Unicode — bằng chứng mà `detect(&str)` không thể
thấy, vì lúc nó cầm được `&str` thì hư hỏng đã được vá rồi. Hai cửa **bất đồng** trên
cùng một nội dung Latin-1; đó là bản chất, và có test ghim lại.

### Đọc cả tài liệu: `charset::document`

`decode_bytes`/`encode_bytes` là hai cửa *một tầng*; `document::read` là tầng trên chúng, và là
thứ một shell đọc tệp thật sự gọi. Nó xử lý BOM trước (UTF-16 là verdict, UTF-8 chỉ là gợi ý nên
bị bóc), rồi chọn cửa: byte giải mã được UTF-8 thì xử như **text** — đó là ca tệp TCVN3 mở bằng
Notepad rồi lưu lại, và là cách tệp cũ đến tay ta phổ biến nhất; byte không giải mã được thì đọc
một-một và **chỉ** được trả lời bằng bảng mã theo byte.

Nó **không** phải cascade của `funput-config`. Bảng gõ tắt hỏi câu chặt hơn: chỉ diễn giải lại
khi cách đọc mới giải thích được **mọi** ký tự, vì ba dòng viết tắt là quá ít bằng chứng để lật
một phép giải mã UTF-8 đã chạy được. Tài liệu thì khác: một trang tiếng Việt phần lớn đọc ra
TCVN3 **là** TCVN3, và một dấu `°` lạc không chứng minh điều gì. Cùng cấu trúc, khác mức tự tin,
nên chúng ở riêng một cách có chủ ý.

### Hai cửa byte

`decode_bytes` đọc tệp vào; `encode_bytes` ghi tệp ra. Cái sau không phải đối xứng cho đẹp: bảng
mã theo byte lưu **một byte mỗi chữ**, nên ghi text của nó ra dạng UTF-8 sẽ thành hai byte mỗi chữ
và cho ra tệp `.VnTime` đọc không được. Mọi consumer lưu tệp cũ đều cần đúng phép này, nên nó ở
core chứ không phải viết lại ở từng nơi.

Chữ mà bảng mã đích không có vẫn được ghi — `encode` không bao giờ làm rơi ký tự — nhưng có thể là
ký tự dạng byte không chứa nổi (`₫` chẳng hạn); những chữ đó thành `?`. Chúng đã nằm trong
`unmapped`, nên caller biết được từ chính con số nó vẫn phải xem.

### Cửa C ABI (`funput-ffi`, feature `charset`, mặc định tắt)

Cho shell không link Rust được — macOS là ca thật. Bảng mã được gọi tên bằng **chỉ số trong
`ALL`**, không phải hằng số riêng của FFI: viết `match charset` ngoài `funput-core` cần nhánh
wildcard, nên bảng mã thêm sau sẽ âm thầm rơi ra ngoài. `funput_charset_count()` +
`funput_charset_name()` đủ dựng menu. Đổi lại, **`ALL` chỉ được thêm vào cuối** — đảo thứ tự sẽ
biến TCVN3 mà người dùng đã lưu thành VNI.

Chuỗi đi qua UTF-32 + buffer của caller như phần còn lại của crate đó, nhưng **được ăn cả ngã
không**: trả về độ dài cần thiết, và chỉ ghi khi vừa. `copy_codepoints` cắt bớt vì nó phục vụ
buffer 64 ký tự đang gõ; ở đây đầu vào là cả văn bản, và một nửa văn bản ghi vào buffer thiếu chỗ
tệ hơn là không ghi gì. Hai bộ đếm có mặt ngay ở lượt hỏi độ dài, nên host cảnh báo được trước khi
cấp phát.

**Chỉ cửa text.** Cửa byte không xuất ra: shell đọc *tệp* cần cả tầng giải mã (BOM UTF-16, thử
UTF-8, và luật byte đã trượt UTF-8 thì chỉ được trả lời bằng bảng mã theo byte). Xuất ba nguyên
thuỷ đó ra là giao phần tinh tế cho caller và để nó bị viết lại một lần mỗi nền tảng — đúng thứ
core sinh ra để tránh. Khi có shell cần, tầng đó thuộc về core dưới dạng **một** lời gọi.

### Thứ nó không làm được

Mọi phán đoán ở đây đều **tương đối**: bốn giả thuyết, cái nào khớp nhất thì thắng.
Một bảng mã chưa hiện thực không có giả thuyết riêng, nên nó luôn được trả lời bằng
láng giềng gần nhất. VISCII chia sẻ chữ cái Latin-1 với TCVN3, nên văn bản VISCII bị
báo là TCVN3 một cách tự tin và chuyển ra thứ sai một cách tinh vi.

Không ngưỡng rẻ nào sửa được — cách sửa thật là hiện thực VISCII. Có test ghim hành vi
này để người sau thấy đây là chuyện đã biết.

Module nằm trong không gian tên riêng (`funput_core::charset::…`), **không** re-export
ra gốc crate: khối "API FROZEN (Phase 8)" ở `lib.rs` liệt kê bề mặt cho
`funput-engine`, và danh sách đó phải tiếp tục đúng cho bản build mặc định.

## Thứ tự hiện thực

Mỗi mục một PR:

1. Tài liệu này.
2. Khung core + **TCVN3**. Phải kèm một bảng mã thật, vì CI chạy `-D warnings` và một
   trục chỉ có codec identity là dead code. TCVN3 được chọn vì nó dùng tới cả adapter
   Latin-1 lẫn đường `unmapped`, tức kiểm chứng nhiều phần khung nhất.
3. **VNI-Windows**. Bảng mã đầu tiên có chữ dài hai ký tự, nên nó cũng là phép thử
   thật đầu tiên cho mô hình trục: `TCVN3 ↔ VNI` chạy được mà không codec nào biết
   codec kia.
4. **Unicode tổ hợp**. Bảng mã duy nhất không phải byte, nên nó làm lộ hai lỗi thật
   trong `decode_bytes` và buộc `unmapped` tách làm hai.
5. **`detect()`**. Nó làm lộ một lỗi thật trong hai codec byte: ký tự trên `U+00FF`
   được nhận là `Exact`, nên `convert("Việt Nam", Tcvn3, Unicode)` báo 0 lỗi và ba
   bảng mã hoà nhau tuyệt đối.
6. **`funput-config`** (sửa import UniKey). Consumer đầu tiên. Nó buộc `is_byte_oriented`
   thành API công khai: byte đã trượt `from_utf8` thì không thể là bảng mã Unicode,
   và nói điều đó mà không gọi tên bảng mã nào là thứ giữ cho consumer không phải
   sửa khi có bảng mã mới.
7. **`ALL` + `Charset::name()`**. Hai thứ duy nhất consumer không tự suy ra được:
   `Charset` là `#[non_exhaustive]`, nên code ngoài crate phải viết nhánh wildcard và
   sẽ âm thầm bỏ sót biến thể thêm sau. Đây cũng là chỗ xoá bản sao `charset_label`
   mà UI Windows đã phải tự viết ở bước 6 — hai cửa sổ cài đặt tự đặt tên lấy là cách
   chúng trôi khỏi nhau.
8. **C ABI trong `funput-ffi`** (feature `charset`, mặc định tắt) — cửa cho macOS.
9. **`funput convert`** (CLI). Consumer đầu tiên chuyển cả tài liệu, nên nó đòi hai thứ core còn
   thiếu: `Charset::slug()` (cờ dòng lệnh cần một tên máy đọc — `name()` là chữ hiển thị, sửa lời
   là đổi hợp đồng) và `encode_bytes` (ghi tệp cũ ra byte). Nó cũng là chỗ **hai cửa** lộ rõ nhất:
   tệp TCVN3 mở bằng Notepad rồi lưu lại là UTF-8 hợp lệ mà nội dung vẫn TCVN3, và đọc lại theo
   byte sẽ ra `Ã` ở chỗ tài liệu có `Ö`.
10. **UI Windows** — cửa sổ Chuyển mã riêng. Bốn nước đi làm nên nét riêng, và cả bốn đều
    mọc ra từ thứ UniKey không có (`detect` + hai bộ đếm): không hỏi bảng mã nguồn mà **nói**
    ra; thấy trước/sau theo thời gian thực; **gọi tên** những chữ sẽ mất chứ không chỉ đếm; và
    nhận diện **từng tệp** trong lô thay vì ép một bảng mã cho cả thư mục.
11. **`crates/funput-convert`** — tách ruột của cửa sổ ra khỏi shell, **trước** khi có cửa sổ
    thứ hai. Bốn việc trong đó không có gì là đồ hoạ (đọc lô theo từng tệp, nói ra cái giá,
    ghi bản sao cạnh bản gốc, thuật lại), và chép chúng sang GTK sẽ đặt câu cảnh báo mất chữ,
    luật `vanban (2).txt` và tên thư mục `Đã chuyển mã` vào hai chỗ cùng lúc. Đây là cùng
    một phán quyết đã ra ở mục 7 cho `Charset::name()`, chỉ ở quy mô lớn hơn.

    Lợi ích thứ hai lớn hơn ý định ban đầu: `platforms/windows` và
    `platforms/linux/settings-gtk` đều nằm ngoài cargo workspace, nên `clippy --workspace`,
    `test --workspace` và `check-loc.sh` không với tới cái nào — 11 test của cửa sổ Windows
    chỉ chạy lúc phát hành. Đưa vào `crates/` là lần đầu chúng chạy ở mỗi PR, cho cả hai nền
    tảng.
12. **UI Linux GTK** — cùng ba trạng thái, cùng bốn nước đi, cùng từng câu chữ. Ba điều
    riêng của nền tảng:

    - **Không có tray.** Fcitx5 và iBus tự vẽ status icon, nên mục tray của Windows không có
      chỗ đứng. Thay bằng hai cửa: một hàng trong Settings → Chung, và
      `funput-convert.desktop` chạy `funput-settings --convert`. Cùng một tiến trình, cùng
      một thể hiện — `HANDLES_COMMAND_LINE`, vì cờ mặc định của `GApplication` không chuyển
      tiếp argv và `--convert` sẽ mất lặng lẽ.
    - **Dựng lại từ state, có điều chỉnh.** Thuộc tính Slint là giá trị nên Windows dựng lại
      được cả cửa sổ; tạo lại một `TextView` mỗi phím gõ thì giết focus và con trỏ. Bộ khung
      dựng một lần, `refresh()` chỉ đặt thuộc tính, và **một** cờ `refreshing` chặn tái nhập
      cho cả bốn tín hiệu mà chính `refresh()` phát ra — nếu không thì mỗi lần nó đặt một
      dropdown sẽ là một `borrow_mut` lồng nhau, tức panic.
    - **Ký tự điều khiển.** `TextBuffer` của GTK nhận cả độ dài chứ không dừng ở NUL, nên một
      tệp cũ hỏng làm nó đo và vẽ sai phần còn lại. `capped()` khử C0 — trong lõi dùng chung,
      nên Windows được hưởng luôn, dù ở đó chưa ai thấy.

    Fcitx5 và iBus **dùng chung** cửa sổ này: nó không hỏi khung nào đang chạy, không đọc
    `settings.json`, không nói chuyện với engine. Cả hai đã phụ thuộc gói `funput-settings`
    tại đúng phiên bản, nên cả hai nhận nó mà không sửa một dòng C++.
