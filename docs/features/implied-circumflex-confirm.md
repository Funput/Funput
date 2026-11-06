# Gõ `e` sau mũ tự thêm của `iê`

> **Trạng thái:** đã làm (PR #…). Sửa lỗi, không thêm công tắc. Chưa thử tay trên Windows thật.

Nguồn: issue #582 (Windows), mục "Về cách đặt dấu câu mới".

## Vấn đề

Funput tự thêm mũ cho `iê` khi đặt dấu thanh: `ie` + phụ âm cuối (hoặc `u`) bắt buộc là `iê`, nên `viecj` ra `việc` ([tone_position.rs](../../crates/funput-core/src/orthography/tone_position.rs)). Người quen Unikey gõ **dấu thanh trước, mũ sau** (`viecje`), và phím `e` cuối bị hiểu là **gỡ mũ**:

| Gõ (Telex) | Hiện tại | Mong muốn |
|---|---|---|
| `viecj` | việc | việc |
| `vieecj`, `viecej`, `vieejc` | việc | việc |
| `viecje` | **viẹce** | việc |
| `tiengse` | **tiénge** | tiếng |
| `dienje` | **diẹne** | diện |
| `tienges`, `dongso`, `muonjo`, `xuongso` | tiếng, dống, muộn, xuống | không đổi |

Đo bằng `funput dev run -m telex` (bản release ngày 2026-11-02). Chạy lại trên code mới nhất trước khi sửa.

Nguồn gốc lỗi theo `--steps` của `viecje`: phím `j` ra `việc` (mũ tự thêm), phím `e` gặp `ê` sẵn có nên đi vào nhánh `revert` của [circumflex.rs](../../crates/funput-core/src/composition/intent/kinds/circumflex.rs): gỡ mũ rồi chèn `e` thô, ra `viẹce`. Funput gỡ một cái mũ mà người dùng chưa hề gõ.

Các vần khác không bị vì không có mũ tự thêm: `dongs` ra `dóng`, phím `o` sau đó đội mũ bình thường.

VNI có cùng lỗi với phím `6`: `viec56` ra `viẹc6`, `tieng16` ra `tiéng6`, `dien56` ra `diẹn6` (phím
`6` đi qua `try_revert_shape(Circumflex)` trong `transform/action.rs`). `viec65`, `viec5`, `dong16`,
`muon56` không bị.

## Quyết định

**Mũ do Funput tự thêm thì phím `e` gõ sau là xác nhận, không phải gỡ.** Phím đó bị nuốt, chữ giữ nguyên.

- Chỉ gỡ mũ khi chính người dùng đã gõ mũ (`ee`) rồi gõ `e` thêm lần nữa, như hiện nay.
- Sau khi xác nhận, phím `e` tiếp theo gỡ mũ như thường (`viecjee` ra `viẹce`), giống người dùng đã tự gõ mũ.
- **Không thêm công tắc** bật/tắt việc tự thêm mũ. Sửa xong thì mọi thói quen (`viecj`, `vieecj`, `viecej`, `viecje`) đều ra `việc`, không ai phải đổi cách gõ. Tắt việc tự thêm mũ còn làm hỏng quy tắc `giefm` → `gièm` / `giemf` → `giềm` dựa vào chỗ gõ dấu, đã ghi ở `reposition_existing_tone`.
- **Không chuyển sang sửa lỗi gõ** như issue gợi ý. Quy tắc `iê` là chính tả chắc chắn; sửa lỗi gõ dựa trên xác suất, chạy ở cuối từ, và có thể sai.
- `ngwuoif` → `người`: đã chạy với mọi thứ tự của `w` (`ngwuoif`, `nguoiwf`, `nguowif`, `nguwoif`, `nguoifw`). Không đổi gì.

## Cách nhận biết mũ tự thêm

Lớp `funput-core` chỉ thấy buffer, không thấy phím thô, nên chỉ nhìn buffer thì không phân biệt được `việc` từ `viecj` với `việc` từ `vieecj`. Hai cách:

1. **Engine quyết định** (đề xuất): engine có phím thô (`Session::keys`). Trước khi đưa phím `e` vào `funput-core`, nếu nguyên âm `ê` mục tiêu là `iê` và phím thô chỉ có **một** `e` (không tính phím `e` đang gõ) cho nguyên âm đó, thì nuốt phím: giữ buffer, chỉ thêm phím vào `keys` để lật từ và English restore vẫn thấy đúng phím đã gõ.
2. **Ghi dấu lúc tự thêm:** `funput-core` báo lại khi đặt dấu thanh đã tự thêm mũ, engine giữ một cờ cho từ đang gõ. Chính xác hơn khi trong từ có nhiều `e`, nhưng phải luồn thêm thông tin qua `ComposeResult`.

**Đã chốt cách 1**, trong `crates/funput-engine/src/compose/pipeline/confirm.rs`, không đụng
`funput-core`, không thêm field vào `Session`, không đổi FFI. Quy tắc đếm phím:

- Chỉ xét khi core trả `TransformKind::Reverted` cho phím vừa gõ. Trên phím mũ, một `Reverted` chỉ có
  thể là gỡ mũ (Telex `e` chỉ đi vào intent mũ; VNI `6` chỉ đi vào nhánh mũ của `action.rs`).
- Phím mũ là `e`/`E` với Telex và Telex nâng cao, `6` với VNI.
- Các phím gõ trước đó của từ (`keys` bỏ phím vừa gõ) phải toàn ASCII. Sau một lần gỡ, sau Backspace
  hay sau `adopt`, `keys` được viết lại từ chữ trên màn hình (`việc`) nên không còn biết ai gõ mũ; từ
  đó giữ hành vi gỡ cũ. Đây cũng là điều kiện `doubled::on_revert` đang dùng.
- Đếm số phím mũ trong các phím trước đó (không phân biệt hoa thường): phải ít hơn số phím một chữ
  `ê` do người dùng gõ cần, 2 với Telex (`ee`), 1 với VNI (`6`).
- Chữ đang hiện trên màn hình (buffer trước phím này) phải là một âm tiết tiếng Việt hoàn chỉnh
  (`SyllableRules::is_complete_syllable`, cùng phép kiểm tra ranh giới từ dùng) **với phụ âm đầu
  chuẩn**: có phụ âm đầu (không onset thì tiếng Việt viết `yê`), không phải `qu` (viết `quyê`), và
  nằm trong bảng phụ âm đầu chuẩn (`b c ch d đ g gh h k kh l m n ng ngh nh p ph r s t th tr v x`),
  không nhận các cụm dân tộc/địa danh mà bảng vần cho phép (`br`, `pr`, `dr`); `z f w j` chỉ được
  nhận khi người dùng đã bật phụ âm đầu mở rộng. Xác nhận nghĩa là người dùng đang gõ xong chính từ
  tiếng Việt mà Funput đã thêm mũ; từ tiếng Anh có `ie` + phụ âm + `e` (`directed` hiện `diểct`,
  `Irene` hiện `Iển`, `quiesce` hiện `quiếc`, `briefness` hiện `briền`) hay từ chưa xong (`giếc`
  không có trong bảng vần, `viêc` sau khi `z` bỏ dấu) không phải từ đó, và lần gỡ mũ như cũ, vốn mở
  doubled-key restore đưa `directed` về, vẫn đúng với chúng. Bảng phụ âm đầu nằm trong `confirm.rs`
  vì core không công khai bộ phân loại phụ âm đầu. Phép kiểm tra âm tiết cấp phát một chuỗi vần nên
  chỉ chạy sau khi các điều kiện rẻ ở trên đã qua.

Khi xác nhận, engine trả `Send` với backspace 0 và output rỗng; `buffer`, `vn_form`, `literal` và
`restore_override` giữ nguyên, `keys` vẫn giữ phím vừa gõ. Không cấp phát thêm
(`tests/alloc_budget/pairs.rs`).

## Ảnh hưởng

- **Lật từ:** dạng phím thô vẫn là mọi phím đã gõ (`viecje`), không bỏ phím `e` bị nuốt.
- **English restore:** từ dùng phím thô để quyết định. `viecje` là một âm tiết tiếng Việt đúng nên không restore.
- **Mở lại sau Backspace (`adopt`):** phím thô được dựng lại từ chữ, không có thông tin mũ tự thêm. Coi mọi `ê` của từ được mở lại là người dùng đã gõ, giữ hành vi gỡ mũ như hiện nay.
- **Mọi nền tảng:** sửa trong engine nên Windows, macOS, Linux, iOS, Android cùng được.

## Hợp đồng với nền tảng

Phím bị nuốt là `Action::Send` với backspace 0 và output rỗng. macOS, Linux (preedit và non-preedit),
iOS, Android và funput-term đã coi `Send` rỗng là nuốt phím. Chỉ hook Windows và harness test của
`funput-desktop` từng coi kế hoạch rỗng là cho phím đi qua, nên đổi hợp đồng của `funput-desktop`:

- `plan_inject(&ImeResult) -> Option<InjectPlan>`: `None` khi `Action::None` (cho phím đi qua); `Some`
  thì nuốt phím, kể cả khi kế hoạch rỗng.
- `InjectPlan::is_noop` đổi tên thành `is_empty`: chỉ nói "không có gì để gửi", không quyết định có cho
  phím đi qua hay không.
- Hook Windows (`keydown.rs`) `match` trên `Option`; phím lật từ (`keyboard/mod.rs`) dùng `if let Some`.
  Mới biên dịch chéo, **chưa thử tay trên Windows thật**.

Phím xác nhận không phải `Send` rỗng duy nhất: engine đã trả `Send` rỗng ở vài chỗ khác, và hook
Windows cũ cho các phím đó đi qua thành chữ thường mà engine không biết. Đổi hợp đồng sửa luôn các
lệch đó trên Windows (`tests/swallow.rs`; các nền tảng khác vốn đã nuốt):

| Gõ | Windows cũ | Windows mới (= macOS/Linux) |
|---|---|---|
| Telex `cuwuu` | cưuu | cưu |
| VNI `uu7u` | ưuu | ưu |
| Telex nâng cao, eager restore tắt, `Newsweek` | NNewsweek | Newsweek |
| Telex nâng cao, `slowworm` | slowworm | sloworm |

Ba dòng đầu là sửa lỗi; dòng cuối là Windows về cùng hành vi với các nền tảng khác (`ww` của Telex
nâng cao nuốt phím), còn việc engine xử lý `slowworm` là việc riêng.

## Tác dụng phụ đã biết

Phím mũ xác nhận bị nuốt, nên phím gõ tiếp theo tác động lên chữ đã có dấu chứ không lên chữ đã gỡ mũ
(ghi trong test `known_side_effects`):

| Gõ | Trước | Sau |
|---|---|---|
| `giemfe` (Telex) | gième | giềm |
| `viecjse` | viéce | viếc |
| `viecjej` | viecej | viêcj |
| `viec566` (VNI) | viẹc66 | viẹc6 |
| `viec516` | viéc6 | viếc |

Không đổi nhờ điều kiện âm tiết hoàn chỉnh: `viecjze` vẫn ra `viece`, VNI `viec506` vẫn ra `viec6`,
`giecse` vẫn ra `giéce`.

Sau khi xác nhận, phím mũ tiếp theo gỡ mũ như cũ: `viecjee` ra `viẹce`, `viecjeee` ra `viẹcee`,
`chieufee` ra `chièue`; VNI `viec566` ra `viẹc6`.

**Tiếng Anh:** quét `en.tsv` (25 979 từ) và `/usr/share/dict/words` (235 976 từ, gõ như nguyên bản và
chữ thường) qua Telex và VNI với eager restore mặc định, so với bản gốc: `en.tsv` không đổi dòng nào;
VNI không đổi dòng nào; Telex đổi 16 từ của `dict`, tất cả vốn đã sai ở bản gốc (`direness` gốc ra
`diẻness`, nay `diêns`; `bireme` gốc `biẻme`, nay `biểm`; `disremember` gốc `diemember`, nay đúng
`disremember`). Chúng là những từ mà 4–5 phím đầu đúng là cách gõ UniKey của một âm tiết có phụ âm
đầu chuẩn (`diển`, `biểm`, `siển`); không từ nào đang đúng mà thành sai. Sinh lại `data/doubled/`
bằng `funput-lexicon-tool doubled` bỏ 3 từ không còn bị gỡ mũ trong bất kỳ setup nào (`disrespect`,
`disrespectful`: `diếp` là âm tiết chuẩn; `firemen`: `fiểm` ở setup bật `f`) và 2 tiền tố trong
`decisive.txt`; cả ba vẫn ra đúng ở ranh giới từ.

## Hướng dẫn

Thêm vào trang Telex trên website: Funput tự thêm mũ cho `iê` khi có phụ âm cuối (`viecj` ra `việc`, `tiengs` ra `tiếng`), và gõ thêm `e` như thói quen cũ cũng ra cùng chữ. Ghi tương tự cho VNI nếu áp dụng.

## Ngoài phạm vi

Funput chỉ tự thêm mũ cho `iê`; `uô` thì không (`muonj` ra `muọn`, phải gõ `muoonj` hoặc `muonjo` để có `muộn`). Hai vần đang không nhất quán. Mở rộng việc tự thêm mũ là thay đổi cách gõ, bàn riêng.

## Kiểm thử

- `crates/funput-engine/src/compose/pipeline/confirm.rs`: unit test cho điều kiện xác nhận.
- `crates/funput-engine/tests/diacritics/implied_circumflex.rs`: các dòng của bảng Vấn đề, cả chữ hoa
  (`VIECJE`, `Viecje`); `viecjee` → `viẹce`; `vieecje` → `viẹce` (mũ do người dùng gõ, `e` thứ ba gỡ
  mũ như cũ); `giefm`/`giemf` không đổi; `ye`/`uye`; Telex nâng cao; VNI; lật từ; Backspace và `adopt`
  giữ hành vi gỡ cũ; tiếng Anh; sửa lỗi gõ; bảng tác dụng phụ.
- Lật từ trên `viecje` ra phím thô đủ `viecje`.
- `crates/funput-desktop/tests/swallow.rs`: shell hook + inject nuốt phím xác nhận (`viecje ` ra
  `việc `), phím thường vẫn đi qua.
- Differential test của engine (so 70k chuỗi phím với worktree gốc): chỉ được đổi các chuỗi có phím `e` sau mũ tự thêm của `iê`. Liệt kê các chuỗi bị đổi để duyệt, không tự sửa cho hết khác biệt.
- `tests/alloc_budget/pairs.rs`: `viecje tiengse dienje chieufe hieure` không cấp phát nhiều hơn
  `vieecj tieengs dieenj chieeuf hieeur`.
