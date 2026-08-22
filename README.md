<p align="right">
  <strong>Tiếng Việt</strong> · <a href="README.en.md">English</a>
</p>

<p align="center">
  <img src="assets/logo.png" width="256">
</p>

<pre align="center">
   ███████╗██╗   ██╗███╗   ██╗██████╗ ██╗   ██╗████████╗
   ██╔════╝██║   ██║████╗  ██║██╔══██╗██║   ██║╚══██╔══╝
█████╗  ██║   ██║██╔██╗ ██║██████╔╝██║   ██║   ██║
██╔══╝  ██║   ██║██║╚██╗██║██╔═══╝ ██║   ██║   ██║
██║     ╚██████╔╝██║ ╚████║██║     ╚██████╔╝   ██║
╚═╝      ╚═════╝ ╚═╝  ╚═══╝╚═╝      ╚═════╝    ╚═╝
</pre>

---

**Funput** là bộ gõ tiếng Việt mã nguồn mở — nhẹ, tập trung vào quyền riêng tư,
thiết kế riêng cho từng hệ điều hành. Gõ Telex hoặc VNI trên iOS, Android, macOS,
Windows và Linux. Mọi nền tảng dùng chung một lõi xử lý để giữ hành vi gõ nhất quán.

## Bắt đầu

<p align="center">
  <a href="https://github.com/Funput/Funput/releases/latest">
    <img src="https://img.shields.io/badge/Tải_xuống-Bản_mới_nhất-22C55E?style=for-the-badge&logo=github&logoColor=white" alt="Tải phiên bản Funput mới nhất">
  </a>
  <a href="https://docs.funput.app/">
    <img src="https://img.shields.io/badge/Tài_liệu-Hướng_dẫn_cài_đặt-2563EB?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Hướng dẫn cài đặt Funput">
  </a>
  <a href="https://github.com/Funput/Funput/issues">
    <img src="https://img.shields.io/badge/Hỗ_trợ-Báo_lỗi-E11D48?style=for-the-badge&logo=github&logoColor=white" alt="Báo lỗi Funput">
  </a>
</p>

## Cài đặt theo nền tảng

| Nền tảng | Trạng thái | Cài đặt |
|---|---|---|
| iOS | Đã phát hành | [Tải trên App Store](https://apps.apple.com/vn/app/id6788829996) |
| macOS | Đã phát hành | [Tải bản mới nhất](https://github.com/Funput/Funput/releases/latest) |
| Windows | Đã phát hành | [Tải bản mới nhất](https://github.com/Funput/Funput/releases/latest) |
| Linux | Đã phát hành | [Xem hướng dẫn](https://docs.funput.app/) |
| Android | Kiểm thử khép kín | [Gửi email để tham gia](mailto:hello@funput.app) |

Google Play yêu cầu kiểm thử khép kín đủ **14 ngày** với số tester tối thiểu trước khi xuất bản. Vòng trước chưa đạt ngưỡng đó, nên Funput phải chạy lại vòng 14 ngày.

Nếu bạn dùng Android và muốn giúp đưa Funput lên Play Store, hãy gửi email tới
[hello@funput.app](mailto:hello@funput.app) (nên dùng Gmail gắn với tài khoản
Google Play). Chúng tôi sẽ mời bạn vào nhóm kiểm thử khép kín.

## Giao diện

<p align="center">
  <img src="assets/screenshot/ios.png" height="420" alt="Bàn phím Funput trên iOS">
  &nbsp;&nbsp;
  <img src="assets/screenshot/android.png" height="420" alt="Bàn phím Funput trên Android">
  <br>
  <sub>Bàn phím Funput trên iOS và Android.</sub>
</p>

<p align="center">
  <img src="assets/screenshot/macOS.png" height="280" alt="Giao diện cài đặt Funput trên macOS">
  &nbsp;&nbsp;
  <img src="assets/screenshot/windows.png" height="280" alt="Giao diện cài đặt Funput trên Windows">
  <br>
  <sub>Giao diện cài đặt trên macOS và Windows.</sub>
</p>

## Hiệu năng

Lõi xử lý viết bằng Rust. Chi phí mỗi phím, đo bằng Criterion trên release build:

| Thành phần / API | Phạm vi đo | Telex | VNI |
|---|---|---:|---:|
| [`funput-core::apply`](crates/funput-core) | Lõi biến đổi Telex/VNI | 0,049 µs/phím | 0,042 µs/phím |
| [`funput-engine::Engine::process_char`](crates/funput-engine) | Pipeline đầy đủ (boundary + English restore) | 0,106 µs/phím | 0,096 µs/phím |
| [`funput-ffi::{funput_process_char, funput_buffer}`](crates/funput-ffi) | Engine qua C ABI + đọc composed buffer | 0,114 µs/phím | 0,106 µs/phím |

> Đo trên release build, máy Apple M-series; chỉ gồm phần xử lý của Funput (không
> tính OS chuyển sự kiện bàn phím hay app đích render). Số phụ thuộc phần cứng —
> nên chạy lại trên máy bạn.

Xem [phương pháp đo benchmark](benchmarks/README.md).

## Trạng thái

Funput đang được phát triển tích cực. Tính năng và kiến trúc có thể tiếp tục thay đổi trong các phiên bản đầu.

Bug report, thảo luận và đóng góp đều được chào đón.

## License

[MIT](LICENSE) — © Funput
