<p align="right">
  <a href="README.md">Tiếng Việt</a> · <strong>English</strong>
</p>

<p align="center">
  <img src="assets/logo.png" width="256" alt="Funput logo">
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

**Funput** is an open-source Vietnamese input method — lightweight, privacy-focused,
and designed for each operating system. Type with Telex or VNI on iOS, Android,
macOS, Windows, and Linux. Every platform shares the same processing core for
consistent typing behavior.

## Get started

<p align="center">
  <a href="https://github.com/Funput/Funput/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest_release-22C55E?style=for-the-badge&logo=github&logoColor=white" alt="Download the latest Funput release">
  </a>
  <a href="https://docs.funput.app/">
    <img src="https://img.shields.io/badge/Documentation-Installation_guide-2563EB?style=for-the-badge&logo=readthedocs&logoColor=white" alt="Read the Funput installation guide">
  </a>
  <a href="https://github.com/Funput/Funput/issues">
    <img src="https://img.shields.io/badge/Support-Report_an_issue-E11D48?style=for-the-badge&logo=github&logoColor=white" alt="Report a Funput issue">
  </a>
</p>

## Install by platform

| Platform | Status | Install |
|---|---|---|
| iOS | Released | [Get it on the App Store](https://apps.apple.com/vn/app/id6788829996) |
| macOS | Released | [Download the latest release](https://github.com/Funput/Funput/releases/latest) |
| Windows | Released | [Download the latest release](https://github.com/Funput/Funput/releases/latest) |
| Linux | Released | [Read the installation guide](https://docs.funput.app/) |
| Android | Closed testing | Public enrollment is not available yet |

## Interface

<p align="center">
  <img src="assets/screenshot/screenshot-ios.png" height="420" alt="Funput keyboard on iOS">
  &nbsp;&nbsp;
  <img src="assets/screenshot/screenshot-macos.png" height="420" alt="Funput settings interface on macOS">
  <br>
  <sub>Funput on iOS and the settings experience on macOS.</sub>
</p>

## Performance

The core is written in Rust. Per-keystroke cost, measured with Criterion on a
release build:

| Component / API | Measured scope | Telex | VNI |
|---|---|---:|---:|
| [`funput-core::apply`](crates/funput-core) | Telex/VNI transformation core | 0.049 µs/key | 0.042 µs/key |
| [`funput-engine::Engine::process_char`](crates/funput-engine) | Full pipeline (boundaries + English restore) | 0.106 µs/key | 0.096 µs/key |
| [`funput-ffi::{funput_process_char, funput_buffer}`](crates/funput-ffi) | Engine through the C ABI + composed-buffer reads | 0.114 µs/key | 0.106 µs/key |

> Measured from a release build on an Apple M-series machine; covers Funput's own
> processing only (excludes OS keyboard-event delivery and host-app rendering).
> Numbers depend on hardware — re-run on yours.

See the [benchmark methodology, source code, and reproduction instructions](benchmarks/README.md).

## Project status

Funput is under active development. Features and architecture may continue to
evolve during the project's early releases.

Bug reports, discussions, and contributions are welcome.

## License

[MIT](LICENSE) — © Funput
