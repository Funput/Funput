# P6 — nghiệm thu nhập bằng giọng nói Android

Trạng thái ngày 03/10/2026: **đang nghiệm thu, production vẫn tắt**.
Thiết kế và contracts ở [tài liệu chính](../android-speech-to-text.md).
Kết quả kỹ thuật ở [report](report-2026-10-03.md); thao tác trên máy thật ở
[checklist](checklist.md). Không coi capability, preview hoặc regression dùng
fake backend là bằng chứng nhận dạng lời nói offline.

Ngày 03/10/2026, người dùng yêu cầu **không kiểm tra tiếng Anh**. EN được loại
khỏi phạm vi nghiệm thu này, không phải PASS và không còn là gate chặn P6.
Không thay code hỗ trợ EN hoặc tự tải model. Các ngưỡng tiếng Việt giữ nguyên.

## Phân công

- Agent: build/test/lint, manifest/APK/dependencies, chuẩn bị test host, tổng hợp
  phép đo thật, sửa lỗi và cập nhật bằng chứng.
- Người thử: nói trực tiếp, bật/tắt mạng, cấp/thu hồi quyền, khoá màn hình,
  thử editor/phụ kiện/cuộc gọi và đánh giá chữ được chèn.
- Cả hai: đối chiếu kết quả, xử lý lỗi và quyết định gate. Chỉ bật production
  khi đầy đủ mọi mục; không bao gồm deploy hoặc thay release version.

## Hai thiết bị

| Device ID cho CSV | Máy | Android/API | App dùng để thử |
| --- | --- | --- | --- |
| `samsung-a` | Samsung SM-G998B (S21 Ultra) | 15/35 | Debug `app.funput.funput` |
| `fold-b` | Samsung SM-F946B (Z Fold5) | 16/36 | Debug `app.funput.funput.speechtest` |

Fold5 đã có Funput Play Store với signer khác. Bản **Funput Speech Test** có
package, dữ liệu và quyền riêng, không thay bản Play Store. Người thử phải bật
và chọn đúng bàn phím test; cấp quyền cho đúng app. Không gỡ app hoặc xoá dữ
liệu để vượt trở ngại cài đặt. Hai máy vật lý là hai device ID, dù cùng provider.

Build bản song song từ `platforms/android/`:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug \
  -Pfunput.speechTestApp=true --console=plain
```

APK vẫn ở `app/build/outputs/apk/debug/app-debug.apk`. Build debug thường và
bản song song dùng chung đường output: lưu APK cần dùng riêng và xác minh
application ID trước khi cài. Thuộc tính này chỉ áp dụng debug; release giữ ID
và feature gate ban đầu.

Mở editor tổng hợp chứa hai ô text và một ô password trên Fold5:

```bash
adb -s RFCW80JQRPK shell am start --activity-clear-top \
  -n app.funput.funput.speechtest/app.funput.funput.catalog.CatalogActivity \
  --ez speech_spike true
```

Trên S21 dùng `-s R5CRB2DKXSM` và component
`app.funput.funput/app.funput.funput.catalog.CatalogActivity`. Đây là IME thật;
flag `speech_panel` chỉ là preview UI, không dùng để nghiệm thu nhận dạng.
Mở host không tự thu âm. Người thử chủ động tap mic từng phiên.

## Corpus và phép đo

Dùng nguyên các câu VI trong [corpus 80 câu](../android-speech-spike/corpus.csv) và
[quy trình ghi CSV](../android-speech-spike/README.md#6-điền-csv). Không tạo corpus
khác để giảm độ khó. Mỗi máy chạy **60 câu VI**: 40 VI rõ, 10 VI tên/số và
10 VI nhiễu nhẹ. Giữ 20 câu EN trong corpus gốc để dùng về sau, nhưng không
yêu cầu chạy trong đợt này. Không ghi hàng đo EN giả hoặc tính mục bỏ qua là đạt.

Sao chép `measurements.template.csv` vào thư mục `android-speech-spike/runs/`
(đã gitignore). File ban đầu chỉ có header. Ghi **mọi attempt**, kể cả lỗi,
không điền reference thành hypothesis. Kết quả nhận dạng thủ công chỉ dùng
câu corpus tổng hợp; không đưa lời nói cá nhân vào Git. App không lưu audio
hoặc transcript; công cụ đo không truy cập mic, ADB hay mạng.

Sau khi model sẵn sàng, người thử bật airplane mode và tắt cả Wi-Fi/dữ liệu.
Đo tap mic → Listening và tap Dừng → final/chèn xong bằng đồng hồ quan sát;
ghi phương pháp/độ phân giải trong notes. Nếu chưa đo đáng tin cậy, để trống
timing và giữ pending. Final tự về trước Dừng không có số đo “sau Dừng”.

```bash
python3 docs/features/android-speech-spike/measure.py \
  --measurements docs/features/android-speech-spike/runs/p6-combined.csv \
  --require-two-device-gate
```

Ghép hai run bằng CSV reader/writer, giữ đúng một header. Retry có run ID mới;
không thay một hàng lỗi bằng một lần thành công. Báo riêng tên, số và lỗi dấu.
WER: NFC, lowercase và bỏ punctuation, giữ dấu tiếng Việt và biểu diễn số.
p95 dùng nearest rank. Không giảm ngưỡng sau khi thấy kết quả.

Mỗi máy phải có đủ 40 VI rõ offline: WER ≤20%, usable ≥90%, ready p95 và
final sau Dừng p95 đều ≤3.000 ms. Gate hai máy xét từng máy độc lập. Numeric
gate đạt vẫn phải hoàn tất [lifecycle/UI checklist](checklist.md) và các
nhóm corpus còn lại trước khi bật production.

## Tài liệu người dùng và bản phát hành

[Hướng dẫn, quyền riêng tư và release notes dự thảo](../../../platforms/android/docs/VoiceInput.md)
đã ghi rõ đây là tính năng debug đang nghiệm thu. Nội dung quyền riêng tư chỉ
mô tả trách nhiệm của Funput, không cam kết thay cho provider Android. Chưa
publish website hoặc phát hành app. Sau nghiệm thu, ghi kết quả vào report,
chuyển các mục pending có bằng chứng cụ thể và mới bật gate.
