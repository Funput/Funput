# Tự sửa lỗi gõ nhầm phím

## Trạng thái

**Thiết kế — chờ duyệt, chưa có code.** Mọi quyết định hành vi sống ở tài liệu này. Khi hiện
thực lệch khỏi bản viết, cập nhật lại tài liệu trong cùng PR.

## Vì sao

Người dùng gõ tiếng Việt không nhìn bàn phím thấy bàn phím hệ thống iOS "nhận phím chuẩn hơn"
Funput. Điều tra ngày 17/09/2026 (XCUITest trên bàn phím Telex hệ thống, iOS 27) cho thấy khác
biệt **không nằm ở vùng chạm**, mà ở bước **sửa lỗi sau khi gõ**.

| Gõ (nhầm phím bên cạnh) | iOS, ô nhập bật autocorrect | iOS, ô nhập tắt autocorrect |
|---|---|---|
| `nhsf` | **nhà** | nhsf |
| `vieeyj` | **việt** | viêyj |
| `tpoi` | **tôi** | tpoi |
| `khpong` | **không** | khpong |
| `dduowxj` | **được** | đượ |

- **Vùng chạm cố định:** vùng chạm của bàn phím Telex hệ thống không đổi theo ngữ cảnh. Sau `ng`, sau `gh` hay đầu từ, ranh giới giữa hai phím đều như nhau.
- **Mặc định bật:** hầu hết app (Tin nhắn, Ghi chú, Zalo, Messenger) bật autocorrect, nên người dùng luôn có bước sửa này mà không biết.
- **Funput:** không có bước tương đương.

Chi tiết đo: [KEY_ACCURACY_INVESTIGATION.md](../../platforms/ios/docs/KEY_ACCURACY_INVESTIGATION.md).

## Mục tiêu

Khi người dùng kết thúc một từ (dấu cách, dấu câu, Enter) mà từ vừa gõ **không phải âm tiết
tiếng Việt hợp lệ**, Funput thay nó bằng âm tiết hợp lệ gần nhất theo **vị trí chạm thật** của
từng phím, giống cách bàn phím hệ thống sửa `nhsf` thành `nhà`.

Ràng buộc:

1. **Không bao giờ sửa một từ hợp lệ.** Chỉ từ không thành âm tiết mới là ứng viên. Đây là
   điều kiện cứng, không phải ngưỡng điểm.
2. **Tôn trọng ô nhập.** Tắt khi host đặt `autocorrectionType = .no` (iOS) hoặc
   `TYPE_TEXT_FLAG_NO_SUGGESTIONS` / ô mật khẩu (Android), và khi người dùng tắt cài đặt.
3. **Hoàn tác một chạm.** Bấm Xoá ngay sau khi sửa thì trả lại đúng chữ đã gõ, như iOS.
4. **Không làm chậm luồng gõ.** Chỉ chạy ở ranh giới từ, có trần số phép thử cố định.
5. **Một bản cho mọi nền tảng.** Lõi nằm trong Rust (`funput-engine`), iOS và Android gọi
   qua `funput-ffi` / `funput-jni`.

## Không thuộc phạm vi

- Sửa từ tiếng Anh, hay sửa khi đang ở chế độ tiếng Anh.
- Sửa từ hợp lệ nhưng "có vẻ sai" (ví dụ `bạn` → `bạn` khác dấu). Cần mô hình ngôn ngữ; để sau.
- Thêm hoặc bớt ký tự (`nhaaf` thiếu hoặc thừa phím). Bản đầu chỉ xét **thay phím**.
- Vùng chạm thay đổi theo ngữ cảnh. Bàn phím Telex hệ thống không làm việc này.
- Bảng tần suất từ tiếng Việt đóng gói kèm app. Quyết định 18/09/2026: bản đầu xếp hạng bằng
  điểm chạm + từ người dùng đã học. Bảng tĩnh, nếu có, sẽ là một nguồn điểm thứ hai ghép vào
  mục "Chấm điểm".

## Hành vi

### Khi nào chạy

Cần đủ **tất cả** các điều kiện sau:

- Từ vừa kết thúc bằng dấu cách, dấu câu hoặc Enter.
- Ngôn ngữ đang là tiếng Việt, kiểu gõ Telex, Telex nâng cao hoặc VNI.
- Từ **không** thành âm tiết hợp lệ theo `funput-core` (`is_complete_syllable`), sau khi đã
  qua smart restore. Từ đã được restore thành tiếng Anh thì không đụng tới.
- Từ không phải gõ tắt, không chứa chữ số, không toàn chữ hoa.
- Ô nhập cho phép autocorrect và cài đặt "Tự sửa lỗi gõ" đang bật.

### Sửa

- Thay từ trong tài liệu bằng ứng viên thắng, giữ nguyên ký tự kết thúc từ (dấu cách hoặc dấu câu).
- Giữ nguyên kiểu chữ: `Nhsf` → `Nhà`.
- Thanh gợi ý hiện chip "↩ nhsf" trong lúc người dùng chưa gõ phím khác. Chạm chip là hoàn tác.

### Hoàn tác

- Bấm Xoá **ngay** sau khi sửa: xoá ký tự kết thúc từ và trả từ về đúng chữ đã gõ. Từ đó được
  đánh dấu "không sửa lại" trong phiên.
- Gõ bất kỳ phím nào khác thì mất khả năng hoàn tác một chạm, giống iOS.

## Sinh ứng viên

Đầu vào cho mỗi từ là chuỗi phím đã gõ, mỗi phím kèm **điểm chạm** tính theo đơn vị bước phím
(pitch):

```
KeyTouch { key: 's', dx: -0.31, dy: +0.08, alternates: [('a', d=0.62), ('d', d=0.74), ...] }
```

- **Phím thay thế:** mỗi phím có tối đa 3 phím thay thế, là các phím có tâm nằm trong 1,2 bước
  phím tính từ điểm chạm. Nền tảng tính danh sách này từ hình học đang hiển thị. Lõi không cần
  biết bố cục.
- **Số phím thay tối đa:** 2 phím mỗi từ. Từ dài tối đa 10 phím. Trần phép thử:
  `1 + 10×3 + C(10,2)×9 = 436` chuỗi. Mỗi chuỗi được phát lại qua bộ ghép dấu (vài micro giây),
  nên tổng dưới 2ms.
- **Lọc:** chỉ giữ chuỗi mà khi phát lại qua bộ ghép dấu cho ra **âm tiết hoàn chỉnh hợp lệ**.
- **Phím điều khiển dấu:** các phím ghép dấu của Telex (`s f r x j w z`, lặp nguyên âm) và VNI
  (`1`–`9`) được đối xử như mọi phím. Nhờ vậy `nhsf` → thay `s` bằng `a` → `nhaf` → `nhà`, và
  `dduowxj` → thay `x` bằng `c` → `dduowcj` → `được`.

## Chấm điểm

```
score(ứng viên) = Σ log P(chạm | phím)  +  log prior(từ)  −  λ × số phím thay
```

- **`P(chạm | phím)`:** phân phối chuẩn theo khoảng cách từ điểm chạm tới tâm phím, σ = 0,45
  bước phím. Phím giữ nguyên cũng được tính.
  - Nhờ điểm chạm thật, chạm sát mép `a` mà ra `s` thì ứng viên `a` đắt hơn rất ít, còn chạm giữa
    `s` thì rất đắt. Đây là lợi thế so với bàn phím hệ thống, vốn chỉ biết phím nào kề phím nào.
- **`prior(từ)`:** lấy từ kho từ người dùng đã học của `funput-suggestions` (số lần gõ, có làm
  mịn), cộng một mức nền cho mọi âm tiết hợp lệ. Từ người dùng hay gõ thắng từ hiếm.
- **`λ`:** phạt mỗi phím thay, để một phím thay luôn được ưu tiên hơn hai.

**Điều kiện sửa:** ứng viên tốt nhất phải hơn ứng viên thứ hai một biên `Δ`. Nếu hai ứng viên
sát nhau (ví dụ `nhà` và `nhá`), **không sửa**, chỉ đưa cả hai lên thanh gợi ý. Sửa sai tệ hơn
không sửa.

`σ`, `λ`, `Δ` là hằng số khởi điểm, chỉnh bằng bộ kiểm thử ở mục cuối.

## Vị trí trong code

| Tầng | Việc |
|---|---|
| `funput-engine` (mới: `correction/`) | Sinh và lọc ứng viên bằng bộ ghép dấu có sẵn; chấm điểm; trả về `Correction { replacement, runner_up, original }` hoặc không có gì. |
| `funput-suggestions` | Thêm hàm tra cứu tần suất một từ đã học (đọc, không khoá luồng gõ). |
| `funput-ffi` / `funput-jni` | Một hàm `funput_correct_word(keys, touches, prior)` theo khuôn các hàm hiện có. |
| iOS `KeyboardInput` | Giữ `KeyTouch` của từ đang gõ (pipeline chạm đã có toạ độ); gọi sửa ở ranh giới từ; ghi tài liệu bằng xoá + chèn như smart restore; hoàn tác bằng Xoá. |
| iOS Settings | Công tắc "Tự sửa lỗi gõ" trong nhóm Thông minh. |
| Android | Cùng lõi; ghi bằng một batch edit như retone. |

**Còn mở:** có nên để `funput-engine` phụ thuộc `funput-suggestions`, hay nền tảng truyền
`prior` vào như một bảng nhỏ gồm các ứng viên đã lọc? Đề xuất cách thứ hai: lõi trả về tối đa 8
ứng viên hợp lệ kèm điểm chạm, nền tảng hỏi tần suất rồi gọi bước chọn cuối. Như vậy hai crate
vẫn độc lập như hiện nay.

## Chỉ số

- `correctionsApplied`
- `correctionsReverted` (bấm Xoá ngay sau khi sửa)
- `correctionsSkippedAmbiguous`

Tỉ lệ hoàn tác cao là tín hiệu `Δ` quá thấp. Chỉ đếm, không lưu nội dung.

## Kiểm thử và cổng gác

1. **Kho lỗi tổng hợp:** lấy danh sách âm tiết hợp lệ (bộ phủ `funput dev coverage`), gõ lại
   bằng mô phỏng chạm có nhiễu Gauss quanh tâm phím, giữ lại các chuỗi bị nhầm phím.
   - **Đo:** tỉ lệ sửa đúng, tỉ lệ sửa sai, tỉ lệ bỏ qua.
   - **Cổng:** sửa sai < 1% số lần sửa.
2. **Bất biến:** mọi âm tiết hợp lệ gõ đúng phím thì không bao giờ bị đổi. Property test trên
   toàn bộ tập âm tiết.
3. **Ca đã đo trên iOS:** `nhsf`, `vieeyj`, `tpoi`, `khpong`, `dduowxj` phải ra như bàn phím hệ
   thống, với điểm chạm lệch về phía phím đúng.
4. **Hiệu năng:** trần 436 lần phát lại, đo p99 < 2ms trên thiết bị cũ nhất hỗ trợ.
5. **UI test iOS:** gõ `nhsf ` trong ô bật autocorrect ra `nhà `; trong harness tắt autocorrect
   ra nguyên văn; bấm Xoá ngay sau khi sửa ra `nhsf`.

## Thứ tự hiện thực

1. `funput-engine::correction`: sinh, lọc, chấm (prior đều) + bộ kiểm thử tổng hợp.
2. Tra cứu tần suất trong `funput-suggestions`; FFI/JNI.
3. iOS: ghi `KeyTouch`, sửa ở ranh giới từ, hoàn tác, chip trên thanh gợi ý, công tắc, tôn trọng `autocorrectionType`.
4. Android: tương tự.
5. Chỉnh `σ`, `λ`, `Δ` theo kho lỗi tổng hợp và phản hồi TestFlight.
