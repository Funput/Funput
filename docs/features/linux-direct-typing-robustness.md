# RFC — Độ bền của chế độ gõ thẳng trên Linux

> **Sửa đúng chỗ hỏng · Đo trước khi xây · Giữ nguyên các bất biến đã trả giá mới có**
>
> Tài liệu này phân tích vì sao chế độ gõ thẳng (non-preedit) trên Linux chưa mượt
> như Windows và macOS, lấy issue [#640](https://github.com/Funput/Funput/issues/640)
> làm ca điển hình, rồi đề xuất kiến trúc và lộ trình sửa lỗi theo từng PR nhỏ.

| Thuộc tính | Nội dung |
| :--- | :--- |
| Trạng thái | **Đang triển khai** — PR 1/5 |
| Cập nhật | 02/11/2026 |
| Phạm vi | `platforms/linux` (composer dùng chung và hai shell Fcitx5, IBus). Không đụng engine Rust |
| Đối tượng | Maintainer, contributor và người dùng quan tâm kiến trúc |
| Liên quan | Issue [#640](https://github.com/Funput/Funput/issues/640); mục [Chế độ không preedit](../../platforms/linux/README.md#chế-độ-không-preedit) trong README Linux |
| Đã xác minh | Nguyên nhân A (tái hiện bằng test); cơ chế làm rơi lệnh xoá ở mutter, gnome-shell và Firefox (đọc mã nguồn upstream) |
| Cần đo trên máy thật | Nguyên nhân C; đường IM thực tế của Firefox snap/flatpak; khả năng vẽ preedit của Chrome Wayland native |

## 1. Đề xuất trong một phút

**Lỗi #640 có ba nguồn khác nhau, và chỉ một nguồn nằm hoàn toàn trong tay Funput.**

| Nguồn | Bản chất | Mức chắc chắn | Hướng xử lý |
| :--- | :--- | :--- | :--- |
| **A** | Composer không ghi nhận những thay đổi mà app tự thực hiện (space khi không soạn, Backspace app tự xoá), nên mất bằng chứng "tài liệu đang đồng bộ" | **Đã tái hiện** | Sửa ngay (PR 1) |
| **B** | Lệnh xoá rơi im lặng khi cầu nối mutter/gnome-shell chưa có, hoặc không còn, bản surrounding text đủ dài | **Đã xác minh trong mã nguồn** | Không phát lệnh xoá mà cầu nối chắc chắn bỏ (PR 4) |
| **C** | Firefox tính vị trí xoá dựa trên cache nội dung/selection của chính nó, có thể trễ một nhịp IPC | **Giả thuyết có cơ sở mã nguồn** | Đo bằng trace trước (PR 2), sau đó dựng tài liệu dự đoán (PR 3) |

Kiến trúc mục tiêu thêm **ba mảnh nhỏ** vào composer hiện có, không thay nó:

1. **Tài liệu dự đoán** (`PredictedDocument`): một journal ngắn các thay đổi đã biết trước,
   thay cho việc chỉ nhớ "lần sửa cuối cùng".
2. **Chặn lệnh xoá vô vọng** (`DeleteGuard`): không phát `deleteSurroundingText` khi chắc
   chắn bên nhận không có dữ liệu để thực hiện nó.
3. **Trace có kiểm soát quyền riêng tư** (`FUNPUT_TRACE`): để từ nay mọi chẩn đoán dựa trên
   dữ liệu thay vì suy đoán từ mô tả của người dùng.

Một số hướng nghe hợp lý đã được **cân nhắc và loại bỏ**, kèm lý do, ở [mục 8](#8-phương-án-đã-cân-nhắc-và-loại-bỏ).

## 2. Bối cảnh: cơ chế hiện tại

Chi tiết đầy đủ nằm ở README Linux. Tóm tắt những gì RFC này dựa vào:

- Mỗi phím, `Composer::onKey()` trả về một `ComposePlan`. Ở chế độ gõ thẳng đó là
  `Effect::Replace{deleteChars, text}`: xoá N **ký tự** trước caret rồi commit `text`. N đến
  thẳng từ engine (`FunputResult::backspace`), giống hệt đường inject của Windows.
- Shell thực hiện plan bằng `deleteSurroundingText(-N, N)` rồi `commitString(text)`
  (`fcitx5/src/funput_client.cpp`, `ibus/src/engine/client.cpp`).
- Các lần ghi **cố tình không được tuần tự hoá**. Thay vào đó, ở phím kế tiếp composer so
  bản đọc được với ba khả năng (`nonpreedit/observe.h`): đã áp dụng, lệnh xoá bị bỏ, hoặc
  client chưa trả lời. Chỉ khả năng thứ hai là một phán quyết.
- Hai thao tác cần **bằng chứng dương** rằng tài liệu đang đọc là mới (`inSync`): Funput tự
  thực hiện Backspace (`backspaceOutsideWord()`), và mở lại một từ đã xong để bỏ dấu lại
  (`adoptWordBeforeBackspace()`).
- Các lưới an toàn `BlindWrites`, `ClientMemory` và allowlist WPS rút chế độ về preedit cho
  những client bị bắt quả tang làm hỏng chữ.

Thiết kế này tốt ở chỗ phần lõi tách sạch khỏi framework, test được bằng engine thật, và
mọi quyết định khó đều có số đo đi kèm. RFC này **giữ nguyên** các bất biến ở [mục 6.1](#61-các-bất-biến-phải-giữ).

## 3. Issue #640: triệu chứng

Môi trường: Ubuntu 26.04, GNOME Wayland, Firefox bản snap và flatpak.

| # | Triệu chứng | Nơi gặp |
| :--- | :--- | :--- |
| 1 | Sau **1** dấu cách, Backspace rồi sửa dấu được; sau **2** dấu cách thì không | Thanh địa chỉ, ô tìm kiếm, tiêu đề Gmail |
| 2 | Space rồi Backspace không bao giờ sửa dấu được | Nội dung Gmail, ô bình luận diễn đàn (contenteditable) |
| 3 | Thỉnh thoảng lặp chữ (`sổ` thành `sôổ`), con trỏ nhảy về trước ký tự cuối | Ô soạn issue trên GitHub |
| 4 | Telex nâng cao: `[[` ra `ơ[` thay vì `[` | Tiêu đề issue GitHub, từ đầu tiên của phần nội dung |

## 4. Phân tích nguyên nhân

### 4.1. Đường đi của một lệnh sửa trên GNOME Wayland

Với ứng dụng Wayland native như Firefox trên Ubuntu 26.04, một `Effect::Replace` đi qua bốn
tầng, mỗi tầng giữ **bản sao riêng** của surrounding text:

```
Funput ──(xoá N ký tự, commit T)──► ibus-daemon / Fcitx5 (frontend ibus)
                                     │
                                     ▼
                        gnome-shell  (InputMethod: _surroundingText)
                                     │  ký tự → byte, dựa trên cache của mutter
                                     ▼
                        mutter       (text-input-v3: surrounding.text)
                                     │  delete_surrounding_text(byte)
                                     ▼
                        GTK3 imwayland (context->surrounding)  ──► Firefox (ContentCache)
                                        byte → ký tự                 tính vị trí xoá
```

Fcitx5 trên GNOME cũng đi qua đường này (frontend ibus → gnome-shell), nên các kết luận
dưới đây **áp dụng cho cả hai shell**.

### 4.2. Nguyên nhân A — chuỗi bằng chứng bị đứt (đã tái hiện)

Composer chỉ ghi nhận những lần ghi do chính nó phát ra (`NonPreeditState::noteRepair`).
Ba thay đổi mà app tự làm, hoàn toàn đoán trước được, lại **không được ghi nhận**:

| Thao tác | Vị trí trong mã | Hệ quả |
| :--- | :--- | :--- |
| Ranh giới từ (space, dấu câu) khi không có từ đang soạn | `Composer::onBoundary()`, nhánh `before.empty()` | `observe()` kế tiếp không có gì để đối chiếu và đặt `inSync = false` |
| Backspace mà Funput nhường cho app xoá | `Composer::backspaceOutsideWord()`, nhánh từ chối | Không khôi phục được `inSync` |
| Backspace trong từ đang soạn (app tự xoá ký tự) | `Composer::onKey()`, `KeyKind::Backspace` | Như trên |

Diễn biến với `phur␣␣⌫⌫s`:

1. Space thứ nhất kết thúc từ và được ghi nhận (`noteRepair(0, " ")`).
2. Space thứ hai đến khi không có gì đang soạn, đi thẳng qua mà không được ghi nhận.
3. Ở ⌫ thứ nhất, `observe()` đặt `inSync = false`. Funput nhường Backspace cho app, và cũng
   không ghi nhận.
4. Ở ⌫ thứ hai, `adoptWordBeforeBackspace()` từ chối vì `!inSync`. Phím `s` được gõ như chữ
   thường.

Tái hiện bằng test với engine thật:

| Client giả lập | `phur␣⌫s` | `phur␣␣⌫⌫s` |
| :--- | :--- | :--- |
| Trả lời đúng hạn ở mọi phím | `phú` ✅ | `phủs` ❌ |
| Trả lời đúng nhưng **trễ một phím** | `phủs` ❌ | `phủs` ❌ |

Dòng thứ nhất là lỗi logic thuần tuý, xảy ra cả khi client hoàn hảo. Dòng thứ hai cho thấy
riêng việc sửa A **chưa đủ** cho client trả lời trễ, ví dụ Firefox (xem 4.4). Đó là lý do có
`PredictedDocument` ở mục 6.

### 4.3. Nguyên nhân B — cầu nối không có dữ liệu để chuyển lệnh xoá (đã xác minh mã nguồn)

Protocol text-input-v3 đếm độ dài xoá bằng **byte**, còn IBus đếm bằng **ký tự**. Mutter
chuyển đổi bằng bản surrounding text nó đang cache
(`meta_wayland_text_input_focus_delete_surrounding`, `src/wayland/meta-wayland-text-input.c`):

```c
/* offset and len are counted by UTF-8 chars, but text_input_v3's lengths are
 * counted by bytes, so we convert UTF-8 char offsets to pointers here, this
 * needs the surrounding text */
before = g_utf8_offset_to_pointer (cursor, offset);
g_return_if_fail (before >= start);
```

Cache này **chỉ được cập nhật khi client gửi trạng thái mới** (`set_surrounding_text` kèm
`commit`). Từ đó có hai trường hợp lệnh xoá bị bỏ mà không ai được báo:

- **Cache ngắn hơn N ký tự**, điển hình là ở đầu ô nhập. `[` commit ra `ơ` khi cache còn rỗng;
  `[` tiếp theo phát "xoá 1, ghi `[`". Mutter không có ký tự nào để đổi,
  `g_return_if_fail` thoát sớm, và kết quả là `ơ[`. Khớp với triệu chứng 4, "từ đầu tiên của
  phần nội dung".
  `g_return_if_fail` chỉ ghi cảnh báo rồi `return`, không ném exception, nên nhánh `catch` thử
  lại trong `_onDeleteSurroundingText` của gnome-shell (`js/misc/inputMethod.js`) không chạy.
- **Sau một lần reset**: click chuột vào bề mặt đang focus làm mutter gọi
  `clutter_input_focus_reset`, và `vfunc_reset` của gnome-shell đặt `_surroundingText = null`.
  Cho tới khi client gửi lại, `_onDeleteSurroundingText` bỏ **mọi** lệnh xoá.

Hệ quả thực tế là lệnh xoá bị bỏ còn lệnh commit vẫn tới nơi. Bộ so ba chuỗi hiện có có thể
bắt được trường hợp này, nhưng chỉ **sau khi** chữ đã hỏng.

### 4.4. Nguyên nhân C — Firefox xoá theo cache của chính nó (giả thuyết)

`IMContextWrapper::DeleteText` (`widget/gtk/IMContextWrapper.cpp`) không xoá theo tài liệu
thật tại thời điểm nhận lệnh. Nó:

1. Lấy vị trí caret từ `mContentSelection`, một bản cache.
2. Gửi `eQueryTextContent`. Ở Firefox đa tiến trình (e10s), câu trả lời đến từ `ContentCache`
   của tiến trình cha. Nếu rỗng thì trả `FAILED`, và lệnh xoá bị bỏ.
3. Tính lại offset trên chuỗi đó rồi `DispatchSetSelectionEvent` để chọn đoạn cần xoá.

Nếu cache trễ một nhịp IPC so với lần commit vừa rồi, có hai kết quả:

- Cache rỗng hoặc quá ngắn: lệnh xoá bị bỏ, sinh ra lặp chữ dạng `sôổ` (triệu chứng 3).
- Cache có nhưng caret cũ: chọn sai đoạn, **xoá nhầm ký tự và con trỏ nhảy về trước ký tự
  cuối** (cũng triệu chứng 3).

Với editor contenteditable, nội dung cache phức tạp hơn ô một dòng, nên khả năng trả lời trễ
cao hơn. Kết hợp với 4.2, client trả lời trễ làm `inSync` gần như không bao giờ đúng, nên
không mở lại từ được (triệu chứng 2).

Đây vẫn là **giả thuyết**. Phép thử phân biệt B và C:

- Chạy Firefox trong phiên **X11**, hoặc với `GTK_IM_MODULE=ibus` để đi qua module IBus của
  GTK thay vì mutter. Nếu lỗi vẫn còn thì thủ phạm là Firefox, không phải mutter.
- Bật `MOZ_LOG=IMEHandler:5` để xem `DeleteText(), FAILED, ...` có xuất hiện không.
- Xác nhận Firefox snap/flatpak thực sự dùng module IM nào trong sandbox.

### 4.5. Điều đã loại trừ: hỏng UTF-8 do đổi byte

Một giả thuyết ban đầu cho rằng mutter đổi ký tự sang byte bằng cache cũ sẽ gửi số byte sai
(ví dụ xoá 1 byte của `ô` vốn chiếm 2 byte), làm vỡ UTF-8. **Giả thuyết này sai** và được ghi
lại ở đây để không ai đề xuất lại:

GTK3 (`text_input_delete_surrounding_text`, `modules/input/imwayland.c`) đổi byte **ngược về
ký tự** bằng `g_utf8_pointer_to_offset` trên `context->surrounding`. Đó chính là bản snapshot
nó đã gửi cho mutter, nên hai phép đổi triệt tiêu nhau và N ký tự vẫn là N ký tự. Chỉ còn một
cửa sổ race rất hẹp: client vừa gửi snapshot mới nhưng mutter chưa xử lý. Trường hợp đó hiếm,
và không giải thích được tần suất trong #640.

## 5. Đánh giá kiến trúc hiện tại

### Điểm mạnh nên giữ

- Composer không biết gì về Fcitx5 hay IBus. Mọi quy tắc gõ test được bằng engine Rust thật.
- Một chỉ thị duy nhất, "xoá N rồi ghi T", dùng chung với Windows. N đến từ engine, không
  diff chuỗi.
- Phán quyết **sau** khi ghi, không phải trước. README ghi lại những lần thử ngược lại đã thất
  bại, ví dụ chế độ không bao giờ bật trong Chrome.

### Điểm yếu mang tính cấu trúc

| Điểm yếu | Hệ quả |
| :--- | :--- |
| Bằng chứng đồng bộ là **một bit** dựa trên **một lần sửa** | Một thay đổi không được ghi nhận, hoặc một câu trả lời trễ, là đủ để mất khả năng sửa dấu cho tới khi người dùng gõ từ mới |
| Không phân biệt "client bỏ lệnh xoá" với "cầu nối không thể chuyển lệnh xoá" | Phát những lệnh xoá chắc chắn bị bỏ (đầu ô nhập, sau reset), rồi mới phát hiện khi chữ đã hỏng |
| Không có trace | Chẩn đoán phải dựa vào mô tả của người dùng. Bản thân README ghi `g_debug` của engine IBus đi vào `/dev/null` |
| Pipeline mỗi phím lặp ở hai shell | `fcitx5/src/funput_input.cpp` và `ibus/src/engine/callbacks.cpp` gần như giống hệt (chọn chế độ, observe, reopen, adopt), dễ lệch nhau theo thời gian |
| IBus coi là "không có tên client" | IBus có tín hiệu `focus-in-id(object_path, client)` với chuỗi như `gtk3-im:firefox`, `xim`, `gnome-shell`. Trên GNOME Wayland nó vẫn chỉ trả `gnome-shell`, nhưng với client X11/XWayland thì có tên thật |

## 6. Kiến trúc đề xuất

### 6.1. Các bất biến phải giữ

Mọi thay đổi trong RFC này phải giữ các quyết định đã được đo đạc trong README Linux:

1. **Không tuần tự hoá các lần ghi.** Không chờ client xác nhận trước khi phát lần ghi tiếp.
2. **Phán quyết chỉ đến sau một lần ghi**, không gác ở đầu vào dựa trên nội dung surrounding.
3. **Im lặng không phải là phán quyết.** Chỉ câu trả lời mới được tính (`BlindWrites`).
4. **Tên client rỗng không bao giờ được nhớ**, và `gnome-shell` không phải tên một app.
5. **Không đổi chế độ giữa một từ.** Việc chọn chế độ chỉ diễn ra giữa hai từ.
6. **Funput chỉ tự thực hiện Backspace khi tài liệu đồng bộ hoàn toàn.** Đọc trễ một nhịp
   chính là kịch bản vùng chọn cũ trong thanh địa chỉ Chrome.

### 6.2. Tổng quan

```
             ┌──────────── Shell adapter (Fcitx5 / IBus) ────────────┐
 phím,       │  chuẩn hoá sự kiện; thực hiện ComposePlan              │
 focus,  ───►│  ghi trace (nếu bật)                                   │
 surrounding └───────────────────────────┬────────────────────────────┘
                                         ▼
 ┌────────────────────────── common/compose ──────────────────────────────┐
 │  Composer (giữ nguyên) ── planFromResult ──► DeleteGuard ──► ComposePlan │
 │        │                                         ▲                      │
 │        ▼                                         │                      │
 │  PredictedDocument ◄── observeDocument ──────────┘                      │
 │  (journal ngắn các thay đổi đã biết; số lần ghi đã được xác nhận)        │
 │        │                                                                │
 │        ▼                                                                │
 │  Verdicts hiện có (observe 3 chuỗi, BlindWrites, ClientMemory)          │
 └────────────────────────────────────────────────────────────────────────┘
```

### 6.3. `PredictedDocument` — tài liệu dự đoán

**Vấn đề nó giải quyết:** nguyên nhân A, và triệu chứng 2 nếu C đúng.

**Thiết kế:**

- Thay cặp `repairText`/`repairDeleted` bằng một **journal có thứ tự**, giới hạn khoảng 4 mục.
  Gõ tay thực tế không bao giờ có quá 2–3 lần ghi chưa được trả lời.
- **Mọi** thay đổi đoán trước được đều vào journal, kể cả những thay đổi do app tự làm: ranh
  giới từ khi không soạn, Backspace nhường cho app, phím app tự gõ.
- Mỗi lần đọc được surrounding text, đối chiếu với các tiền tố của journal để biết **k trên n**
  lần ghi đã được xác nhận. Không cần thêm enum trạng thái; cặp (k, n) là đủ.
- Phép so "lệnh xoá bị bỏ" hiện có được giữ nguyên, áp dụng cho từng mục chưa xác nhận. Một
  bản đọc khớp với tiền tố cũ hơn có thể là client trả lời trễ, cũng có thể là client đã bỏ
  một lệnh sau đó.

**Quy tắc sử dụng:**

| Thao tác | Điều kiện hiện tại | Điều kiện mới |
| :--- | :--- | :--- |
| Mở lại từ để bỏ dấu (`adoptWordBeforeBackspace`) | `inSync` | Bản đọc khớp một tiền tố của journal. Từ được lấy từ **tài liệu dự đoán**, giống `CommittedTail` của Windows (`crates/funput-desktop/src/retone.rs`) |
| Funput tự thực hiện Backspace (`backspaceOutsideWord`) | `inSync` | **Giữ nguyên**: chỉ khi k == n (bất biến 6) |

Điểm mấu chốt: mở lại một từ **không ghi gì vào tài liệu**. Nếu từ đó sai thì engine từ chối
(không phải âm tiết tiếng Việt) hoặc lần sửa tiếp theo bị bộ so ba chuỗi bắt. Vì vậy nới điều
kiện cho thao tác này an toàn hơn nhiều so với nới điều kiện cho việc tự thực hiện Backspace.

### 6.4. `DeleteGuard` — không phát lệnh xoá vô vọng

**Vấn đề nó giải quyết:** nguyên nhân B, tức triệu chứng 4 và một phần triệu chứng 3.

**Phạm vi cố ý hẹp:** guard **không** cố đoán mọi lệnh xoá có thể thất bại (xem mục 8). Nó
chỉ xử lý những trường hợp mà Funput **biết chắc** bên nhận không có dữ liệu để thực hiện:

- Kể từ lần focus hoặc reset gần nhất, client chưa gửi surrounding text nào, hoặc bản đọc ngắn
  hơn `deleteChars`.
- **Và** đường truyền đi qua cầu nối gnome-shell (Fcitx5: `program() == "gnome-shell"`; IBus:
  `focus-in-id` trả `gnome-shell`).

**Hành vi đề xuất, cần đo trước khi chốt:** khi điều kiện trên đúng **ở đầu một từ**, từ đó
được soạn bằng preedit thay vì gõ thẳng. Quyết định nằm ở `applyNonPreeditMode()`, vốn chỉ
chạy giữa các từ, nên không vi phạm bất biến 5. README đã có tiền lệ: với IBus, "ô trống ngay
sau một ô trống: từ đầu tiên có gạch chân".

**Đánh đổi cần đo:**

- Nếu client trên cầu nối gnome-shell không vẽ preedit, từ đầu tiên sẽ vô hình cho tới ranh
  giới từ. Cần kiểm tra Chrome Wayland native, vốn đi qua cầu nối này.
- Đổi focus khi đang gõ dở từ đầu tiên sẽ phụ thuộc vào cơ chế flush preedit của framework.
  `PREEDIT_COMMIT` của IBus và lần flush khi focus-out của Fcitx5 đã xử lý việc này.

Nếu phép đo cho thấy đánh đổi không chấp nhận được, phương án dự phòng là chỉ **ghi trace** cho
trường hợp này, rồi quyết định tiếp dựa trên dữ liệu.

### 6.5. `FUNPUT_TRACE` — chẩn đoán dựa trên dữ liệu

- Bật bằng biến môi trường `FUNPUT_TRACE=1`, hoặc một khoá ẩn trong `settings.json`. **Mặc
  định tắt.**
- Ghi ring buffer ra `~/.cache/funput/trace.jsonl`, mỗi dòng là một sự kiện: phím (đã phân
  loại), plan, surrounding nhận được, cờ `answered`, phán quyết, timestamp đơn điệu.
- **Quyền riêng tư:** mặc định chỉ ghi độ dài, số ký tự và hash của nội dung. Nội dung gốc
  chỉ được ghi khi người dùng bật thêm một cờ riêng, và tài liệu hướng dẫn phải nói rõ điều đó.
  Funput là bộ gõ ưu tiên quyền riêng tư, nên trace không được trở thành keylogger.
- Dùng chung một định dạng cho cả hai shell, để trace từ người dùng phát lại được trong test.

### 6.6. `ClientSim` — mô phỏng client trong test

Một test double trong `common/tests` thay cho các client viết tay hiện có (`sync.h`,
`refusal.h`). Nó mô phỏng được:

- Độ trễ trả lời: đúng hạn, trễ k phím, không bao giờ trả lời.
- Cầu nối gnome-shell và mutter: cache chỉ cập nhật khi client trả lời; bỏ lệnh xoá khi cache
  ngắn; reset khi click. **Đổi byte và ký tự phải mô phỏng đủ cả hai chiều như GTK**, nếu không
  sẽ lặp lại sai lầm ở 4.5.
- Kiểu Firefox: xoá theo cache trễ một nhịp.
- Các client đã biết trong README: Chrome (bỏ lệnh xoá sau Backspace do app tự xử lý), Cursor
  (trả lời rỗng), WPS.

Mỗi triệu chứng của #640 trở thành một regression test.

### 6.7. Tên client cho IBus

Đăng ký `has-focus-id` và xử lý `focus-in-id` khi phiên bản IBus hỗ trợ (kiểm tra lúc chạy, có
đường lùi về `focus-in` như hiện tại). Truyền chuỗi `client` vào `Composer::onFocusChanged()`.
Các chuỗi `gnome-shell` và `fake` phải được coi là rỗng, cùng lý do như `clientId()` của Fcitx5.

Lợi ích: client X11/XWayland trên IBus được nhớ phán quyết qua các lần focus, đóng một hạn chế
README đang ghi. `DeleteGuard` cũng biết được khi nào đường truyền đi qua gnome-shell.

### 6.8. Hướng dài hạn (ngoài phạm vi RFC này)

- **Focus bridge**: một GNOME Shell extension (và KWin script cho KDE) xuất app id đang focus
  qua D-Bus. Đây là con đường duy nhất tới danh tính app thật trên Wayland. Nó mở khoá chọn chế
  độ theo app và cả **nhớ VI/EN theo app**, thứ README hiện ghi là giới hạn nền tảng. Cái giá
  là phải bảo trì extension theo từng phiên bản GNOME.
- **Gom pipeline mỗi phím vào `common/`**, sau một interface `ClientPort`, để hai shell chỉ còn
  làm adapter. Nên làm khi đụng vào phần này lần tới, nhưng không nằm trên đường găng của #640.
- **Hồ sơ client dạng dữ liệu** thay cho mảng hardcode ở `hidden_preedit.cpp`, cùng override
  theo app trong app Cài đặt. Chỉ có ý nghĩa khi đã có focus bridge, vì trên GNOME Wayland hiện
  không có khoá nào để gắn hồ sơ vào.

## 7. Lộ trình sửa lỗi

Mỗi bước là **một PR độc lập**, có test, và không phụ thuộc vào kết quả đo của bước sau.

| PR | Nội dung | Sửa được | Rủi ro | Độ tin cậy |
| :--- | :--- | :--- | :--- | :--- |
| **1** | Ghi nhận thay đổi do app tự làm | Triệu chứng 1 với client đúng hạn | Rất thấp | Cao |
| **2** | `FUNPUT_TRACE` và hướng dẫn thu thập | Chẩn đoán triệu chứng 2, 3 | Thấp | Cao |
| **3** | `PredictedDocument` cho việc mở lại từ | Triệu chứng 1 với client trễ; triệu chứng 2 nếu C đúng | Trung bình | Khá cao |
| **4** | `DeleteGuard` ở đầu từ trên cầu nối gnome-shell | Triệu chứng 4; một phần triệu chứng 3 | Trung bình, cần đo | Trung bình |
| **5** | `focus-in-id` cho IBus | Nhớ phán quyết trên X11; điều kiện cho PR 4 | Thấp | Trung bình |

### PR 1 — Ghi nhận thay đổi do app tự làm

**Trạng thái:** Đã triển khai trong PR này (`Composer::appPerforms()`, test
`nonpreedit/appedits_tests.cpp` và `nonpreedit/appedits_sync_tests.cpp`).

- `Composer::onBoundary()`, nhánh không có từ đang soạn: khi gõ thẳng đang bật, ghi
  `noteRepair(0, " ")` trước khi trả `passThrough()` — **chỉ với dấu cách thường** (U+0020). Ranh
  giới khác đi thẳng qua không ghi nhận: Tab thì app thụt lề hoặc hoàn thành chứ không chèn, còn
  `(`, `[`, `"` thì editor tự đóng cặp — bản ghi `(` sẽ được bản đọc xác nhận, Backspace kế tiếp
  thành lệnh xoá một ký tự của Funput và bỏ lại dấu đóng mà Backspace của chính app xoá cùng cặp.
  Mất `inSync` ở đó là phía an toàn.
- `Composer::onKey()`, Backspace trong từ đang soạn ở chế độ gõ thẳng: ghi `noteRepair(1, {})`.
- `Composer::backspaceOutsideWord()`, nhánh nhường cho app: **không ghi gì** (xem dưới).

Bản ghi chỉ được tạo khi `inSync` đang đúng (và không có vùng chọn): một thay đổi do app làm được
phép **giữ** bằng chứng đồng bộ, không được tạo ra nó. Với client trễ một phím, hai thay đổi giống
hệt nhau liên tiếp (␣␣, ⌫⌫) làm bản đọc cũ trùng đúng với điều bản ghi dự đoán; ghi khi chưa đồng
bộ sẽ cấp `inSync` cho một tài liệu đi sau thực tế một bước, Funput tự thực hiện Backspace và mở
lại từ từ bản đọc dài hơn một ký tự — `phur ␣ ␣ ⌫ s` thành `phủú`, `phur ab ⌫⌫⌫⌫ s` thành `pú`,
vi phạm bất biến 6. Nhánh nhường Backspace cho app vì thế không ghi: ba trong bốn điều kiện dẫn
tới đó cũng là điều kiện `appPerforms()` từ chối, còn điều kiện thứ tư (bản đọc rỗng khi đang đồng
bộ) thì trước con trỏ không còn gì để xoá nên không có gì để dự đoán. Lấy lại đồng bộ sau thay đổi
của app là việc của journal k trên n ở PR 3.

Các bản ghi này không bao giờ buộc tội được client **qua phép so ba chuỗi**. Bản ghi không xoá thì
"đã áp dụng" và "bị bỏ" là cùng một chuỗi. Bản ghi chỉ xoá mà không ghi gì thì đã được `observe()`
coi là không đủ để phán quyết. Riêng bản ghi có ghi chữ (dấu cách) vẫn được `BlindWrites` đếm như
một câu trả lời, y như những chữ cái app tự gõ (nhánh `ACTION_NONE`) xưa nay — hành vi giữ nguyên,
chỉ phát biểu lại cho đúng.

Test cần thêm:

- `phur␣␣⌫⌫s` → `phú` với client đúng hạn.
- Biến thể dấu câu: `phur,␣⌫s` → `phủ,s`, dừng ở dấu phẩy (đúng quy tắc tách từ hiện có).
- Dấu cách gõ trên vùng chọn không được ghi nhận; vùng chọn đang có thì không mở lại từ.
- Ranh giới không phải dấu cách (`(` được editor đóng cặp) đi thẳng qua; Backspace kế tiếp là của
  app.
- Client trễ một phím: `phur ␣ ␣ ⌫ s` → `phủ s`, `phur ab ⌫⌫⌫⌫ s` → `phs` — đúng những gì app tự
  làm, không hỏng chữ.
- Toàn bộ test hiện có vẫn pass.

### PR 2 — Trace và hướng dẫn thu thập

- Hiện thực [mục 6.5](#65-funput_trace--chẩn-đoán-dựa-trên-dữ-liệu) ở cả hai shell.
- Bổ sung mục "Gửi trace khi báo lỗi" vào README Linux.
- Nhờ người báo lỗi #640 gửi trace kèm ba phép thử ở [mục 4.4](#44-nguyên-nhân-c--firefox-xoá-theo-cache-của-chính-nó-giả-thuyết).

### PR 3 — Tài liệu dự đoán

- Hiện thực [mục 6.3](#63-predicteddocument--tài-liệu-dự-đoán), kèm `ClientSim` tối thiểu (độ
  trễ k phím).
- Test: `phur␣⌫s` và `phur␣␣⌫⌫s` ra `phú` với client trễ một phím. Funput không tự thực hiện
  Backspace khi k < n.
- Nếu trace ở PR 2 cho thấy Firefox trả lời trễ hơn một phím ở contenteditable, điều chỉnh độ
  dài journal theo số đo.

### PR 4 — Chặn lệnh xoá vô vọng

- Đo trước, ghi kết quả vào PR: Chrome Wayland native có vẽ preedit qua text-input-v3 không;
  Firefox, GTK4 và LibreOffice trên cầu nối gnome-shell hiển thị preedit ra sao.
- Hiện thực [mục 6.4](#64-deleteguard--không-phát-lệnh-xoá-vô-vọng), kèm mô phỏng cầu nối
  gnome-shell trong `ClientSim`.
- Test: `[[` → `[` và `soor` → `sổ` ở đầu ô nhập, với mô phỏng mutter.

### PR 5 — Tên client cho IBus

- Hiện thực [mục 6.7](#67-tên-client-cho-ibus).
- Cập nhật mục "IBus không biết tên client" trong README Linux.

## 8. Phương án đã cân nhắc và loại bỏ

| Phương án | Lý do loại |
| :--- | :--- |
| Kiểm tra độ dài byte trước mỗi lệnh xoá, hoãn phím khi không khớp | Dựa trên giả thuyết sai ở 4.5. Sẽ hoãn khoảng 39% số phím (tỷ lệ commit không được trả lời), và chính là kiểu tuần tự hoá README đã đo và loại bỏ. Hoãn phím trong Fcitx5 còn gây lỗi thứ tự với các phím sau, với auto-repeat, và với focus-out khi hàng đợi chưa xả |
| Bù số ký tự xoá để mutter ra đúng số byte | GTK đổi ngược bằng snapshot của chính nó, nên bù sẽ thành xoá thừa. Client đi thẳng qua IBus thì đếm ký tự, không có gì để bù |
| Mặc định soạn bằng preedit trên gnome-shell, chỉ dùng surrounding text khi mở lại từ (kiểu `fcitx5-unikey`) | Lần xoá khi mở lại từ chính là loại lệnh thanh địa chỉ Chrome hay bỏ (`RefuseRetone`), và cần đúng điều kiện đồng bộ mà Firefox hiếm khi đạt, nên không sửa được triệu chứng 2. Nó còn đưa lại rủi ro mất từ khi đổi focus, một trong hai lý do gõ thẳng ra đời |
| Hạ từ đang gõ về preedit giữa chừng khi phát hiện nguy cơ | Vi phạm bất biến 5: nửa từ nằm trong tài liệu, nửa còn lại trong preedit |
| Gửi phím BackSpace bằng `forwardKey` thay cho `deleteSurroundingText` | Tạo kênh thứ hai không có thứ tự so với commit (README đã lập luận cho "một kênh"), và lộ ra cho handler `keydown` của trang web. Có thể cân nhắc sau, chỉ làm phương án dự phòng cho client `RefuseRetone` |
| `commitStringWithCursor` của Fcitx5 | Không xoá được gì, và frontend ibus không chuyển nó đi |
| Bảng hồ sơ app và override trong Cài đặt ngay bây giờ | Trên GNOME Wayland mọi app đều là `gnome-shell`, nên chưa có khoá để gắn hồ sơ. Để dành tới khi có focus bridge |

## 9. Kế hoạch kiểm thử thủ công

Ma trận tối thiểu trước khi đóng #640:

| Phiên | Framework | Client |
| :--- | :--- | :--- |
| GNOME Wayland (Ubuntu 26.04) | IBus, Fcitx5 | Firefox snap, Firefox flatpak, Chrome, GNOME Text Editor (GTK4), LibreOffice |
| GNOME X11 hoặc `GTK_IM_MODULE=ibus` | IBus | Firefox (phép thử phân biệt B và C) |
| KDE Wayland | Fcitx5 | Firefox, Chrome, Kate |

Kịch bản cho mỗi ô: bốn triệu chứng ở mục 3, cùng các ca README đã ghi (Google Sheets, vùng chọn
bằng chuột, Cursor).

## 10. Câu hỏi mở

1. Firefox snap/flatpak trên Ubuntu 26.04 dùng module IM nào trong sandbox (`wayland` hay
   `ibus`)? Câu trả lời quyết định B hay C chiếm ưu thế.
2. Chrome Wayland native trên GNOME có vẽ preedit qua text-input-v3 không? Câu trả lời quyết
   định hành vi của PR 4.
3. Ở contenteditable, Firefox trả lời trễ bao nhiêu phím? Câu trả lời quyết định độ dài journal
   ở PR 3.
4. Phiên bản IBus tối thiểu có `focus-in-id` là bao nhiêu, và các distro mục tiêu đang ship bản
   nào?

## 11. Tài liệu tham khảo

Mã nguồn upstream, trích theo tên hàm vì số dòng thay đổi theo phiên bản:

- mutter, `src/wayland/meta-wayland-text-input.c`: `meta_wayland_text_input_focus_delete_surrounding`,
  `text_input_commit_state`, `meta_wayland_text_input_handle_event` (reset khi click).
- gnome-shell, `js/misc/inputMethod.js`: `_onDeleteSurroundingText`, `vfunc_reset`.
- GTK 3, `modules/input/imwayland.c`: `text_input_delete_surrounding_text`.
- Firefox, `widget/gtk/IMContextWrapper.cpp`: `IMContextWrapper::DeleteText`.
- IBus, `src/ibusengine.c`: tín hiệu `focus-in-id`, thuộc tính `has-focus-id`.
- Funput: `platforms/linux/README.md` (mục Chế độ không preedit),
  `platforms/linux/common/compose/composer/`, `crates/funput-desktop/src/retone.rs`.
