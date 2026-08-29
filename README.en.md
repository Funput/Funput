<p align="right">
  <a href="README.md">Tiếng Việt</a> · <strong>English</strong>
</p>

<p align="center">
  <img
    src="assets/horizontal-lockup/gradient.png"
    width="520"
    alt="Funput"
  >
</p>

---

<p align="center">
  <strong>Funput</strong> is an open-source Vietnamese input method and keyboard that is lightweight and privacy-focused.<br>
  It supports Telex and VNI across iOS, Android, macOS, Windows, and Linux, delivering a familiar and consistent Vietnamese typing experience across all your devices.
</p>

## Get started

<p align="center">
  <a href="https://github.com/Funput/Funput/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest_release-22C55E?style=for-the-badge&logo=github&logoColor=white" alt="Download the latest Funput release">
  </a>
  <a href="https://docs.funput.app/docs/install/">
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
| Linux | Released | [Read the installation guide](https://docs.funput.app/docs/install/linux) |
| Android | Closed testing | [Email to join](mailto:hello@funput.app) |

> [!NOTE]
> Google Play requires a full **14-day** closed test with a minimum number of testers before production. The previous round did not meet that bar, so Funput has to run another 14-day loop.
>
> If you use Android and want to help get Funput onto the Play Store, email [hello@funput.app](mailto:hello@funput.app) (preferably from the Gmail account tied to Google Play). We will invite you into the closed testing track.

## Interface

<p align="center">
  <img src="assets/screenshot/ios.png" height="420" alt="Funput keyboard on iOS">
  &nbsp;&nbsp;
  <img src="assets/screenshot/android.png" height="420" alt="Funput keyboard on Android">
  <br>
  <sub>Funput keyboard on iOS and Android.</sub>
</p>

<p align="center">
  <img src="assets/screenshot/macOS.png" height="280" alt="Funput settings on macOS">
  &nbsp;&nbsp;
  <img src="assets/screenshot/windows.png" height="280" alt="Funput settings on Windows">
  <br>
  <sub>Settings on macOS and Windows.</sub>
</p>

## Architecture

All five platforms share one Rust core. Each shell talks to the core through the
right bridge: [`funput-ffi`](crates/funput-ffi) (C ABI) for iOS, macOS, and Linux;
[`funput-jni`](crates/funput-jni) for Android; a direct
[`funput-engine`](crates/funput-engine) link for Windows.

<p align="center">
  <a href="assets/design/architecture.png">
    <img
      src="assets/design/architecture.png"
      width="960"
      alt="Funput architecture: iOS, macOS, and Linux via funput-ffi; Android via funput-jni; Windows via direct link; all converging on funput-engine and funput-core"
    >
  </a>
  <br>
  <sub><a href="assets/design/architecture.png">Click for full-size image</a></sub>
</p>

## Project status

Funput is under active development. Features and architecture may continue to
evolve during the project's early releases.

Bug reports, discussions, and contributions are welcome.

## Support

Funput is a community project — free for everyone.
If you find Funput useful, you're welcome to support the project:

<p align="center">
  <img src="assets/donate/qr.png" width="360" alt="Donate to Funput">
  <br>
  <sub>Made with ❤️ by Funput</sub>
</p>

## License

[MIT](LICENSE) — © Funput

