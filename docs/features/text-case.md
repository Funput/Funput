# Chuyển đổi kiểu chữ

## Trạng thái

**Chưa có code.** Tài liệu này chốt mô hình trước, như [charset.md](charset.md) đã làm,
và là nơi mọi quyết định thiết kế sống — mỗi phép biến đổi mới cập nhật lại nó trong
cùng PR.

Phạm vi **V1 đã chốt: một tab trong cửa sổ Chuyển mã**, ba nền tảng desktop, cộng
`funput case` trong CLI. Không hotkey, không đụng vùng bôi đen, không nghe clipboard. Những thứ đó nằm ở
[Lộ trình sau V1](#lộ-trình-sau-v1) và có tài liệu riêng khi tới lượt.

## Mục tiêu

Đổi **kiểu chữ của văn bản có sẵn**, năm phép:

| Phép | Ví dụ |
| --- | --- |
| CHỮ HOA | `Xin chào Việt Nam` → `XIN CHÀO VIỆT NAM` |
| chữ thường | `Xin Chào Việt Nam` → `xin chào việt nam` |
| Bỏ dấu tiếng Việt | `Tiếng Việt rất đẹp` → `Tieng Viet rat dep` |
| Viết hoa đầu câu | `xin chào. hôm nay trời đẹp.` → `Xin chào. Hôm nay trời đẹp.` |
| Viết Hoa Đầu Mỗi Từ | `bàn phím tiếng Việt` → `Bàn Phím Tiếng Việt` |

Đây là tính năng của **người soạn thảo**, không phải của bộ gõ: nó chạy trên đoạn văn
đã có, không can thiệp vào phím đang gõ. Vì vậy nó ở cạnh Chuyển mã chứ không ở cạnh
engine.

## Không thuộc phạm vi

- **Sửa ngữ nghĩa.** `chữ thường` sẽ phá `Việt Nam` thành `việt nam`, và đó là đúng
  yêu cầu. Không có danh sách tên riêng, không đoán, không "thông minh". Cách chữa
  duy nhất là **hoàn tác**, nên hoàn tác phải rẻ và luôn có.
- **Danh sách viết tắt cho "đầu câu".** `v.v.`, `TS.`, `Th.S` sẽ bị coi là hết câu và
  chữ sau đó bị viết hoa. Đã cân nhắc và **cố ý không làm**: một danh sách viết tắt
  tiếng Việt không bao giờ đủ, và sai sót của nó khó đoán hơn là một quy tắc đơn giản
  mà người dùng nhìn preview là thấy ngay. Ghi vào phần giới hạn đã biết ở UI.
- **Chế độ slug** (`tieng-viet`) và **bỏ dấu giữ dấu kiểu Telex** (`đẹp` → `depj`).
  Có thể thêm sau; không thuộc V1.
- Chuyển đổi trên vùng bôi đen, hotkey toàn cục, clipboard tự động — xem lộ trình.
- Wayland: mọi đường đi qua vùng bôi đen đều không khả thi, và tài liệu sẽ nói thẳng
  như vậy thay vì hứa.

## Năm phép biến đổi, chính xác đến ca biên

### Chung cho cả năm

Hàm nhận `&str`, trả `String`. **Không phải phép ánh xạ từng `char`**: `char`
tiếng Việt viết hoa có thể dài hơn nguyên bản ở dạng tổ hợp, nên lõi làm việc trên
chuỗi. Dùng `to_uppercase`/`to_lowercase` của Rust ở mức chuỗi (không phụ thuộc
locale — tiếng Việt không có ca biên kiểu `i` của tiếng Thổ).

Chuỗi vào có thể ở **Unicode dựng sẵn hoặc tổ hợp**. Cả năm phép phải cho cùng kết
quả ở hai dạng, và **giữ nguyên dạng của đầu vào** — không tự dựng sẵn hoá một tài
liệu tổ hợp, vì người dùng chỉ xin đổi kiểu chữ.

### CHỮ HOA / chữ thường

Thẳng. Ca biên duy nhất đáng nói nằm ở [bảng mã cũ](#bảng-mã-cũ-là-ca-biên-thật-sự).

### Bỏ dấu tiếng Việt

Bỏ **cả thanh điệu lẫn dấu phụ của nguyên âm**, giữ chữ cái ASCII:

```
ế → e     ơ → o     ă → a     ữ → u     Ề → E
đ → d     Đ → D                (mặc định; xem tuỳ chọn bên dưới)
```

Không dùng crate normalization: `funput-core` có **bất biến không phụ thuộc runtime**
(`[dependencies]` rỗng), và phá nó cho một tính năng desktop là cái giá sai.

Và **không cần bảng mới nào**: `unicode::shapes::base_vowel` đã bỏ cả thanh lẫn hình
mà giữ hoa/thường, trên toàn bộ bảng chữ dựng sẵn — chính engine dùng nó để quyết định
phím hình có đổi được nguyên âm hay không. Một bảng `family → ASCII` sẽ là bản copy
thứ hai của cùng câu trả lời, và bản thứ hai là bản sẽ lệch. Chữ tổ hợp xử lý bằng
cách lọc tám dấu rời, danh sách lấy từ `unicode::combining`.

**Tuỳ chọn `đ → d`**: mặc định **bật**. Người cần giữ `đ` (đặt tên file cho hệ thống
chấp nhận nó, hoặc chỉ muốn bỏ thanh) tắt được trong cùng tab. Đây là tuỳ chọn duy
nhất của phép này.

Hai điều phép này **không làm được**, và đều là bản chất chứ không phải thiếu sót.
`é` chỉ có một code point dù người gõ nó cho tiếng Việt hay tiếng Pháp, nên `café`
thành `cafe`: một phép biến đổi trên chữ cái không biết được ai có ý gì. Và dấu tổ hợp
bị bỏ bất kể ai đặt nó, nên `ñ` ở dạng tổ hợp ra `n` — còn `ñ`, `ü`, `ç`, `ß` dựng sẵn,
chữ Nhật và emoji thì giữ nguyên mọi thứ. Cả hai đều có test khoá lại để chúng là quyết
định, không phải điều bất ngờ.

### Viết hoa đầu câu

Viết hoa chữ cái đầu của mỗi câu, **giữ nguyên phần còn lại**:

```
xin CHÀO. hôm nay trời đẹp.  →  Xin CHÀO. Hôm nay trời đẹp.
```

Đã cân nhắc phương án "hạ thường tất cả rồi mới viết hoa" và **không chọn**: nó phá
`TP. HCM`, phá tên riêng, phá chữ viết hoa cố ý, trong khi người dùng muốn cả hai việc
đó thì đã có "chữ thường" rồi bấm tiếp "đầu câu".

Ranh giới câu:

| Là ranh giới | Không là ranh giới |
| --- | --- |
| `.` `!` `?` `…` kèm khoảng trắng sau | `.` giữa số (`1.5`) |
| **Xuống dòng** (`\n`), kể cả khi dòng trước không có dấu chấm | dấu `.` của viết tắt (`v.v.`, `TS.`) — giới hạn đã biết |
| Đầu văn bản | |

Xuống dòng là ranh giới vì đầu vào thật của tính năng này thường là danh sách, phụ đề
`.srt`, ghi chú — nơi không ai chấm câu.

Chữ cái đầu có thể không phải chữ cái: `"xin chào"` và `(xin chào)` phải viết hoa
`x`, không bỏ qua vì gặp dấu nháy. Quy tắc: bỏ qua mọi ký tự không phải chữ cái cho
tới chữ cái đầu tiên của câu.

### Viết Hoa Đầu Mỗi Từ

Viết hoa chữ cái đầu mỗi từ, hạ thường phần còn lại của từ — **trừ từ đang viết HOA
toàn bộ, giữ nguyên**:

```
bàn phím tiếng Việt   →  Bàn Phím Tiếng Việt
gửi về TP. HCM        →  Gửi Về TP. HCM        (không phải "Tp. Hcm")
```

Ngoại lệ ALL-CAPS bật mặc định, tắt được. Từ một chữ cái (`A`) coi như ALL-CAPS —
vô hại, vì kết quả hai đường như nhau.

Ranh giới từ là khoảng trắng và dấu câu; `bàn-phím` thành `Bàn-Phím`, `e-mail` thành
`E-Mail`. Đây là quy ước của mọi công cụ cùng loại và người dùng đã quen.

Không có danh sách từ nối ("của", "và", "là" vẫn được viết hoa). Tiếng Việt không có
quy ước title case như tiếng Anh, và đoán sẽ sai nhiều hơn đúng.

## Bảng mã cũ là ca biên thật sự

Cửa sổ Chuyển mã làm việc với TCVN3 và VNI-Windows, nên tab mới **bắt buộc** phải trả
lời câu hỏi: viết hoa một đoạn TCVN3 thì sao?

Viết hoa từng `char` của chuỗi TCVN3 là **sai**, vì `char` ở đó không phải chữ cái —
nó là byte TCVN3 nằm trong `U+0020..U+00FF` mà font `.VnTime` vẽ ra. `to_uppercase`
của Rust không biết điều đó.

Đường đúng dùng lại nguyên mô hình trục đã có ở [charset.md](charset.md):

```
văn bản nguồn ──decode──> Unicode dựng sẵn ──đổi kiểu chữ──> ──encode──> bảng mã nguồn
```

Hệ quả quan trọng: **encode ngược có thể mất chữ**. TCVN3 không đủ chỗ cho nguyên âm
hoa có dấu, nên viết hoa một văn bản TCVN3 là phép **có mất mát** — đúng loại mất mát
mà `charset::Cost` và `text::warning` đã được xây để cảnh báo, kể cả việc **nêu tên
ký tự** bị mất. Tab mới dùng lại y nguyên hai thứ đó, không viết câu cảnh báo thứ hai.

"Bỏ dấu" thì ngược lại: luôn về ASCII, nên không bao giờ mất mát ở bước encode, dù
bản thân nó là phép mất thông tin — chuyện này nói bằng một dòng ghi chú tĩnh ở UI,
không dùng đường cảnh báo.

## Nơi đặt code

| Tầng | Nội dung |
| --- | --- |
| `funput-core::textcase` | Năm phép biến đổi thuần, không phụ thuộc, không alloc ngoài chuỗi kết quả, và không bảng dữ liệu mới nào. Sau cargo feature riêng, **mặc định tắt** như `charset` — iOS/Android không bao giờ biên dịch nó |
| `funput-convert` | Trục thứ hai của `Session`: "đổi bảng mã" và "đổi kiểu chữ" dùng chung một `Session`, một `View`, một đường cảnh báo |
| Ba shell | Chỉ tab, nút, và preview. Không quyết định gì — đúng hợp đồng `refresh()` / `view()` hiện tại |
| `funput-cli` | `funput case --upper|--lower|--no-diacritics|--sentence|--title` đọc stdin. Rẻ, và là bề mặt test tốt nhất |

**Vì sao mở rộng `Session` chứ không dựng session thứ hai.** Lý do `funput-convert`
tồn tại — ghi ở đầu `lib.rs` — là để câu chữ cảnh báo, quy tắc `vanban (2).txt` và
tên thư mục đích chỉ có một bản. Một session thứ hai sẽ chép lại đúng những thứ đó.
Cụ thể: thêm một trường "phép biến đổi" (`None` = chỉ đổi bảng mã) vào `Session`,
`Mode` giữ nguyên ba hình dạng `Empty`/`Text`/`Files`.

Trần 150 LOC/file vẫn áp dụng: năm phép nhiều khả năng là năm file trong
`core/src/textcase/`.

### Feature `textcase`, tách khỏi `charset`

**Một cargo feature riêng trong `funput-core`, không phải một crate riêng.**

Feature riêng chứ không gộp vào `charset`, vì hai thứ đó không đi cùng nhau ở cả hai
đầu: `funput-config` bật `charset` chỉ để giải mã file gõ tắt UniKey và không bao giờ
đổi kiểu chữ; còn chiều quan trọng hơn là **mobile có thể cần `textcase` mà chắc chắn
không cần `charset`**. iOS và Android đều đọc được vùng bôi đen từ chính bàn phím, nên
một phím "bỏ dấu" ngay trên bàn phím là hướng đi hợp lý về sau — và gộp hai feature sẽ
kéo cả bốn codec bảng mã vào bản build của chúng, đúng thứ mà ghi chú của feature
`charset` cấm.

Crate riêng thì không, vì phần bỏ dấu **đọc thẳng ruột của core**:
`unicode::shapes::base_vowel` và `unicode::combining::is_mark`, cả hai đều
`pub(crate)`. Một crate ngoài chỉ có hai đường: chép lại dữ liệu — hai nguồn sự thật
cho cùng một thứ — hoặc đổi chúng thành `pub`, biến chi tiết nội bộ thành API công khai
của core cho đúng một người dùng.
`funput-convert` là crate riêng vì lý do ngược lại: nó không cần ruột của core, nó cần
nằm *trong* workspace để `cargo test --workspace` và `check-loc.sh` với tới hai shell
bị loại khỏi workspace. `textcase` không có vấn đề đó.

Hai chi tiết bắt buộc khi hiện thực:

- Module `unicode::combining` (tám dấu tổ hợp) nằm sau
  `#[cfg(any(feature = "charset", feature = "textcase"))]`: nới đúng lúc có người đọc
  thứ hai, không nới trước — gate rộng hơn số người đọc là dead code, và bước CI
  textcase-on/charset-off sẽ trượt vì `-D warnings`. Không có gate nào khác phải đổi;
  `base_vowel` vốn đã `pub(crate)` không gated.
- `funput-ffi` đã có cấu trúc hai tầng sẵn; chỉ thêm `textcase = ["funput-core/textcase"]`
  và đổi `convert` thành `["charset", "textcase", "dep:funput-convert"]`. Job CI hiện
  có (`cargo test -p funput-ffi --features convert`) phủ luôn, không cần job mới.

## Giao diện V1

Tab thứ hai trong cửa sổ Chuyển mã, cùng vùng dán và cùng cặp khung trước/sau:

- Năm nút phép biến đổi (chọn một), áp dụng ngay lên preview.
- Hai công tắc: **đ → d** (mặc định bật) và **giữ nguyên từ viết HOA** (mặc định bật),
  chỉ hiện khi liên quan tới phép đang chọn.
- Dòng cảnh báo dùng chung với Chuyển mã, chỉ hiện khi encode ngược mất chữ.
- Nút **Sao chép kết quả**, và **Hoàn tác** trả về nguyên bản.
- Phép biến đổi **cộng dồn được**: bấm "chữ thường" rồi "Viết Hoa Đầu Mỗi Từ" cho kết
  quả của cả hai. Nguyên bản luôn giữ để hoàn tác về một bước.

Một file thả vào vẫn rơi vào `Mode::Text` như hiện tại. Nhiều file là batch —
**không thuộc V1**, nhưng bảng `Row` không cần đổi khi tới lượt.

## Lộ trình sau V1

| Giai đoạn | Nội dung | Rào cản |
| --- | --- | --- |
| V2 | Batch kéo-thả file | Không có; chỉ là nối vào đường batch sẵn có |
| V3 | Hotkey xử lý clipboard: copy → hotkey → clipboard đã đổi, người dùng tự dán | Không cần quyền hệ thống. Phải sao lưu/khôi phục clipboard đủ kiểu dữ liệu, và từ chối khi clipboard không phải text |
| V4 | Bôi đen → hotkey → thay tại chỗ, kèm HUD | Quyền và API theo nền tảng, xem dưới |
| V5 | Đổi kiểu chữ **từ/câu vừa gõ** qua engine, không cần bôi đen | Dùng bộ nhớ văn bản đã gõ sẵn có (`Engine::adopt`, typed-text shadow của Windows) |

### V4: mô hình kích hoạt đã chốt

**Một hotkey, rồi một phím chọn.** Bấm tổ hợp → thanh nhỏ hiện cạnh con trỏ → bấm
`U` HOA, `T` thường, `D` bỏ dấu, `C` đầu câu, `W` đầu từ, `Esc` thôi. Chỉ tốn một
keybind, tự dạy người dùng, và có chỗ đặt nút Hoàn tác — thứ bắt buộc phải có vì
"chữ thường" phá tên riêng.

Đã loại: năm hotkey riêng (không ai nhớ nổi, đụng phím tắt app khác) và một hotkey
xoay vòng (không với tới bỏ dấu và đầu câu).

### V4: khả thi theo nền tảng

| | Đọc vùng chọn | Ghi đè | Ghi chú |
| --- | --- | --- | --- |
| macOS | `AXSelectedText`, hoặc giả lập ⌘C | AX set, hoặc giả lập ⌘V | Cần quyền Accessibility. **NSServices** là đường thứ hai: hệ thống tự đưa text và tự thay lại, không cần quyền, hiện sẵn trong menu chuột phải |
| Windows | UIA `TextPattern`, hoặc giả lập Ctrl+C | Ctrl+V hoặc inject | Đường inject đã có sẵn |
| Linux X11 | PRIMARY selection — đọc được **không cần Ctrl+C** | XTEST | OK |
| Linux Wayland | ✗ | ✗ | Không hỗ trợ, nói thẳng |

Đường "giả lập phím copy" là **fallback, không phải đường chính**: phải chờ
`changeCount` đổi chứ không ngủ một khoảng đoán mò, và nó sai trong terminal, nơi
⌘C/Ctrl+C là ngắt tiến trình chứ không phải copy.

An toàn: không chạy trong ô mật khẩu (secure input trên macOS, cờ ô nhập trên
Windows), và không đụng clipboard ở đó.

## Cấu hình

V1 **không thêm khoá nào vào file cấu hình**. Hai công tắc của tab là lựa chọn của
phiên làm việc, lưu cùng chỗ cửa sổ Chuyển mã lưu trạng thái của nó.

Từ V3 trở đi mới cần hotkey, và hotkey là khối **theo từng nền tảng** trong
[CONFIG_FORMAT.md](../../platforms/CONFIG_FORMAT.md) — Windows và Linux dùng preset
giống nhau nhưng mỗi bên chỉ đọc khối của mình. Đặt tên khoá khi tới giai đoạn đó,
không đoán trước.

## Kiểm thử

- **Corpus vàng** cho cả năm phép, chạy ở cả Unicode dựng sẵn và tổ hợp, cho cùng kết
  quả.
- **Property** (`proptest` đã có sẵn trong dev-dependencies):
  - viết HOA và viết thường là idempotent;
  - bỏ dấu cho kết quả chỉ chứa ASCII với mọi đầu vào tiếng Việt;
  - bỏ dấu là idempotent;
  - đổi kiểu chữ rồi encode về TCVN3 không bao giờ panic, chỉ báo `Cost`.
- **Roundtrip bảng mã**: viết HOA một đoạn TCVN3 rồi đọc lại phải ra đúng chữ hoa
  tương ứng, hoặc báo mất chữ có nêu tên — không có đường thứ ba.
- Ca biên phải có test riêng: `TP. HCM`, `1.5`, `v.v.` (test **khoá giới hạn đã
  biết**, không phải test đòi nó đúng), chuỗi rỗng, chỉ dấu câu, emoji, chữ Nhật lẫn
  trong câu.

### Kiểm tra thủ công

1. Mở Chuyển mã → tab Kiểu chữ, dán `xin chào. hôm nay trời đẹp.` rồi thử lần lượt
   năm nút; kiểm tra preview trước/sau khớp với bảng ở đầu tài liệu.
2. Dán đoạn có `TP. HCM` và một dòng xuống dòng không chấm câu: kiểm tra ngoại lệ
   ALL-CAPS và ranh giới dòng.
3. Tắt công tắc `đ → d`, bỏ dấu `đẹp` → `đep`.
4. Dán một đoạn TCVN3 (copy từ Word font `.VnTime`), bấm CHỮ HOA: phải thấy dòng cảnh
   báo nêu tên ký tự sẽ mất.
5. Bấm chồng hai phép rồi Hoàn tác: phải về đúng nguyên bản, một bước.
6. `echo "Tiếng Việt" | funput case --no-diacritics` cho `Tieng Viet`.
