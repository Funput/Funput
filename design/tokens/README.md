# Token thiết kế của app

`app.tokens.json` là **nguồn duy nhất** cho màu, chữ, bo góc, khoảng cách, độ mờ và hiệu ứng
của app cài đặt Funput. Đây là ngôn ngữ thiết kế **của Funput**: điểm khởi đầu lấy từ app iOS,
nhưng Android không cố làm bản sao iOS. Hiện Android sinh code từ file này; iOS có thể dùng sau. Token của bàn phím (chủ đề, phím) **không** nằm ở
đây — chúng thuộc `theme-runtime` / `ThemeRuntime`.

Bối cảnh: [làm lại app Android theo phong cách iOS](../../docs/features/android-app-redesign.md).

## Định dạng

```json
{
  "schemaVersion": 1,
  "color":      { "<vai trò>": { "light": "#RRGGBB[AA]", "dark": "#RRGGBB[AA]" } },
  "radius":     { "<tên>": số },
  "spacing":    { "<tên>": số },
  "layout":     { "<tên>": số },
  "opacity":    { "<tên>": 0..1 },
  "typography": { "<kiểu>": { "size": số, "lineHeight": số, "weight": 100..900 } },
  "motion":     { "<tên>": { "curve": "spring|easeOut|easeInOut|linear", "durationMs": số, "bounce": 0..1 } }
}
```

- Màu viết `#RRGGBB` hoặc `#RRGGBBAA`. **Alpha đứng cuối** (thứ tự CSS), khác với `0xAARRGGBB`
  của Android.
- Số không có đơn vị: pt trên iOS, dp/sp trên Android.
- `motion` giữ dạng iOS (`spring(duration:bounce:)`). Android tự đổi sang damping/stiffness khi
  sinh code.

## Kiểm tra

Mỗi lần build Android, task `:funput-ui:generateDesignTokens` đọc file này, kiểm tra, và chỉ
sinh code khi mọi luật đều qua; có lỗi thì build dừng.
Nó liệt kê **mọi** lỗi cùng lúc, mỗi lỗi kèm đường dẫn token. Luật kiểm tra:

- đúng cấu trúc, `schemaVersion` được hỗ trợ, đủ các vai trò màu bắt buộc
- tên token viết lowerCamelCase, vì chúng thành tên biến trong code sinh ra
- `groupedBackground` và `cardBackground` phải đặc (không có alpha)
- `label` đạt ≥ 4.5:1, `accent` đạt ≥ 3:1 trên cả hai nền, ở cả sáng và tối (WCAG AA)
- `onAccent` (chữ trên nút nền cam) đạt ≥ 4.5:1 trên `accent`. Chữ trắng trên cam sáng chỉ
  đạt 3,56:1, nên chữ trên nút là màu nâu rất sẫm `#1F1300`
- `opacity` trong 0..1, các nhóm số khác không âm
- `lineHeight` không nhỏ hơn `size`, vì dấu chồng tiếng Việt (ế, ự, ỡ) cần chỗ
- `durationMs` trong 1..2000, `bounce` trong 0..1

Code nằm ở `platforms/android/buildSrc/src/main/kotlin/app/funput/build/tokens/`. Chạy test:

```bash
cd platforms/android && ./gradlew -p buildSrc test
```

## Nguồn giá trị

| Nhóm | Lấy từ |
|---|---|
| Nền, thẻ, chữ, đường kẻ, `success`, `destructive`, `brand*` | Màu hệ thống iOS (`systemGroupedBackground`, `secondarySystemGroupedBackground`, `label`, `secondaryLabel`, `tertiaryLabel`, `separator`, `systemGreen`, `systemRed`, `systemPink`, `systemPurple`, `systemBlue`), sáng/tối |
| `cardStroke` | `Color.primary.opacity(0.07)` trong `ContentCard.swift` |
| `accent` | Cam Funput. Sáng `#D46B08`, sẫm hơn seed `#EF8A1A` để đạt 3:1 trên nền trắng. Tối `#FFA43F`, trùng `BrandOrange` của Android |
| `radius` | `ContentCard` (22), `InteractiveGlassCard` (20), `ThemeCardThumbnail` (16), ô icon (12), `AboutLinkSection` (10) |
| `spacing`, `layout` | `AppScreen.swift`: khoảng cách thẻ 18, lề 18, rộng tối đa 720; `ContentCard`: padding 18, viền 0.5 |
| `opacity` | `tintFill` 0.12 (ô icon, badge), `glassTintSelected` 0.16, `screenAccentWash` 0.08 (gradient nền `AppScreen`) |
| `typography` | Thang riêng của Funput theo thói quen Android (body 16sp). Line height ≥ 1,4× cỡ chữ ở chữ thường để dấu chồng tiếng Việt không bị cắt |
| `motion` | `FunputLaunchExperience.swift` (launch), `ThemeGallery.swift` (chọn chủ đề) |

## Thêm hoặc đổi token

1. Sửa `app.tokens.json`.
2. Build Android (hoặc chạy `./gradlew :funput-ui:generateDesignTokens`) để kiểm tra.
3. Nếu thêm vai trò màu mà mọi nền tảng bắt buộc phải có, thêm nó vào
   `DesignTokenRules.RequiredColors`.
4. Đổi cấu trúc file thì tăng `schemaVersion` và cập nhật parser trong cùng PR.

## Việc tiếp theo

- **Android:** `:funput-ui` sinh `FunputTokens.kt` từ file này bằng `GenerateDesignTokensTask`
  (validate trước, sinh code sau).
- **iOS:** sinh code Swift khi app iOS chuyển sang đọc file này.
- **iOS:** đặt `AccentColor` sang cam Funput trong một PR iOS riêng, để hai app khớp nhau.
