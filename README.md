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

**Funput** là bộ gõ tiếng Việt mã nguồn mở, hiện đã phát hành trên macOS,
Windows và Linux; phiên bản Android đang kiểm thử khép kín trên Google Play,
còn phiên bản iOS đang ở giai đoạn TestFlight beta. Mọi nền tảng dùng chung
một lõi xử lý để giữ hành vi gõ nhất quán.

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
| macOS | Đã phát hành | [Tải bản mới nhất](https://github.com/Funput/Funput/releases/latest) |
| Windows | Đã phát hành | [Tải bản mới nhất](https://github.com/Funput/Funput/releases/latest) |
| Linux | Đã phát hành | [Xem hướng dẫn](https://docs.funput.app/) |
| Android | Kiểm thử khép kín | Chưa mở đăng ký công khai |
| iOS | TestFlight beta | [Tham gia TestFlight](https://testflight.apple.com/join/E8YRd3sy) |

## Giao diện

<p align="center">
  <img src="assets/screenshot/screenshot-ios.png" height="420" alt="Bàn phím Funput trên iOS">
  &nbsp;&nbsp;
  <img src="assets/screenshot/screenshot-macos.png" height="420" alt="Giao diện cài đặt Funput trên macOS">
  <br>
  <sub>Bàn phím Funput trên iOS và giao diện cài đặt trên macOS.</sub>
</p>

## Hiệu năng

Lõi xử lý viết bằng Rust. Chi phí mỗi phím, đo bằng Criterion trên release build:

| Thành phần / API | Phạm vi đo | Telex | VNI |
|---|---|---:|---:|
| [`funput-core::apply`](crates/funput-core) | Lõi biến đổi Telex/VNI | 0,054 µs/phím | 0,047 µs/phím |
| [`funput-engine::Engine::process_char`](crates/funput-engine) | Pipeline đầy đủ (boundary + English restore) | 0,11 µs/phím | 0,10 µs/phím |
| [`funput-ffi::{funput_process_char, funput_buffer}`](crates/funput-ffi) | Engine qua C ABI + đọc composed buffer | 0,12 µs/phím | 0,11 µs/phím |

> Đo trên release build, máy Apple M-series; chỉ gồm phần xử lý của Funput (không
> tính OS chuyển sự kiện bàn phím hay app đích render). Số phụ thuộc phần cứng —
> nên chạy lại trên máy bạn.

Xem [phương pháp đo benchmark](benchmarks/README.md).

## Trạng thái

Funput đang được phát triển tích cực. Tính năng và kiến trúc có thể tiếp tục thay đổi trong các phiên bản đầu.

Bug report, thảo luận và đóng góp đều được chào đón.

## License

[MIT](LICENSE) — © Funput
