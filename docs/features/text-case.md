# Chuyển đổi kiểu chữ

## Trạng thái

**V1 đã xong.** `funput_core::textcase` sau cargo feature `textcase` có cả năm phép,
kèm corpus và property test; `funput-convert` mang trục thứ hai của `Session`;
`funput-ffi` mở cửa C cho nó; và cả bốn bề mặt đều đã gọi tới: **`funput case`** trong
CLI, **macOS** (`ConvertCasingBar.swift`), **Windows** (`ui/convert/casing.slint`) và
**Linux** (`settings-gtk/src/convert/ui/casing.rs`). Không shell nào tự quyết định gì
— cả ba đọc cùng `View` và gọi cùng `Session`.

Tài liệu này chốt mô hình trước khi có code, như [charset.md](charset.md) đã làm, và là
nơi mọi quyết định thiết kế sống — mỗi thay đổi cập nhật lại nó trong cùng PR.

Phạm vi **V1 đã chốt: một thanh trong cửa sổ Chuyển mã**, ba nền tảng desktop, cộng
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
chấp nhận nó, hoặc chỉ muốn bỏ thanh) tắt được bằng công tắc **Giữ đ/Đ** trên cùng
thanh — công tắc đặt tên theo thứ nó bật lên, nên mặc định của phép hiện ra ở UI là
công tắc **tắt**. Đây là tuỳ chọn duy nhất của phép này.

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
`x`, không bỏ qua vì gặp dấu nháy. Nhưng **chữ số thì chặn**: câu mở đầu bằng số là câu
đã bắt đầu, nên `3 con mèo` không được thành `3 Con mèo`. Quy tắc: đi qua dấu mở, dừng
ở chữ cái **hoặc** chữ số đầu tiên, và chỉ chữ cái mới được đổi.

### Viết Hoa Đầu Mỗi Từ

Viết hoa chữ cái đầu mỗi từ, hạ thường phần còn lại của từ — **trừ từ đang viết HOA
toàn bộ, giữ nguyên**:

```
bàn phím tiếng Việt   →  Bàn Phím Tiếng Việt
gửi về TP. HCM        →  Gửi Về TP. HCM        (không phải "Tp. Hcm")
```

Ngoại lệ ALL-CAPS bật mặc định, tắt được bằng công tắc **Hạ chữ viết hoa** — đặt tên
theo thứ nó bật lên, nên cũng mặc định **tắt** như công tắc kia. Từ một chữ cái (`A`)
coi như ALL-CAPS — vô hại, vì kết quả hai đường như nhau.

Ranh giới từ là **khoảng trắng hoặc dấu câu ASCII**; `bàn-phím` thành `Bàn-Phím`,
`e-mail` thành `E-Mail` — quy ước của mọi công cụ cùng loại. Dấu nháy đơn là ngoại lệ,
để `don't` không thành `Don'T`.

Giữ tập ranh giới trong ASCII mua được một bảo đảm đáng giá hơn những ca biên nó bỏ
lỡ: **không bao giờ cắt một từ ở giữa một grapheme cluster**, vì mọi dấu tổ hợp đều
ngoài ASCII. `Tiếng Việt` viết dạng tổ hợp vẫn là hai từ, trong khi quy tắc "cắt ở mọi
ký tự không phải chữ cái" sẽ cắt từng nguyên âm khỏi dấu của nó rồi viết hoa mảnh vụn
phía sau. Giá phải trả: dấu câu ngoài ASCII không có khoảng trắng thì không tách từ
(`xin…chào` chỉ viết hoa `x`) — thuần thẩm mỹ, và hiếm trong loại văn bản này.

Vì một từ có thể mở đầu bằng thứ không có hoa/thường, phép này tìm **chữ cái đầu tiên
trong từ**: `"xin` được viết hoa, và `5g` thành `5G`.

Không có danh sách từ nối ("của", "và", "là" vẫn được viết hoa). Tiếng Việt không có
quy ước title case như tiếng Anh, và đoán sẽ sai nhiều hơn đúng.

**Giới hạn đã biết**, đối xứng với ngoại lệ ALL-CAPS: chữ hoa nằm giữa từ sẽ mất, nên
`iPhone` thành `Iphone`. Giữ được nó nghĩa là không hạ thường phần còn lại của từ, mà
đó chính là việc của phép này.

## Bảng mã cũ là ca biên thật sự

Cửa sổ Chuyển mã làm việc với TCVN3 và VNI-Windows, nên trục mới **bắt buộc** phải trả
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
| Ba shell | Chỉ thanh, nút, và preview. Không quyết định gì — đúng hợp đồng `refresh()` / `view()` hiện tại |
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

**Một thanh luôn hiện, không phải một tab.** Đã cân nhắc tab và **loại**: đổi bảng mã
và đổi kiểu chữ là hai lựa chọn về cùng một văn bản và chúng **cộng dồn** — một đoạn
đọc ra từ TCVN3 vẫn viết hoa đầu mỗi từ được — nên cả hai ở trên màn hình cùng lúc.
Một tab sẽ nói rằng người dùng phải chọn một trong hai.

Thanh nằm ngay dưới hàng chọn bảng mã ở hình dạng văn bản, và dưới header ở hình dạng
batch; hình dạng rỗng (vùng thả tệp) không có nó. Cùng vùng dán và cùng cặp khung
trước/sau:

- Năm nút phép biến đổi, áp dụng ngay lên preview. Phép đang áp có **nền accent và
  dấu tick** — chỉ đổi màu là tín hiệu mà người mù màu không nhận được.
- Dòng **`Đang áp: chữ thường → Viết Hoa Đầu Mỗi Từ`** viết rõ thứ tự, vì thứ tự ra
  văn bản khác nhau. Chỉ hiện khi có phép đang áp.
- Hai công tắc, đặt tên theo thứ chúng **bật lên** nên cả hai **mặc định tắt**:
  **Giữ đ/Đ** (tắt = `đ → d`) và **Hạ chữ viết hoa** (tắt = giữ nguyên từ viết HOA).
  Mỗi công tắc chỉ hiện **khi phép sở hữu nó đang được áp** — ngoài lúc đó nó không có
  gì để nói, và hiện ở đó là cách dạy phép nào sở hữu nó. Trên GTK là `gtk::Switch`
  kèm nhãn, không phải `adw::SwitchRow`: `SwitchRow` là một `ActionRow`, đặt vào thanh
  ngang sẽ ra một hàng full-width có nền riêng. Giá phải trả là công tắc trần không có
  tên cho screen reader, nên mỗi cái được trỏ về nhãn của nó bằng `LabelledBy`.
- Dòng cảnh báo dùng chung với Chuyển mã, chỉ hiện khi encode ngược mất chữ.
- Nút **Hoàn tác** gỡ **phép cuối cùng** chứ không phải tất cả — bấm nhầm phép thứ ba
  không nên mất hai phép trước nó — và **Bỏ hết** trả về nguyên bản.
- Phép biến đổi **cộng dồn được**: bấm "chữ thường" rồi "Viết Hoa Đầu Mỗi Từ" cho kết
  quả của cả hai. Bấm lại đúng phép đang ở đỉnh là **không làm gì**, vì mọi phép đều
  idempotent nên lần bấm thứ hai không đổi được văn bản. Nguyên bản luôn giữ: danh
  sách phép được phát lại từ đầu ở mỗi lần dựng preview, không bao giờ ghi đè lên
  đoạn người dùng đã dán.
- Thanh **mờ và không bấm được** khi chưa có bảng mã nào giải thích được văn bản: văn
  bản đó không được chuyển mã, và cũng không được đổi kiểu chữ — một quy tắc, không
  phải hai. Batch mang bảng mã theo từng dòng nên không bao giờ bị chặn.

Một file thả vào vẫn rơi vào `Mode::Text` như hiện tại. Nhiều file là batch —
**không thuộc V1**, nhưng bảng `Row` không cần đổi khi tới lượt.

### Ba chỗ mỗi shell tự quyết, và đã quyết khác nhau

Hợp đồng ở trên là chung; cách vẽ ra thì theo idiom của nền tảng, và ba chỗ dưới đây
là nơi ba shell không giống nhau. Ghi lại để lần sau không ai "sửa" chúng cho khớp.

- **Hình dạng chip.** macOS dùng capsule `glassEffect`, Windows vẽ capsule 28px trong
  Slint. GTK dùng **nút thường trong `gtk::FlowBox`**, không dùng class `pill` của
  libadwaita: `.pill` là `padding: 10px 32px`, và năm nhãn kèm padding đó vượt cả
  `default_width` 880 lẫn `width_request` 640 của cửa sổ — GTK nâng minimum của cửa sổ
  cho vừa con nó, nên capsule sẽ khiến Chuyển mã không co lại được. Đo được: thanh có
  min 496px, natural 778px, nên vừa một hàng ở kích thước mặc định và xuống dòng khi
  người dùng thu nhỏ. Mỗi chip `halign: Start` để một chip không đổi kích thước tuỳ
  theo hàng có xuống dòng hay chưa. `adw::WrapBox` là container đúng hơn nhưng có từ
  libadwaita 1.7, còn crate nhắm 1.5.
- **Dấu tick.** GTK dùng `object-select-symbolic`, vì nó nằm trong chính gresource của
  GTK4 lẫn theme Adwaita hệ thống; `check-plain-symbolic` và `emblem-ok-symbolic`
  không có trong Adwaita trên 24.04 và sẽ ra ô ảnh thiếu.
- **Thanh lúc batch đang chạy.** macOS tắt thanh (`!isBusy && …`); Windows và Linux để
  nguyên, đúng câu chữ ở trên — chỉ tắt khi chưa đoán được bảng mã. Để nguyên là an
  toàn vì `Session::batch_job` chụp `casing` trước khi việc rời luồng, nên phép bấm
  giữa lúc chạy không đổi được file đang ghi; giá phải trả thuần hiển thị là cột
  `N chữ sẽ mất` tính lại theo phép mà job đang chạy không có. Đây là lệch thật giữa
  ba shell, không phải lỗi của shell nào.

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

V1 **không thêm khoá nào vào file cấu hình**. Hai công tắc của thanh là lựa chọn của
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

1. Mở Chuyển mã → thanh Kiểu chữ, dán `xin chào. hôm nay trời đẹp.` rồi thử lần lượt
   năm nút; kiểm tra preview trước/sau khớp với bảng ở đầu tài liệu.
2. Dán đoạn có `TP. HCM` và một dòng xuống dòng không chấm câu: kiểm tra ngoại lệ
   ALL-CAPS và ranh giới dòng.
3. Bật công tắc **Giữ đ/Đ**, bỏ dấu `đẹp` → `đep`. Nó chỉ hiện khi `Bỏ dấu tiếng Việt`
   đang áp; **Hạ chữ viết hoa** cũng vậy với `Viết Hoa Đầu Mỗi Từ`.
4. Đặt đích TCVN3 rồi bấm CHỮ HOA: phải thấy dòng cảnh báo **nêu tên** ký tự sẽ mất —
   phép biến đổi chạy trước khi tính giá, nên chữ hoa TCVN3 không có chỗ mới lộ ra ở
   đây. Ở hình dạng batch, cột `N chữ sẽ mất` của từng dòng phải đổi theo.
5. Bấm chồng hai phép: dòng `Đang áp` phải viết đúng thứ tự đã bấm, và đổi thứ tự phải
   cho văn bản khác. **Hoàn tác** gỡ đúng phép cuối; **Bỏ hết** về nguyên bản.
6. Dán chuỗi không đoán được bảng mã (`hello world`): cả thanh phải mờ và không bấm
   được.
7. `echo "Tiếng Việt" | funput case --no-diacritics` cho `Tieng Viet` — CLI và ba cửa
   sổ gọi cùng một hàm, nên lệch nhau ở đây là lỗi nối dây của shell.
