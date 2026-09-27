# Làm lại app Android trên FunputUI

## Trạng thái

P0 → P4.2 đã xong, tất cả nằm trên nhánh tổng `feat/android-app-redesign`. Mọi màn của app
cài đặt (`:app`) giờ chạy thuần FunputUI. `:app` không còn phụ thuộc Material 3, và CI có
cổng chặn nó quay lại.

Còn lại trước khi mở **một** PR từ nhánh tổng vào `main`: rà soát trên máy và ghi chú phát
hành (xem "Còn lại"). Phiên bản phát hành đi riêng qua CI.

Bản theo dõi chi tiết từng PR nằm ở
[tài liệu Claude](https://claude.ai/code/artifact/f42fd634-f917-4141-805b-c6ccca7b4c7b).

## Mục tiêu

Người dùng phản hồi app Android xấu và vỡ giao diện. Mục tiêu là một giao diện **đẹp, hiện
đại, chuyên nghiệp** cho Funput trên Android. Cảm hứng lấy từ app iOS (thẻ nhóm, tiêu đề
lớn, thanh tab kính) nhưng **không sao chép 1:1**: vẫn giữ điều hướng, back và rung kiểu
Android.

**Trong phạm vi:** app cài đặt (`:app`): Cài đặt, Gõ tắt, Giao diện và bộ sưu tập chủ đề,
trình tạo chủ đề, ảnh nền, Giới thiệu, Giấy phép, luồng bật bàn phím.

**Ngoài phạm vi:** layout phím và bộ vẽ bàn phím (`:keyboard-renderer`, `:keyboard-ui`,
`:ime`), engine, dữ liệu cài đặt.

**Ưu tiên máy mới.** Máy cũ chỉ cần dùng được và không vỡ (xem "Hoãn").

## Quyết định đã chốt

| Quyết định | Chọn | Phương án bị loại và lý do |
| --- | --- | --- |
| Công nghệ UI | Compose + `compose-foundation`, design system riêng **FunputUI** (`:funput-ui`) | Flutter, React Native, Compose Multiplatform: quá nặng. Thư viện giả iOS: lạ với người dùng Android |
| Material 3 | Không dùng trong `:app`. `:funput-ui` chỉ mượn sheet, dialog, slider, vuốt, sau `MaterialBridge` | Giữ và đổi style: luôn phải chống lại kích thước, ripple, lớp phủ màu của Material |
| Kính | Thư viện backdrop của Kyant, 2 tầng (xem "Kính") | Haze: chỉ kính mờ. Tự viết AGSL: tốn công |
| Font | Be Vietnam Pro đóng gói trong APK (4 độ đậm, OFL) | SF Pro: giấy phép Apple. Font tải qua Google Play services: máy không GMS bị đổi font |
| Màu nhấn | Cam Funput | Xanh hệ thống: mất nhận diện thương hiệu |
| Icon | Phosphor (MIT), mỗi nhóm cài đặt một màu | SF Symbols: không được dùng trên Android |
| "Màu theo hình nền" | Bỏ | App có bảng màu riêng |
| Xem trước bàn phím ở Cài đặt | Giữ, thu nhỏ; chạm vào mở tab Giao diện | Bỏ hẳn cho giống iOS |
| Thanh tab | Viên thuốc kính mờ nổi, 56dp, cả 3 tab đều có tên | Thanh rộng 64dp của bản đầu: quá cao |
| Kiểm tra giao diện | Bằng tay trên máy thật | Ảnh golden trong CI: mỗi thay đổi giao diện tốn một vòng chụp lại trên Linux |

## Kiến trúc

```
funput-ui/tokens/app.tokens.json ──(buildSrc: kiểm tra + sinh code)──▶ FunputTokens.kt
                                                                          │
:funput-ui  (theme, glass, layout, cards, rows, controls, overlays, gestures, icons)
     │
:app  (chỉ dùng FunputUI; không import material3, có script chặn)
```

### Token

`platforms/android/funput-ui/tokens/app.tokens.json` là nguồn duy nhất cho màu, chữ, bo góc, khoảng cách, độ mờ
và chuyển động. Task `GenerateDesignTokensTask` (buildSrc) kiểm tra rồi sinh
`FunputTokens.kt` ở mỗi lần build. Các luật kiểm tra gồm:

- chữ chính ≥ 4.5:1 trên cả 4 bề mặt (nền nhóm, thẻ, và 2 bề mặt nổi của sheet)
- màu nhấn ≥ 3:1 trên cả 4 bề mặt
- `onAccent` ≥ 4.5:1 trên màu nhấn
- mọi màu nhóm (`tint*`) ≥ 3:1 trên thẻ
- quy tắc đặt tên và giới hạn giá trị

### Component chính (`:funput-ui`)

| Nhóm | Component |
| --- | --- |
| Màn | `FunputScreen` (tiêu đề lớn thu lại khi cuộn, thanh trên kính, `LazyColumn`), `FunputEditorScreen` (thanh trên cố định, phần đầu ghim, thanh hành động dưới), `FunputTabBar` |
| Thẻ | `FunputCard`, `FunputSection` (tiêu đề + footer), `FunputSectionHeader`, `FunputDivider` |
| Hàng | `FunputRow`, `LinkRow` (có `external` cho liên kết ra ngoài), `ActionRow`, `ChoiceRow`, `ToggleRow`, `SliderRow`, `SegmentedRow` |
| Điều khiển | `FunputToggle`, `FunputSegmented` (ô chọn nền cam đặc), `FunputButton` (primary/secondary/plain/destructive), `FunputIconButton`, `FunputTextField`, `FunputSearchField` |
| Lớp phủ | `FunputSheet`, `FunputDialog` |
| Cử chỉ | `SwipeToDelete` (có custom action cho TalkBack) |
| Icon | `FunputIcons` đặt tên theo ý nghĩa; nhập từ Phosphor bằng `scripts/import-phosphor-icons.py` theo `funput-ui/icons.txt` |

Bản debug có màn catalog (`CatalogActivity`) để xem mọi component.

### Kính

`funputGlass()` là chỗ duy nhất gọi thư viện backdrop.

- **`GlassTier.GLASS`** khi API ≥ 31 và máy không RAM thấp: làm mờ, khúc xạ ở mép (API 33+).
- **`GlassTier.SOLID`** ở các trường hợp còn lại: thẻ đặc có viền mảnh.
- Tuỳ chọn `frosted` dùng cho thanh tab: làm mờ mạnh hơn, lớp phủ dày hơn, để chữ phía sau
  không lẫn với nhãn.
- Shape phải là `RoundedRectangularShape`, vì `lens` ném lỗi với hình khác.

Số đo từ spike (Galaxy S21 Ultra, 120 Hz, `dumpsys gfxinfo`):

| Chế độ | Khung hình giật | Khung p50 / p90 / p99 (ms) | GPU p90 / p99 (ms) |
| --- | --- | --- | --- |
| Mờ đục | 1,83% | 6 / 8 / 11,7 | 4 / 5,7 |
| Haze blur | 1,72% | 10 / 12 / 16 | 6 / 9 |
| Backdrop Liquid Glass | 1,92% | 9 / 15 / 20,7 | 10,7 / 15,7 |

## Quy tắc rút ra khi làm

- **Kính không được nằm trong layer mà nó lấy mẫu.** Nếu đặt một bề mặt kính bên trong node
  có `glassSource`, RenderThread đệ quy tới tràn stack và app văng native. Test JVM không bắt
  được lỗi này. Vì vậy `FunputEditorScreen` dùng thanh đặc và không cung cấp backdrop.
- **Đọc trạng thái bất đồng bộ ở cấp màn hình, không đọc bên trong item của `LazyColumn`.**
  Trên máy CI, một item được dựng trước khi dữ liệu về đôi khi không được dựng lại. Cách làm
  đúng (xem `ShortcutsPage`, `LicensesRoute`) là màn hình đọc trạng thái rồi truyền giá trị
  xuống. Tập item cũng nên cố định: trạng thái đang tải và lỗi hiện ngay tại chỗ của danh sách.
- **Test phải chờ màn hình vẽ xong, không chỉ chờ dữ liệu.** Dùng `waitForText` trong
  `:ui-testing`: khi hết giờ, nó in ra toàn bộ cây giao diện.
- **Ảnh giải mã trên luồng nền:** muốn chụp sau khi ảnh đã hiện thì chờ
  `KeyboardSurfaceView.isBackgroundImageSettled` và test tag của ảnh đã giải mã.

## Kiểm tra chất lượng

- **Cổng trong CI** (job `android` của `ci.yml`):
  - `check-kotlin-loc.sh`: ≤150 dòng mỗi file Kotlin
  - `check-kotlin-layout.sh`: ≤5 file mỗi thư mục trong các thư mục đã đăng ký
  - `check-app-material-free.sh`
  - `-p buildSrc test`: luật token
  - `testDebugUnitTest`
- **Lint chưa chạy trong CI.** Chạy `lintDebug` ở máy trước khi mở PR (hiện `:funput-ui` 0
  cảnh báo, `:app` 3 cảnh báo ngoài phạm vi).
- **Test giao diện chạy trên JVM** (Robolectric, SDK 35, `GraphicsMode.NATIVE`, tiếng Việt):
  test hành vi cho từng màn, và test ảnh chụp dựng mọi màn ở sáng/tối × cỡ chữ 1.0/1.3.
  Test ảnh chụp **không so ảnh**, chỉ bắt màn bị văng. Muốn xem ảnh ở local thì chạy
  `recordRoborazziDebug`; ảnh ghi vào `build/outputs/roborazzi`.
- **Giao diện được kiểm tra bằng tay trên máy thật** (SM-G998B).

## Còn lại

- Rà soát trên máy trước khi phát hành:
  - TalkBack đi hết các màn
  - cỡ chữ 200%
  - chế độ sáng
  - độ mượt khi cuộn bộ sưu tập chủ đề (mỗi thẻ là một bàn phím thật đang vẽ)
- Ghi chú phát hành. Cần nêu việc bỏ "Màu theo hình nền" và thanh tab mới.
- Merge `main` vào nhánh tổng, chạy đủ CI, mở PR tổng vào `main`.
- Ảnh Play Store: chụp tay.

## Hoãn

Làm khi số người dùng đủ lớn:

- Baseline Profile + Macrobenchmark (khởi động, cuộn)
- Duyệt trên máy cũ (Android 8–10, RAM 2–3GB), làm đẹp tầng kính đặc
- Màn 320/360dp
- Tìm gốc rễ của lỗi item `LazyColumn` bỏ lỡ cập nhật trên Robolectric. Hiện đã tránh bằng
  cách đọc trạng thái ở cấp màn hình.
- Ngoài phạm vi đợt này, lint đang nhắc:
  - nâng `androidx.core:core-ktx`
  - icon launcher dạng bitmap thừa (minSdk 26 luôn dùng icon adaptive)
  - `data_extraction_rules.xml` còn chú thích mặc định của template, chưa chốt quy tắc sao lưu
