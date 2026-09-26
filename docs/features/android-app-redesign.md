# Làm lại app Android theo phong cách iOS (Liquid Glass)

## Trạng thái

Đang làm P0. Code nằm trên nhánh tổng `feat/android-app-redesign`: mỗi PR con merge
vào nhánh này, và chỉ khi đạt chất lượng mới mở **một** PR từ nhánh tổng vào `main`.

Bản theo dõi sống (trạng thái từng màn, checklist) nằm ở
[tài liệu Claude](https://claude.ai/code/artifact/f42fd634-f917-4141-805b-c6ccca7b4c7b);
file này được cập nhật trong cùng PR mỗi khi xong một giai đoạn.

## Tổng quan & mục tiêu

Làm lại toàn bộ giao diện app Funput trên Android cho giống app iOS gần 1:1, có hiệu
ứng Liquid Glass ở nơi máy hỗ trợ. Lý do: người dùng phản hồi app đang xấu, vỡ giao
diện, nhất là trên máy cũ.

**Trong phạm vi:** app cài đặt (module `:app`), gồm trang cài đặt, giao diện/chủ đề,
trình tạo chủ đề, gõ tắt, giới thiệu, luồng bật bàn phím.

**Ngoài phạm vi:** layout phím và bộ vẽ bàn phím (`:keyboard-renderer`, `:keyboard-ui`,
`:ime`) giữ nguyên. Engine và dữ liệu cài đặt không đổi.

**Ưu tiên máy mới.** Giai đoạn này nhắm máy Android đời mới trước. Đo hiệu năng và
duyệt kỹ trên máy cũ được hoãn đến khi số người dùng đủ lớn (xem mục "Hoãn").

**Tiêu chí thành công:**

- Không vỡ layout trên máy hiện đại, cỡ chữ 100% và 130%, sáng và tối, có ảnh chụp tự
  động làm bằng chứng
- Máy dưới Android 13 (không có kính) vẫn dùng được và nhất quán, không vỡ
- Đặt ảnh chụp cạnh app iOS, người dùng nhận ra cùng một sản phẩm
- TalkBack đọc đúng mọi control, vùng chạm tối thiểu 48dp

## Quyết định đã chốt

Giữ Kotlin + Jetpack Compose, bỏ Material 3, tự dựng design system riêng theo ngôn ngữ
thiết kế của app iOS.

| Quyết định | Chọn | Phương án bị loại và lý do |
| --- | --- | --- |
| Ngôn ngữ thiết kế | Giống iOS gần 1:1 (cùng bố cục, cùng tab) | Material You: máy cũ không có màu theo hình nền nên hiển thị một bản khác hẳn |
| Công nghệ UI | Compose + `compose-foundation`, design system tự dựng "FunputUI" | Flutter, React Native, Compose Multiplatform: quá nặng, phá thế mạnh Kotlin native của IME |
| Thư viện giả iOS (compose-cupertino…) | Không dùng | Người dùng Android thấy lạ, thư viện ít người bảo trì |
| Material 3 | Bỏ làm nền, chỉ mượn vài component khó nếu cần | Giữ và đổi style: luôn phải chống lại kích thước, ripple, lớp phủ màu của Material |
| Liquid Glass | Có, theo tầng máy; máy không hỗ trợ dùng bề mặt mờ đục đơn giản | Kính làm nền chính: máy Android 8–12 không vẽ được |
| Font | Be Vietnam Pro đóng gói trong APK | SF Pro: giấy phép Apple cấm dùng trên Android. Tải qua Google Play services: máy không có GMS bị đổi font |
| Màu nhấn | Cam Funput, dùng chung cho iOS và Android | Xanh hệ thống iOS (`AccentColor` hiện để trống): mất nhận diện thương hiệu. iOS sẽ đổi sang cam trong một PR riêng |
| Máy mục tiêu | Ưu tiên máy mới | Tối ưu máy cũ ngay: tốn công khi số người dùng còn nhỏ |
| Cài đặt "Màu theo hình nền" | Bỏ | Không còn Material You để áp dụng |
| Cách triển khai | Thay dần từng màn, sau khi design system xong | Đập đi xây lại một lần: rủi ro cao, khó review |

## Chẩn đoán hiện trạng

Nền kỹ thuật không cũ (Compose BOM 2026.09, predictive back, shared transition). Lỗi
nằm ở thiết kế và độ bền trên máy yếu, màn nhỏ. Đây là giả thuyết đọc từ code, chưa
đối chiếu ảnh chụp thật. Đường dẫn tính từ
`platforms/android/app/src/main/java/app/funput/funput/`.

| # | Nguyên nhân nghi ngờ | Chỗ trong code | Bản làm lại xử lý bằng |
| --- | --- | --- | --- |
| 1 | Màu theo hình nền chỉ có từ Android 12, máy cũ thấy bộ màu khác | `ui/theme/DynamicColors.kt` | Một bộ màu riêng cho mọi máy |
| 2 | Các lớp nền chênh rất ít (T98 so với T94), màn LCD rẻ không hiện được | `ui/theme/FunputLightColors.kt` | Token màu kiểu iOS: nền xám, nhóm trắng, tương phản rõ |
| 3 | Font tải qua Google Play services, đổi font và nhảy layout khi tải xong | `ui/theme/FunputFont.kt` | Đóng gói font |
| 4 | Một trang cài đặt dài 8 nhóm + hình bàn phím lớn + thanh tiêu đề lớn | `ui/settings/SettingsScreenSections.kt` | Thẻ nhóm như iOS |
| 5 | Hàng cài đặt không giới hạn độ rộng giá trị, tiêu đề tiếng Việt bị ép | `ui/settings/components/SettingsRow.kt` | Component hàng mới chịu được cỡ chữ 200% |
| 6 | Nhiều hiệu ứng, không có Baseline Profile, giật trên máy yếu | `SettingsRowShapes.kt`, `StaggeredEntry.kt`, `app/build.gradle.kts` | Baseline Profile + hiệu ứng theo tầng máy |

Việc thu ảnh chụp và thông tin máy từ người phản hồi để xác nhận giả thuyết đã được
hoãn (xem mục "Hoãn").

## Kiến trúc FunputUI

FunputUI là một module Compose mới (`:funput-ui`) chỉ phụ thuộc `compose-foundation`,
sao chép ngôn ngữ thiết kế đang có trong app iOS. Mọi màn của `:app` chỉ được dùng
component từ đây.

### Ngôn ngữ thiết kế lấy từ iOS

Đọc từ `platforms/ios/Funput/Components/AppScreen.swift`, `ContentCard.swift`,
`Settings/SettingsSectionCard.swift`:

- Nền màn hình: gradient chéo từ nền nhóm (grouped background) qua màu nhấn 8% rồi về
  nền nhóm
- Thẻ nội dung: bo 22pt góc mượt (continuous/squircle), viền mảnh, **không phải kính**
- Kính chỉ dành cho điều hướng (thanh tab) và nút hành động; thẻ tương tác bằng kính
  bo 20pt
- Khoảng cách: 18pt giữa các thẻ, lề ngang 18pt, nội dung rộng tối đa 720pt
- Tiêu đề lớn thu lại khi cuộn (large title)

### Token

| Nhóm | Nội dung | Ghi chú |
| --- | --- | --- |
| Màu | Nền nhóm, thẻ, chữ chính/phụ/mờ, đường kẻ, màu nhấn, màu phá huỷ, sáng + tối | Giá trị bám theo màu hệ thống iOS, có kiểm tra tương phản |
| Chữ | Large title, title, headline, body, footnote, caption | Be Vietnam Pro, giữ cách căn dòng cho dấu chồng tiếng Việt |
| Hình khối | Bo 22 / 20 / 12, góc mượt kiểu squircle | Compose không có sẵn góc continuous, phải tự viết Shape |
| Khoảng cách | 4 / 8 / 12 / 18 / 24 | Thay bộ `Spacing` hiện tại |
| Hiệu ứng | Lò xo nhẹ, thời gian chuẩn, 3 tầng theo máy | Xem phần Liquid Glass |

**Token dùng chung iOS và Android:** một file JSON trong repo, script sinh ra Kotlin và
Swift. Hai app không thể lệch màu hoặc khoảng cách âm thầm.

### Component

| Component | Tương đương iOS | Ghi chú |
| --- | --- | --- |
| `FunputScreen` | `AppScreen` | Nền gradient, cuộn, lề, giới hạn 720dp, xử lý insets |
| `LargeTitleBar` | Navigation large title | Thu lại khi cuộn, nền kính khi có nội dung phía sau |
| `GlassTabBar` | `TabView` iOS 26 | Thanh tab nổi dạng viên thuốc |
| `ContentCard`, `SectionCard` | `ContentCard`, `SettingsSectionCard` | Tiêu đề nhóm + footer |
| `GlassCard` | `InteractiveGlassCard` | Thẻ bấm được |
| Hàng: link, giá trị, công tắc, thanh trượt, hành động | `SettingsRows` | Tiêu đề xuống dòng được, giá trị xuống dưới khi hẹp, nền sáng lên khi nhấn |
| `Toggle` | iOS switch | Tự vẽ, 51×31, có semantics Switch |
| `SegmentedControl` | Picker segmented | |
| `GlassButton`, `GlassProminentButton` | `.glass`, `.glassProminent` | |
| `SelectionSheet` | `SettingsSelectionSheet` | Sheet kéo được, có thể mượn `ModalBottomSheet` đổi style |
| `Alert` | `.alert` | Dựa trên `Dialog` của compose-ui |
| `KeyboardPreview` | `KeyboardPreview` | Giữ bộ vẽ hiện có, thêm nền đỡ cho chủ đề kính |

**Trợ năng:** mỗi component tự khai báo `Role`, trạng thái, vùng chạm 48dp. Có test
semantics riêng cho từng component.

**Giữ kiểu Android:** predictive back, nút back hệ thống, tràn viền, thanh điều hướng 3
nút, rung theo chuẩn Android.

## Liquid Glass trên Android

Liquid Glass làm được trên Android 13+, bản rút gọn trên Android 12, và Android 8–11
dùng bề mặt mờ đục đơn giản. Tầng 3 (máy mới) được thiết kế và duyệt trước.

| Tầng | Máy | Hiệu ứng | Công nghệ |
| --- | --- | --- | --- |
| 3 — Liquid Glass | Android 13+ (API 33) | Làm mờ nền phía sau + khúc xạ ở mép + vệt sáng | Thư viện backdrop (Kyant): `blur` + `lens` (AGSL) + highlight |
| 2 — Kính mờ | Android 12 (API 31–32) | Làm mờ nền + phủ màu + viền sáng, không khúc xạ | Cùng thư viện backdrop; `lens` tự bỏ qua dưới API 33 |
| 1 — Mờ đục | Android 8–11, hoặc máy yếu ở mọi phiên bản | Nền bán trong suốt + viền sáng 1px + bóng nhẹ | Compose thuần, giữ đơn giản |

Tầng 3 là ưu tiên. Tầng 2 và 1 chỉ cần đúng và không vỡ; chưa đầu tư làm đẹp thêm.

**Tự hạ tầng khi:** máy RAM thấp (`isLowRamDevice`), đang tiết kiệm pin, người dùng tắt
hoạt ảnh trong hệ thống, hoặc đo thấy rớt khung hình.

**Quy tắc dùng kính (theo đúng app iOS):**

- Chỉ thanh tab, thanh tiêu đề khi cuộn, nút hành động và thẻ tương tác là kính
- Thẻ nội dung luôn là bề mặt đặc
- Tối đa vài bề mặt kính mỗi màn, vì mỗi bề mặt phải lấy mẫu nền lại mỗi khung hình khi
  cuộn
- Xem trước chủ đề kính của bàn phím luôn có nền đỡ phía sau (bài học từ iOS: thiếu nền
  đỡ thì chủ đề Midnight gần như vô hình)

### Quyết định kính

Chọn **thư viện backdrop của Kyant** (`io.github.kyant0:backdrop` 2.0.1, Apache 2.0) cho
tầng 3 và tầng 2. Tầng 1 là bề mặt mờ đục tự vẽ. Không dùng Haze và không tự viết AGSL.
Kết luận rút từ spike trên nhánh `spike/android-glass` (không merge), chạy ngày 2026-09-26
trên Galaxy S21 Ultra (SM-G998B, Android 15, 120 Hz).

Demo là một danh sách cài đặt kiểu iOS, cuộn dưới một thanh tiêu đề kính và một thanh tab
viên thuốc. Mỗi chế độ được vuốt 20 lần mỗi vòng, 3 vòng, đọc bằng `dumpsys gfxinfo`. Script
đo: `platforms/android/glass-spike/measure.sh`.

| Chế độ | Khung hình giật | Thời gian khung p50 / p90 / p99 (ms) | GPU p90 / p99 (ms) |
| --- | --- | --- | --- |
| Mờ đục (tầng 1) | 1,83% | 6 / 8 / 11,7 | 4 / 5,7 |
| Haze blur | 1,72% | 10 / 12 / 16 | 6 / 9 |
| Backdrop Liquid Glass | 1,92% | 9 / 15 / 20,7 | 10,7 / 15,7 |

Cả ba chế độ có tỉ lệ khung hình giật gần như nhau, khoảng 1,7–1,9%. Kính tốn thêm GPU (p90
từ 4 lên 10,7ms) nhưng không làm tăng số khung hình giật ở 120 Hz trên máy đời 2021.

| Tiêu chí | Backdrop (Kyant) | Haze 2.0 | Tự viết AGSL |
| --- | --- | --- | --- |
| Giống Liquid Glass iOS | Có khúc xạ ở mép, vệt sáng, bóng | Chỉ kính mờ, không khúc xạ | Làm được, nhưng là viết lại thư viện backdrop |
| API | Blur từ 31, lens từ 33, tự bỏ qua khi không hỗ trợ | Blur từ 31, dưới đó là lớp phủ | Tự lo |
| Dung lượng AAR | ~176 KB (backdrop + shapes) | ~447 KB (haze + utils + blur) | 0 |
| Giấy phép | Apache 2.0 | Apache 2.0 | — |
| Rủi ro | Một người bảo trì | Phổ biến, nhiều người dùng | Tốn công, tự bảo trì shader |

Ảnh chụp trên nhánh spike:
[mờ đục](https://github.com/Funput/Funput/blob/spike/android-glass/platforms/android/glass-spike/results/solid-tabbar.png),
[Haze](https://github.com/Funput/Funput/blob/spike/android-glass/platforms/android/glass-spike/results/haze-tabbar.png),
[Liquid Glass](https://github.com/Funput/Funput/blob/spike/android-glass/platforms/android/glass-spike/results/liquid-tabbar.png).

**Điều kiện đi kèm, bắt buộc trong P1:**

- **Bọc sau API riêng của FunputUI** (ví dụ `Modifier.funputGlass(...)`). Màn hình không bao
  giờ gọi thẳng thư viện. Nếu thư viện ngừng bảo trì, chỉ phải thay một chỗ; shader của nó
  là Apache 2.0 nên có thể vendor lại.
- **Chỉ nhận `CornerBasedShape`.** `lens` ném `UnsupportedOperationException` với hình khác
  (ví dụ `RectangleShape`); spike đã crash vì lỗi này. Thanh tiêu đề dùng bo góc 0 và tắt
  khúc xạ.
- **Tự chọn tầng theo API level.** Dưới API 31 thư viện chỉ vẽ lớp phủ mỏng, gần như trong
  suốt, nên FunputUI phải tự chuyển sang bề mặt mờ đục. Không được để thư viện "tự hạ cấp".
- Giữ quy tắc dùng kính ở trên: chỉ thanh tab, thanh tiêu đề, nút và thẻ tương tác.

## Map màn hình iOS → Android

Android theo đúng khung của iOS: 3 tab (Cài đặt, Giao diện, Giới thiệu), mỗi tab có
chồng màn riêng. Trang Cài đặt là một trang cuộn gồm các thẻ nhóm, như iOS; hình bàn
phím lớn ở đầu trang bị bỏ vì iOS đặt xem trước ở tab Giao diện.

| Màn | iOS | Android hiện tại | Ghi chú | Trạng thái |
| --- | --- | --- | --- | --- |
| Khung app + thanh tab | `AppShellView` | `FunputApp`, `AppNavigationSuite` | Thanh tab kính nổi; giữ chồng màn riêng mỗi tab và predictive back | Chưa bắt đầu |
| Màn khởi động | `LaunchGlassOrb` | `Theme.Funput` (XML) | Dùng SplashScreen API, tránh chớp trắng khi mở ở chế độ tối | Chưa bắt đầu |
| Cài đặt | `SettingsScreen` | `SettingsScreenSections` | Thẻ: Gõ, Bố cục, Thông minh, Phản hồi, Clipboard, Dữ liệu | Chưa bắt đầu |
| Thẻ bật bàn phím | `KeyboardSetupCard` | `KeyboardSetupCard`, `KeyboardSetupStepper` | Android có 2 bước Bật + Chọn, iOS là Truy cập đầy đủ | Chưa bắt đầu |
| Bàn phím vật lý | (không có) | `HardwareKeyboardSettingsSection` | Riêng Android, làm thành một thẻ cùng kiểu | Chưa bắt đầu |
| Sheet chọn giá trị | `SettingsSelectionSheet` | `SettingsPickerSheet` | Kiểu gõ, kiểu bỏ dấu, vị trí bàn phím, hạn clipboard | Chưa bắt đầu |
| Gõ tắt | `ShortcutsScreen`, `Shortcuts/Editor` | `ShortcutsScreen`, `ShortcutEditorSheet` | | Chưa bắt đầu |
| Giao diện + thư viện chủ đề | `AppearanceScreen`, `ThemeGallery`, `ThemeCard` | `AppearanceScreen`, `ThemeCard` | Xem trước bàn phím đầu trang, lưới chủ đề | Chưa bắt đầu |
| Trình sửa chủ đề | `ThemeEditor`, `ThemeEditorPages` | `CustomThemeStudioRoute`, `ThemeEditorPager` | Màn lớn nhất, làm sau cùng | Chưa bắt đầu |
| Cắt ảnh nền | `ThemeImageCropEditor` | `BackgroundFocusPicker` | | Chưa bắt đầu |
| Giới thiệu + giấy phép | `AboutScreen`, `ThirdPartyLicensesScreen` | `AboutScreen`, `LicensesRoute` | | Chưa bắt đầu |

## Lộ trình & theo dõi

Năm giai đoạn, làm tuần tự. Design system phải xong và được duyệt trước khi chuyển màn
nào. Mỗi giai đoạn là một hoặc vài PR con, merge vào nhánh tổng `feat/android-app-redesign`.

| Giai đoạn | Kết quả | Điều kiện qua cổng | Trạng thái |
| --- | --- | --- | --- |
| P0 — Chuẩn bị | Test ảnh chụp trên JVM, token dùng chung, chốt công nghệ kính | Ba việc dưới đây đã merge vào nhánh tổng | Đang làm |
| P1 — FunputUI | Module `:funput-ui` đủ token + component | Màn catalog component được duyệt trên máy Android 13+ | Chưa bắt đầu |
| P2 — Khung app + Cài đặt | Thanh tab kính, màn khởi động, trang Cài đặt, Gõ tắt | Ảnh chụp không vỡ; so cạnh iOS | Chưa bắt đầu |
| P3 — Giao diện | Thư viện chủ đề, trình sửa, cắt ảnh | Như P2 | Chưa bắt đầu |
| P4 — Hoàn tất | Giới thiệu, dọn code cũ, bỏ Material 3; PR nhánh tổng vào `main` | Không còn import `material3` ngoài chỗ đã duyệt; mọi check bắt buộc xanh | Chưa bắt đầu |

### P0 — Chuẩn bị

- [ ] Test Compose trên JVM (Robolectric + Roborazzi), module `:ui-testing`, chụp hiện
      trạng các màn làm bằng chứng "trước" (artifact CI, không commit ảnh)
- [ ] File token dùng chung `design/tokens/app.tokens.json` (giá trị từ app iOS, màu
      nhấn cam) + validator trong buildSrc chạy mỗi lần build
- [x] Spike kính (nhánh `spike/android-glass`, không merge): chọn thư viện backdrop, xem
      "Quyết định kính"

### P1 — FunputUI

- [ ] Tạo module `:funput-ui`, script sinh token Kotlin (và Swift)
- [ ] Đóng gói Be Vietnam Pro (chỉ Latin + tiếng Việt, 4 độ đậm)
- [ ] Shape góc mượt kiểu squircle
- [ ] `FunputScreen`, `LargeTitleBar`, `GlassTabBar`
- [ ] Thẻ: `ContentCard`, `SectionCard`, `GlassCard`
- [ ] Hàng: link, giá trị, công tắc, thanh trượt, hành động
- [ ] `Toggle`, `SegmentedControl`, `GlassButton`, `GlassProminentButton`
- [ ] `SelectionSheet`, `Alert`
- [ ] Bề mặt kính 3 tầng + logic tự hạ tầng
- [ ] Màn catalog component (chỉ bản debug) để duyệt
- [ ] Test ảnh chụp + test semantics cho từng component

### P2 — Khung app + Cài đặt

- [ ] Khung 3 tab + thanh tab kính, giữ predictive back
- [ ] Màn khởi động bằng SplashScreen API
- [ ] Trang Cài đặt dạng thẻ, bỏ hình bàn phím đầu trang
- [ ] Thẻ bật bàn phím, thẻ bàn phím vật lý
- [ ] Sheet chọn giá trị
- [ ] Gõ tắt + trình sửa
- [ ] Bỏ cài đặt "Màu theo hình nền"

### P3 — Giao diện

- [ ] Trang Giao diện + thư viện chủ đề
- [ ] Trình sửa chủ đề
- [ ] Cắt ảnh nền

### P4 — Hoàn tất

- [ ] Giới thiệu + giấy phép
- [ ] Xoá component và theme cũ trong `ui/theme`, `ui/settings/components`
- [ ] Bỏ hoặc thu hẹp phụ thuộc `material3`, `material3-adaptive-navigation-suite`,
      `ui-text-google-fonts`
- [ ] Ảnh chụp Play Store mới + ghi chú phát hành

## Rủi ro & câu hỏi mở

Rủi ro lớn nhất là kính làm giật khi cuộn. Máy cũ trông kém hơn máy mới là rủi ro đã
chấp nhận trong giai đoạn này.

| Rủi ro | Ảnh hưởng | Cách giảm |
| --- | --- | --- |
| Kính gây rớt khung hình khi cuộn | Giật, nóng máy | Giới hạn số bề mặt kính, tự hạ tầng, đo khung hình trong spike |
| Bản tầng 1 trông như hàng hạ cấp | Máy cũ vẫn xấu | Chấp nhận trong giai đoạn này; làm đẹp khi đủ người dùng |
| Component tự làm thiếu trợ năng | TalkBack đọc sai | Test semantics bắt buộc cho mọi component |
| Thư viện backdrop (một người bảo trì) ngừng cập nhật hoặc vỡ khi nâng Compose | Kẹt nâng cấp | Bọc sau API riêng của FunputUI; shader Apache 2.0 có thể vendor lại |
| Người dùng Android thấy app "giống iOS" quá | Khó chịu | Giữ điều hướng, back, rung kiểu Android |
| Người đang dùng "Màu theo hình nền" mất tính năng | Phản hồi tiêu cực | Nêu trong ghi chú phát hành |

**Câu hỏi mở:**

- [ ] Trên Android, giữ hình bàn phím ở đầu trang Cài đặt hay bỏ hẳn cho giống iOS?
- [ ] iOS có chuyển sang đọc token từ `design/tokens/app.tokens.json` không, và khi nào?

**Đã chốt:** phát hành một lần — nhánh tổng chỉ vào `main` khi xong P4 và đạt chất
lượng. Máy duyệt giao diện là máy Android đời mới.

## Hoãn

Làm khi số người dùng đủ lớn:

- Baseline Profile + Macrobenchmark (số đo khởi động, cuộn)
- Ma trận ảnh chụp cho máy cũ: 320/360dp, cỡ chữ 200%, API 26
- Duyệt trên máy thật đời cũ (Android 8–10, RAM 2–3GB), làm đẹp tầng kính 1 và 2
- Thu ảnh chụp và thông tin máy từ người phản hồi
- Chuyển cấu hình Gradle sang convention plugin (`build-logic`)
