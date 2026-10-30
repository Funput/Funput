# Từ điển cá nhân

## Trạng thái

**Phần lõi Rust và C ABI đã hiện thực.** Phần này nằm trong `crates/funput-suggestions`
và `crates/funput-ffi`, và đáp ứng issue
[Funput/Funput#552](https://github.com/Funput/Funput/issues/552).

**Còn lại:**
- Giao diện iOS: trang Từ điển cá nhân và menu nhấn giữ trên thanh gợi ý.
- JNI và giao diện khi port sang Android.

Như [context-suggestion.md](context-suggestion.md), tài liệu này là nơi mọi quyết định
thiết kế sống. Thay đổi hành vi nào sau này cũng cập nhật lại tài liệu trong cùng PR.

## Mục tiêu

Kho từ đã học trở thành một từ điển người dùng **xem được và sửa được từng từ**.

Ràng buộc kế thừa từ gợi ý theo ngữ cảnh, không thương lượng:
- Đường truy vấn vẫn **0 cấp phát bộ nhớ** khi đã ấm, kể cả khi danh sách chặn không
  rỗng. Điều này được gác bởi `tests/alloc_budget.rs`.
- Không đụng tới `funput-core` hay đường xử lý phím.

## Ngữ nghĩa

| Thao tác | Kết quả |
|---|---|
| `Add` | Tạo bản ghi, hoặc đánh dấu bản ghi có sẵn là `Origin::Added`. `uses` được nâng lên ít nhất `promotion_uses` nên từ được gợi ý ngay, nhưng không vượt từ người dùng thực sự gõ. Từ tự thêm không bao giờ bị đẩy ra khi kho đầy. Thêm một từ đang bị chặn thì bỏ chặn từ đó. |
| `Forget` | Xoá bản ghi cùng các cạnh của nó. Gõ lại đủ số lần thì Funput học lại. |
| `Block` | Xoá bản ghi và đưa từ vào danh sách chặn. Từ đó không bao giờ được học hay gợi ý nữa, kể cả từ có trong từ điển tiếng Anh. |
| `Unblock` | Bỏ từ khỏi danh sách chặn. |
| `reset` | Xoá mọi từ, cả từ học lẫn từ tự thêm, **nhưng giữ danh sách chặn**: đó là lựa chọn của người dùng về những gì không muốn thấy, không phải thứ kho tự học. |

Các quy tắc chung:
- Mọi thao tác đều **idempotent**: áp lên một kho đã phản ánh nó thì trả `Unchanged`.
  Nhờ vậy, áp lại một thao tác mà journal hay inbox có thể đã giao rồi là vô hại.
- Giới hạn được kiểm tra **trước** khi đổi bất cứ thứ gì: `max_added_words` (mặc định
  1 000, luôn nhỏ hơn `max_words`) và `max_blocked_words` (mặc định 1 000). Một lần thêm
  bị từ chối thì không làm mất chặn; một lần chặn bị từ chối thì không làm mất từ.
- Danh sách chỉ liệt kê từ **đã đủ ngưỡng gợi ý** hoặc tự thêm. Phần lớn từ chưa đủ ngưỡng
  là gõ trượt, và một danh sách gồm mọi lần gõ trượt thì không phải từ điển.
- Tìm kiếm khớp ở bất kỳ vị trí nào, bỏ qua dấu và hoa thường: gõ `duoc` sẽ tìm ra `được`.
- Sắp xếp theo thứ tự chữ cái thì so dạng đã bỏ dấu trước, nên `ăn` đứng cạnh `an` thay vì
  sau `z`.

## Không có danh sách từ nhạy cảm

Issue #552 còn xin một tuỳ chọn "không gợi ý từ nhạy cảm". Bản thiết kế đầu có một danh
sách soạn sẵn kèm công tắc, nhưng đã được **bỏ trước khi merge**. Lý do:
- Kho cá nhân chỉ gợi ý lại những từ chính người dùng đã gõ, và không gửi gì ra ngoài.
  Funput tự quyết thay người dùng "từ này không nên gợi ý" là áp giá trị của mình lên họ.
- Nhu cầu thật (nhắn cho sếp, chia sẻ màn hình, đưa máy cho trẻ con) đã được đáp ứng bằng
  Block và menu nhấn giữ "Không gợi ý từ này". Người dùng tự chọn từ của mình.
- Danh sách soạn sẵn phải bảo trì liên tục và dễ sai (`dm` là đề-xi-mét, tiếng lóng đổi
  liên tục).

`is_known_word` vẫn trả `true` cho từ bị chặn: người dùng gõ từ đó có chủ ý, nên tính năng
sửa lỗi gõ nhầm không được sửa nó thành từ khác.

Blocklist của từ điển tiếng Anh vẫn giữ nguyên, vì từ điển đó gợi ý cả những từ người dùng
chưa từng gõ.

## Hai tiến trình

Trên iOS, app và bàn phím là hai tiến trình dùng chung kho trong App Group. Chỉ bàn phím
được ghi snapshot và journal.

| Bên | Gọi | Ghi gì |
|---|---|---|
| Bàn phím | `open`, `apply_edit`, `apply_pending_edits` | snapshot, journal, và xoá file inbox đã áp |
| App | `open_snapshot`, `post_edits` | chỉ các file mới trong `edits/` |

`post_edits` ghi mỗi lần gửi thành file riêng trong `edits/`:
- Thứ tự ghi: file tạm `.tmp`, fsync, rename, rồi sync thư mục. Bàn phím chỉ thấy file
  hoặc nguyên vẹn, hoặc không thấy gì.
- Tên file là `<tem>-<pid>-<bộ đếm>.edit`, sắp theo thứ tự gửi. Tem là đồng hồ tính bằng
  nano giây, nhưng **không bao giờ nhỏ hơn tem của file mới nhất còn chờ cộng một**. Nhờ
  vậy, đồng hồ có lùi (ví dụ NTP chỉnh giờ) giữa Chặn và Bỏ chặn thì hai thao tác vẫn được
  áp đúng thứ tự.
- Mỗi file chứa tối đa 256 thao tác; lần gửi lớn hơn được chia thành nhiều file liên tiếp,
  nên không file nào vượt giới hạn kích thước bên đọc chấp nhận.
- Từ dài hơn mọi token (32 ký tự) bị từ chối ngay bằng `InvalidInput` và không file nào
  được ghi. Các kiểm tra khác diễn ra lúc áp.
- Hai tiến trình không bao giờ ghi chung một file, nên không cần khoá.

`apply_pending_edits` đọc tối đa `MAX_INBOX_FILES` file theo thứ tự tên, áp các thao tác,
`flush` vào journal, và **chỉ sau đó** mới xoá file. Nếu crash ở giữa thì lần sau áp lại,
và vì thao tác idempotent nên không mất gì, không đếm trùng gì. Cách xử lý file đặc biệt:
- File hỏng: bàn phím xoá.
- File `.tmp`: bỏ qua, vì app có thể đang ghi dở.
- File của bản build mới hơn: để nguyên.

`open` tự rút inbox; nền tảng gọi thêm `apply_pending_edits` khi bàn phím hiện ra.

`open_snapshot` không ghi gì: không tạo thư mục, không sửa file, không compact. Nó áp sẵn
các thao tác còn chờ trong bộ nhớ, với đúng các giới hạn bàn phím sẽ áp, nên danh sách hiện
đúng trạng thái kho sắp có.

Mỗi thao tác được áp theo một trong ba chế độ (`Mode`):

| Chế độ | Khi nào | Kiểm tra giới hạn | Ghi journal |
|---|---|---|---|
| `Record` | Bàn phím sửa trực tiếp hoặc rút inbox | Có | Có |
| `Preview` | App xem các thao tác còn chờ | Có | Không |
| `Replay` | Đọc lại journal khi mở kho | **Không** | Không |

Replay không kiểm tra giới hạn vì thao tác đã được chấp nhận lúc ghi. Nếu không, một giới
hạn bị hạ sau đó sẽ âm thầm bỏ các thao tác chặn đã ghi, và lần compact kế tiếp xoá hẳn
chúng.

## Lưu trữ

| Định dạng | Thay đổi |
|---|---|
| Snapshot **v3** | Thêm một byte cờ cho mỗi từ (bit 0 = tự thêm), và danh sách chặn sau phần từ. Vẫn đọc được v1 và v2. |
| Journal **v4** | Byte cờ v2/v3 trở thành tag: `0`/`1` là học (không nối/có nối), `2`–`5` là add/forget/block/unblock. Vẫn đọc được v1–v3. |
| File inbox **v1** | Magic `FPED`, version, độ dài, CRC, rồi danh sách thao tác cùng mã tag. |

Ánh xạ tag sang `EditKind` chỉ nằm ở `persistence/codec/record.rs`.

Mỗi thao tác được áp thì tăng `sequence`. Lý do: một frame journal chỉ được replay khi nó
kết thúc sau sequence của snapshot. Nếu thao tác giữ nguyên sequence, frame của nó có thể
bị bỏ qua vì tưởng đã gộp vào snapshot.

Bản build cũ gặp journal v4 hoặc snapshot v3 sẽ báo `TooNew` và để nguyên file, như mọi lần
nâng schema trước.

## Xoá một từ

Từ nằm trong một `Vec` liền nhau; trie và cạnh follower tham chiếu tới từ bằng chỉ số.
`remove_slot` dùng `swap_remove`, sau đó:
- Từ bị dời vào chỗ trống nhận generation **lớn hơn mọi generation cả hai ô từng có**, nên
  không cạnh cũ nào trỏ nhầm vào nó.
- Cạnh trỏ tới từ bị xoá bị xoá theo, và số lần dùng của chúng được trừ khỏi
  `context_seen` của từ đứng trước. Nếu không, phép thử áp đảo vẫn chia cho những lần thấy
  một follower đã không còn, và ngữ cảnh đó im lặng rất lâu sau khi follower mới thế chỗ.
- Mọi cạnh trỏ tới ô cuối vừa trống cũng bị xoá: từ mới đẩy vào ô đó bắt đầu ở generation 0,
  nên nếu không xoá thì nó sẽ khớp nhầm với cạnh của người ở trước.
- Cạnh trỏ tới từ bị dời được chuyển sang chỗ mới.
- Trie được rebuild **một lần cho cả lô** thao tác.

`enforce_capacity` (dùng khi nạp kho vào một cấu hình nhỏ hơn) dùng lại đúng đường này, nên
giờ nó giữ được cạnh thay vì làm mất.

Từ tự thêm không bao giờ bị đẩy ra. Khi kho đầy mà toàn là từ tự thêm (chỉ xảy ra khi
`max_words` bị hạ), từ mới gõ không được học, còn `Add` mới trả `LimitReached`. Riêng `Add`
đọc lại từ journal vẫn được giữ, vượt sức chứa, như khi nạp một snapshot chứa nó.

## Bố cục mã

- `funput-suggestions/src/dictionary/`:
  - `edit/`: chế độ áp và lô thao tác (`mod`), từng thao tác (`words`), xoá một từ
    (`removal`).
  - `listing`: liệt kê, lọc, tìm, sắp xếp.
  - `blocked`: danh sách chặn.
  - `view`: `open_snapshot`, `post_edits`, `apply_pending_edits`.
- `funput-suggestions/src/persistence/`:
  - `inbox/`: thao tác file của inbox (`mod`) và cách đặt tên file theo thứ tự (`name`).
  - `codec/`: `frame` (khung có checksum dùng chung cho journal và file inbox), `record`,
    `journal`, `snapshot/`, `inbox`.
- `funput-ffi/src/suggestion/dictionary/`:
  - `edit`, `view`, `list`.
  - `types`: các hằng `FUNPUT_DICTIONARY_*` và struct `FunputDictionaryEntry`.

## Kiểm thử

- `src/tests/dictionary/`:
  - Ngữ nghĩa từng thao tác và các giới hạn.
  - Cạnh sau khi xoá, kèm proptest so với một mô hình đếm.
  - Round-trip snapshot/journal, crash giữa compact và truncate, đọc file v2/v3 cũ.
  - Inbox: rút, file hỏng, file của bản build mới hơn, giới hạn số file mỗi lần rút, từ
    quá dài, lần gửi lớn chia nhiều file, view không ghi gì.
  - Kho đầy toàn từ tự thêm, replay khi giới hạn đã hạ, ngữ cảnh nói lại sau khi follower
    bị quên.
- Test inline cho danh sách chặn, tên file inbox, codec record và codec inbox.
- `tests/alloc_budget.rs`: thêm ca tra cứu khi danh sách chặn không rỗng.
- `funput-ffi/tests/dictionary.rs`: round-trip qua C ABI, mã không hợp lệ, con trỏ null.
