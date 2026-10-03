# P0 — kiểm chứng nhận dạng giọng nói Android

Thư mục này chứa corpus tự viết, quy trình thử và công cụ tổng hợp **dữ liệu đo
thủ công**. Nó ghi nhận phản hồi smoke test của người dùng, chưa có corpus run
đạt gate, bản ghi âm hoặc lịch sử lời nói. Thiết kế đầy đủ ở
[android-speech-to-text.md](../android-speech-to-text.md).

P0 dùng `SpeechRecognizer.createOnDeviceSpeechRecognizer`. P0–P3 từng có dải
**Speech spike (debug)** với Start/Stop/Cancel/Setup. Từ P5, điểm vào là mic cạnh
Emoji và speech panel trong IME; debug strip đã bỏ. Production vẫn tắt. Không có cloud
fallback, ML Kit, tải model tự động hoặc tự mở một phiên nghe tiếp theo.

Người dùng đã cho phép chuyển sang P1 ngày 03/10/2026 sau smoke test VI.
P2 đã bổ sung màn chuẩn bị dùng chung với Settings; xem phần 19 của docs chính.
P3 dùng controller/reducer/editor guard mới trong cùng dải debug; xem phần 20.
Giới hạn 60 giây gửi Stop và chờ final tối đa 5 giây, terminal xoá preview.
Regression P3 đạt IME 53/53 và keyboard-ui 14/14 trên Samsung; đây không thay
corpus run hoặc kiểm chứng ASR offline/máy thứ hai.
Các gate còn thiếu được giữ để nghiệm thu P6; production vẫn tắt. Không coi
quyết định chuyển phase là bằng chứng các phép đo đã pass.

## 1. Trạng thái bằng chứng

| Hạng mục | Bằng chứng hiện có | Kết luận |
| --- | --- | --- |
| Máy Samsung | SM-G998B, Android 15/API 35, serial kiểm tra `R5CRB2DKXSM` | Metadata ADB đã đọc |
| Service on-device được cấu hình | `com.google.android.as/com.google.android.apps.miphone.aiai.app.AiAiSpeechRecognitionService` | Cấu hình hệ thống, chưa phải kết quả API live |
| Android System Intelligence | `V.41.playstore.oemfull.843410720`, code `12872254` | Metadata package đã đọc |
| `isOnDeviceRecognitionAvailable` từ process kiểm thử | Probe máy thật ngày 03/10/2026 lúc 00:47, Asia/Ho_Chi_Minh: `true` | **PASS — chỉ capability** |
| Installed/pending/supported cho VI và EN | VI installed có `vi-VN`; EN supported/downloadable có `en-US` nhưng chưa installed; pending rỗng | **PASS — đã đọc support, chưa thử ASR** |
| Preparation facade và setup P2 | Máy thật trả VI Ready/EN Downloadable; route Settings và CTA Chuẩn bị từ IME dùng chung UI, không tự thu/tải | **PASS — kiểm tra 03/10/2026; chưa thử tải model** |
| Thanh spike trong IME thật | Debug APK đã cập nhật; 00:54–00:55 thấy controls cùng Funput trong editor tổng hợp | **PASS — chỉ UI** |
| Thiếu quyền và mở setup | Start bị chặn; CTA mở setup, permission vẫn denied; AppOps `RECORD_AUDIO: ignore` | **PASS — chưa thử grant/capture** |
| Nhận dạng VI thực tế từ IME visible | Người dùng báo hoạt động chuẩn, tiếng Việt nhận diện tốt trên bản debug đang thử | **PASS — smoke test theo phản hồi người dùng; chưa có timing/corpus run** |
| Nhận dạng EN thực tế từ IME visible | Chưa có phản hồi kiểm chứng | **PENDING** |
| Nhận dạng khi không có mạng | Người dùng xác nhận lượt VI-C01 offline và Huỷ trên Fold5 OK ngày 03/10/2026 | **PASS — smoke theo phản hồi; corpus/timing PENDING** |
| Máy thứ hai | Fold5 SM-F946B, Android 16/API 36; probe VI installed, regression 62 IME/29 UI pass; người dùng báo lượt VI offline/Huỷ OK | **PASS — capability/regression/smoke; corpus PENDING** |
| Gate chất lượng và lifecycle | Fold5 hide/lock/chuyển TEXT A → B đạt theo người dùng; các ca khác và corpus/timing chưa đủ | **PENDING — chưa đạt toàn bộ gate** |

Probe trên đã đóng cả temporary client. Unit test, lint, debug build và release
compile đã pass trong lượt kiểm chứng P0. Tại snapshot P0, toàn bộ connected suite chưa sạch:
IME có 51 test/4 fail và keyboard-ui có 14 test/8 fail ở fixtures/native tests;
xem ghi chú kiểm chứng trong tài liệu thiết kế chính. Không coi connected suite
hoặc gate sản phẩm đã pass từ snapshot P0, và không quy lỗi fixtures thành kết quả ASR.
P1 đã cập nhật fixtures: snapshot P1 đạt IME 50/50 và keyboard-ui 14/14,
benchmark chuyển riêng sang release. P2 thêm facade test máy thật: IME 51/51;
keyboard-ui lượt đầu 13/14 do clipboard swipe, chạy lại độc lập 14/14. Xem
phần 18–19 trong tài liệu thiết kế chính để giữ cả lịch sử lỗi và giới hạn.
Sau lượt kiểm tra UI do agent thực hiện đã khôi phục Samsung làm default IME;
lượt đó không cấp quyền micro, thu âm hoặc tải model. Sau đó người dùng tự thử
và xác nhận VI hoạt động. Phản hồi này không chứa số đo, transcript hoặc xác
nhận mạng tắt; không tự điền CSV hay coi gate hai máy đã đạt.

Service mặc định cho nhận dạng thường trên Samsung hiện là Google TTS; không
dùng giá trị này để kết luận provider của API on-device. Có package/service hoặc
API availability không chứng minh tiếng Việt đã có model, chạy offline hay đạt
chất lượng. Probe live xác nhận API trả availability/support như trên; smoke
test VI được người dùng xác nhận riêng. Khi có corpus run hoặc report mới, cập nhật bảng và dẫn đến
report cụ thể; không biến support API thành kết quả nhận dạng đã pass.

## 2. Artifact và dữ liệu

P6 dùng lại corpus/tool trong thư mục này. Xem [quy trình P6](../android-speech-acceptance/README.md),
[report hai máy](../android-speech-acceptance/report-2026-10-03.md) và
[checklist máy thật](../android-speech-acceptance/checklist.md). Snapshot P0/P1
trên đây giữ lịch sử; report P6 ghi kết quả EN mới nhất, không suy ra từ VI support.

- `corpus.csv`: đúng **80 câu** — 40 VI rõ, 10 VI tên/số, 10 VI nhiễu nhẹ,
  20 EN rõ. Mỗi câu do tác giả artifact tự viết; tên/địa chỉ trong đó dùng để thử.
- `measurements.template.csv`: chỉ header; sao chép thành file run riêng rồi
  nhập kết quả. Không có hàng đo giả hoặc placeholder được coi là một phép đo.
- `measure.py`: Python 3, chỉ thư viện chuẩn; đọc CSV và xuất JSON ra stdout.
  Không gọi ADB, microphone, service, mạng hoặc ghi audio/log transcript tự động.
- `test_measure.py`: regression của numeric gate dùng fixtures tổng hợp trong
  RAM/temp file, không phải dữ liệu máy thật. Chạy:
  `python3 -m unittest discover -s docs/features/android-speech-spike -p 'test_*.py'`.

Giữ corpus cố định trong một đợt so sánh. Đọc câu tự nhiên; chữ số trong reference
được đọc thành lời như thường dùng. Không yêu cầu đọc dấu chấm/dấu phẩy thành
lệnh. Ghi mọi attempt, kể cả lỗi bắt đầu và không có final; không chỉ giữ câu tốt.
Thử lại cả run hoặc tạo `run_id` mới để không thay kết quả lỗi bằng kết quả tốt.

## 3. Chuẩn bị máy và probe không thu âm

1. Ghi model thiết bị, API, provider/package và phiên bản service; dùng `unknown`
   nếu không có dữ liệu, không tự tạo model version. Dùng một `device_id` ẩn danh
   ổn định cho mỗi máy vật lý. Không đưa tài khoản/thông tin riêng vào CSV.
2. Chuẩn bị debug APK an toàn với bản đã cài. Không gỡ Funput hoặc xoá dữ liệu để
   chạy spike. APK đang cài trên Samsung là debug/test-only; cần đối chiếu signer
   của APK mới trước khi update. Cài test APK và đổi bàn phím là thao tác chuẩn bị
   có chủ đích, không phải side effect của script đo.
3. Khi cần probe API, chạy class instrumented riêng trên thiết bị đã chọn, từ
   `platforms/android/`:

   ```bash
   ./gradlew :ime:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.funput.funput.ime.speech.preparation.OnDeviceSpeechProbeInstrumentedTest --console=plain
   ```

   Gradle triển khai APK kiểm thử. Probe chỉ gọi availability/check support trên
   main thread; mỗi locale chờ tối đa 10 giây, rồi đóng temporary recognizer kể
   cả timeout. Nó in installed/pending/supported và error code, **không nghe hoặc
   tải model**. Test không assert VI có model; test chạy xong không có nghĩa ASR
   đã pass. Lưu report metadata riêng, không gắn transcript vào logcat.
4. Nếu model chưa có, mở **Chuẩn bị** hoặc Settings → Nhập bằng giọng nói.
   Bản debug P2 hiển thị readiness VI/EN và nút tải khi Downloadable; chỉ tải sau
   thao tác chủ động của người thử. API 33 gửi yêu cầu rồi cần Kiểm tra lại;
   API 34 có progress/scheduled/success/error tuỳ service. Ghi thao tác và mạng
   đã dùng. Không chuyển locale hoặc dùng recognizer online để đạt kết quả.

API 31–32 có thể thử on-device nhưng locale readiness vẫn Unknown vì không có
`checkRecognitionSupport`. API <31 không có đường thu của spike này.

## 4. Quyền và một phiên trong IME thật

1. Mở host test của chính Funput trong debug build bằng:

   ```bash
   adb shell am start -n app.funput.funput/.catalog.CatalogActivity --ez speech_spike true
   ```

   Catalog chuyển tới `SpeechSpikeEditorActivity` không exported: hai ô TEXT và
   một ô password để thử chuyển editor/policy. Nội dung chỉ ở RAM, các field tắt
   state saving. Người thử chủ động chọn Funput và đặt caret thu gọn ở ô TEXT A/B;
   giữ bàn phím visible. Password chỉ để kiểm chứng từ chối thu, không chạy corpus.
   Không dùng password/PIN/email, number/phone hoặc host KEY_EVENT cho accuracy.
2. Chọn VI/EN trên bàn phím; locale theo trạng thái đó. Bản P5 có mic cạnh
   Emoji, Preparing/Listening/Finalizing/Error cùng Dừng/Huỷ/Chuẩn bị khi phù hợp.
3. Thiếu quyền thì tap **Chuẩn bị** (Setup). Activity `SpeechSetupActivity` dùng
   chung nội dung quyền/model với Settings; chỉ xin quyền theo tap và grant không thu âm. Quay lại ô nhập và **tap mic lần
   nữa**. Không dùng `adb grant`, không ghi một phiên chưa bắt đầu là đã thử ASR.
4. Tap mic; đo thời gian từ tap đến trạng thái Listening/ready. Chỉ nói khi đã
   ready. Đọc một câu corpus, rồi tap **Dừng** khi hết câu; đo đến final/chèn xong.
   Partial chỉ để nhìn preview, tuyệt đối không dùng làm hypothesis trong CSV.
5. Chép final của ô test vào `hypothesis` bằng thao tác thủ công. Đánh giá usable
   và lỗi dấu/tên/số. Xoá ô test bằng thao tác người thử trước câu tiếp theo.
6. Tap Huỷ khi muốn bỏ phiên; huỷ không chèn partial. Có thể huỷ trong lúc chờ
   final. Không mở phiên nghe nối tiếp tự động hoặc giả lập lời nói bằng test text.

Timing dùng milliseconds. Chỉ ghi giá trị đã đo từ đồng hồ/mốc quan sát trực tiếp;
ghi độ phân giải và phương pháp trong `notes`. Nếu không đo đủ tin cậy để xét
ngưỡng, để trống timing và giữ gate Pending, không suy ra từ cảm giác nhanh/chậm.
`final_after_stop_ms` trong benchmark dùng **Stop thực sự**; khi dịch vụ tự endpoint
trước Stop, ghi notes và đo lại trong run phù hợp, không gộp mốc endpoint vào số
đo “sau Stop”. Không lưu video/audio để công cụ này phân tích tự động.

## 5. Ma trận và điều kiện nói

Chạy cả 80 câu trên từng tổ hợp được hỗ trợ. Nhóm `vi_clear` và `en_clear` ở phòng
yên tĩnh, khoảng cách mic và giọng người đọc ổn định. Nhóm `vi_names_numbers` cũng
yên tĩnh. Nhóm `vi_noise` có tiếng nền nhẹ thật; ghi loại/khoảng cách tiếng nền
trong notes, không tự coi câu reference là một file audio hoặc bơm audio vào API.

Sau khi chuẩn bị model, người thử bật airplane mode và tắt cả Wi-Fi/dữ liệu di
động còn có thể bật riêng. Kiểm chứng các phiên VI/EN thực tế ở trạng thái này;
ghi `airplane_mode=yes` chỉ khi đã xác nhận mạng tắt. Thử có mạng là một run khác.
Không có thao tác tự thay đổi mạng hoặc permission trong `measure.py`.

Ngoài corpus, kiểm tra tay: Cancel, Stop chờ final, đổi caret/editor, hide/show,
screen lock, rotate, nhập phím cứng, privacy toggle, quyền bị thu hồi và app khác
dùng mic. Kiểm tra gõ thường/emoji/clipboard/gõ tắt sau khi huỷ. Các ca race/lifecycle
này có bảng riêng, không chèn vào corpus như câu accuracy. Giữ kết quả thất bại.

## 6. Điền CSV

Mỗi hàng là một attempt của một câu trên một máy trong một run. UTF-8; dùng công
cụ CSV hoặc đặt chuỗi có dấu phẩy/xuống dòng trong dấu nháy kép đúng chuẩn.

| Cột | Quy tắc |
| --- | --- |
| `run_id`, `device_id` | Bắt buộc; retry dùng run mới. Hai run của cùng máy không phải hai máy |
| `device_model`, `android_api`, `service_package`, `service_version` | Metadata; phiên bản không biết ghi `unknown`; không đổi metadata giữa cùng run |
| `locale`, `corpus_id` | `vi-VN`/`en-US` và ID khớp corpus |
| `model_state` | `installed`, `unknown`, `pending`, `downloadable`, `unsupported` theo bằng chứng thực tế |
| `airplane_mode` | `yes`, `no`, `unknown`; yes bao gồm xác nhận Wi-Fi/data đã tắt |
| `status` | `final`, `start_error`, `recognition_error`, `no_match`, `timeout`, `cancelled` |
| `ready_ms` | Tap mic → ready; trống nếu chưa ready hoặc chưa đo được |
| `final_after_stop_ms` | Tap Stop → final/chèn; chỉ có cho `final`, trống nếu chưa đo được |
| `hypothesis` | Final thật, trống cho mọi trạng thái khác; không lấy partial |
| `usable` | Người thử đánh dấu `yes`/`no`; cancelled có thể trống |
| `diacritic_error`, `names_error`, `numbers_error` | Đánh giá tay `yes`/`no`/`not_applicable`; trống = chưa kiểm tra |
| `notes` | Error code, môi trường, cách đo/timing resolution, formatting và vấn đề editor; không thông tin riêng |

`start_error` là không tới ready; lỗi/timeout sau ready dùng trạng thái tương ứng.
Nếu kết quả rỗng dùng `no_match`, không ghi `final` rỗng. Usable nghĩa câu dùng được
mà không phải gõ lại phần lớn; đây là đánh giá của người thử, không suy ra từ WER.
Lỗi tên/số mang ý nghĩa sai cần được đánh dấu riêng dù số token sai rất ít. Khác
cách viết số như “mười lăm” và “15” vẫn khác trong WER; nếu số lượng đúng nghĩa thì
người thử có thể đánh dấu `numbers_error=no` và giải thích formatting ở notes.

## 7. Tính điểm, latency và gate

```bash
python3 docs/features/android-speech-spike/measure.py --measurements /duong-dan/run.csv
python3 docs/features/android-speech-spike/measure.py --measurements /duong-dan/run.csv --require-two-device-gate
```

Công cụ chuẩn hoá **NFC**, chuyển lowercase, đổi mọi ký tự Unicode thuộc nhóm
punctuation (`P*`) thành khoảng trắng và split whitespace. Giữ dấu tiếng Việt,
chữ số, thứ tự token; không bỏ dấu, đổi số thành lời, gộp từ ghép hoặc đổi tên.
WER = tổng edit distance token / tổng reference token; tiếng Việt là token/âm
tiết cách trắng. WER có thể >100% khi nhiều insertions. Không dùng trung bình
WER từng câu để che chênh lệch độ dài.

Mọi attempt không cancelled tính vào WER và usable; lỗi không có final dùng
hypothesis rỗng, tương đương deletion toàn câu. Cancelled được báo riêng, không
giúp hoàn thành 40 câu clear. Timing chỉ tính các mẫu có giá trị thật; report
hiển thị số mẫu. p50/p95 dùng **nearest-rank**: sort tăng dần, chọn vị trí
`ceil(p × n)`, không nội suy. Tỉ lệ start lỗi được báo riêng, không thêm một
ngưỡng chưa được thiết kế. Nhóm tên/số và các manual marks được báo riêng,
không gộp vào nhóm 40 VI rõ để làm gate dễ đạt hơn.

Các ngưỡng giữ nguyên thiết kế, phải chốt trước khi đo, không hạ sau khi thấy số:

| Gate trên từng tổ hợp máy/service được hỗ trợ | Ngưỡng |
| --- | --- |
| Đủ corpus VI rõ | 40/40 câu khác nhau, không chọn riêng câu tốt |
| WER nhóm 40 VI rõ | ≤20% |
| Câu usable nhóm 40 VI rõ | ≥90% |
| Ready p95 | ≤3000 ms |
| Final p95 sau Stop | ≤3000 ms |
| Hai máy | **Hai máy vật lý khác nhau**, mỗi máy đạt các ngưỡng riêng |

**Hard gate hai thiết bị:** một Samsung không đủ. Không pool kết quả hai máy,
không coi hai phiên/service của cùng máy là hai tổ hợp được xác minh. Mỗi máy
phải có bằng chứng nhận dạng offline từ IME thật, gate numeric và lifecycle.
Chưa có máy thứ hai hoặc thiếu timing/bằng chứng thì kết luận **PENDING**.

Report `two_device_numerical_gate=PASS` chỉ xét số và metadata manual; không tự
xác nhận offline/network, transcript có thật hay code đã an toàn. Release gate
luôn `NOT_EVALUATED` trong script. Bắt buộc thêm: 0 commit sai phiên/duplicate,
Cancel không chèn, hide/lock dừng thu, không cloud fallback, gõ thường không tạo
speech client trên mỗi phím. Không có ngưỡng phát hành accuracy riêng cho EN,
nhiễu hoặc tên/số trong thiết kế này; vẫn phải đo và ghi giới hạn trung thực.

Template chưa điền xuất `PENDING`, không xuất số đo mặc định. Flag
`--require-two-device-gate` trả exit 1 khi chưa có hai máy đạt numeric gate;
CSV sai trả exit 2. Script không phán đoán tiêu chí release thay người kiểm thử.

## 8. Mẫu biên bản bằng chứng cần hoàn tất

Không điền Pass chỉ vì build/unit test hoặc service probe đã chạy.

| Hạng mục | Máy A: Samsung SM-G998B | Máy B: chưa chọn | Report/bằng chứng |
| --- | --- | --- | --- |
| Debug APK/signing/build revision | Debug APK update cùng chữ ký, code 27/version 1.2026.70 | PENDING | Source P5: `feat/android-speech-to-text`, implementation `f887424c`; xem docs chính |
| Live availability + VI/EN support lists | PASS capability; EN chưa installed | PENDING | Probe 03/10/2026 00:47 |
| VI smoke test từ IME thật | PASS theo phản hồi người dùng; đánh giá nhận diện tốt | PENDING | Phản hồi trong trao đổi; không có corpus/timing/network status |
| Model preparation đã làm | PENDING | PENDING | |
| IME visible, quyền cấp/từ chối/thu hồi | UI visible/Start thiếu quyền/CTA setup PASS; grant/revoke PENDING | PENDING | Kiểm tra tay 03/10/2026 00:54–00:55 |
| 80 câu + CSV đúng metadata | PENDING | PENDING | |
| Airplane mode, Wi-Fi/data tắt, ASR VI/EN thật | PENDING | PENDING | |
| WER/usable/ready/final của 40 VI rõ | PENDING | PENDING | |
| Tên/số/dấu và nhiễu/EN report riêng | PENDING | PENDING | |
| Cancel/late callback/hide/editor/selection | PENDING | PENDING | |
| Screen lock/rotate/hardware/permission revoke | PENDING | PENDING | |
| Gõ thường/emoji/clipboard/shortcuts | PENDING | PENDING | |
| 0 sai phiên/duplicate; mọi client cleanup | PENDING | PENDING | |

Biên bản cần ghi người thử, điều kiện phòng/mic/giọng đọc, build revision, service
version, ngày chạy, cách đo và giới hạn bằng chứng. Không lưu audio. Khi đủ hai
máy, ghi rõ verdict P0 và các giới hạn còn lại trước khi nghiệm thu P6;
không mô tả nút mic production đã hoàn thành chỉ từ spike này.

## 9. Catalog presentation P4 (03/10/2026)

P4 thêm catalog state giả để review mic/panel. Từ P5, IME thật đã dùng mic/panel
với on-device backend; catalog vẫn là presentation giả độc lập. Mở catalog từ máy đã cài debug APK:

```bash
adb shell am start -n app.funput.funput/.catalog.CatalogActivity --ez speech_panel true
```

Catalog có Preparing/Listening/Finalizing/Error, VI/EN, tất cả preset theme, chế độ một tay
và font 200% chỉ trong catalog. Back/Huỷ về Letters; bấm mic mở lại panel mẫu.
Dừng đổi sang Finalizing giả; không có final commit vào app/editor.

Catalog không tạo recognizer, xin quyền, tải model hoặc mở thiết lập thật.
Preview là câu mẫu viết sẵn, không ghi audio/transcript. Nó không thay spike
nhận dạng thật, corpus/WER hay gate offline/P6. Unit/lint/build pass; Samsung API
35 có 22/22 UI tests (8 mới P4) và regression IME 53/53. Xem phần 21 của tài liệu
chính để biết contracts, geometry, accessibility và phần P5/P6 còn thiếu.


## 10. Thử flow P5 đã cài trên Samsung

Implementation `f887424c` đã cài debug APK cùng signer/version và giữ dữ liệu.
Mở editor qua lệnh `--ez speech_spike true` ở phần 3, hoặc một ô văn bản thông
thường. Chọn Funput/VI và tap mic cạnh Emoji để bắt đầu. Partial chỉ ở panel;
Dừng chờ final, final tự chèn một lần rồi về Letters; Huỷ/Back bỏ phiên. Đổi ô
nhập, hide/finish/rotation/lock hoặc tắt setting giọng nói huỷ phiên. Sau chuẩn bị
quyền/model, quay lại và tap mic lần nữa, không tự start.

P5 có IME 530 unit tests và 62/62 instrumentation (9 mới), UI 22/22 trên Samsung;
LOC/layout/FunputUI, lint/build/release compile pass. Integration dùng backend giả
và editor/JNI/view thật, không thu âm hoặc cấp/revoke quyền thật. AppOps không có
recording mới trong lượt automation. **Smoke test lời nói thật của mic/panel P5
vẫn PENDING**; phản hồi VI tốt trước đó thuộc flow debug strip P0–P3.

Ghi kết quả mới vào biên bản theo revision, tránh coi synthetic final là câu ASR
hoặc tự điền CSV. Corpus/offline/timing/hai máy cùng ma trận mic/editor/P6 vẫn chưa
đủ bằng chứng; production giữ tắt. Chi tiết architecture, safety lease và validation
ở phần 22 của tài liệu thiết kế chính.


## 11. Review thiết kế panel theo theme

Sau phản hồi P5, UI `2f7bf8ca` dùng header/badge, mic illustration, transcript card
và footer bo góc; theme tokens hiện có quyết định màu, nền và chữ. Preview catalog
`eb5f4655` chọn tất cả presets, không thay setting theme thật của người dùng.

Ví dụ mở Listening giả với Glass Dark (index 3 trong catalog hiện tại):

```bash
adb shell am start --activity-clear-top -n app.funput.funput/.catalog.CatalogActivity \
  --ez speech_panel true --ei speech_stage 1 --ei speech_theme 3 --ez speech_show true
```

Thêm `--ez speech_empty true` để xem mic/hint trước partial. `speech_snapshot=true`
chỉ xuất ảnh UI mẫu từ catalog vào `cache/speech-panel-preview.png`; không nối tới
editor hoặc recognizer và không có API tương đương trong IME thật. Review ảnh trên
Samsung xác nhận Glass Dark/Glass Light; UI 25/25, IME 62/62 và contrast/unit/lint/
build/LOC pass. Đây là bằng chứng presentation, không thay nghiệm thu ASR/P6.


## 12. Review voice orb (03/10/2026)

Catalog theme/state ở phần 11 nay dùng thiết kế orb Canvas trung tâm lấy màu
accent của theme; transcript không có card và footer là hai nút pill. Có thể dùng
cùng extras `speech_theme`, `speech_stage`, `speech_show`, `speech_empty` và
`speech_snapshot` để review Glass Dark, Glass Light hoặc Orchid. Orb animate
activity của phiên giả, không mô phỏng âm lượng và không tạo recognizer.

Orb nhỏ khi có transcript, co theo host height khi chưa có chữ; có thể dùng
font 200%/one-hand/floating để review vùng cuộn và footer. System animation scale
0 cho orb tĩnh. Hide parent/window hoặc detach dừng chuyển động. Quyền/audio,
model và state machine P5 giữ nguyên. Xem phần 24 của tài liệu thiết kế để biết
nguồn tham khảo và cơ chế motion; nghiệm thu ASR P6 vẫn chưa được thay thế.
