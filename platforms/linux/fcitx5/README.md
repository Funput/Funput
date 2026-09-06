<p align="center">
  <img
    src="../../../assets/horizontal-lockup/gradient.png"
    width="360"
    alt="Funput"
  >
</p>

<p align="center">
  <strong>Funput cho Fcitx5</strong> — addon input method, build ra <code>libfunput.so</code>.<br>
  Một trong hai shell trên Linux · adapter mỏng trên <code>funput::Composer</code>
</p>

<p align="center">
  <a href="../README.md">
    <img src="https://img.shields.io/badge/←_Quay_lại-README_Linux-2563EB?style=for-the-badge&logo=linux&logoColor=white" alt="README Linux">
  </a>
  <a href="../ibus/">
    <img src="https://img.shields.io/badge/Shell_còn_lại-IBus-6B7280?style=for-the-badge" alt="Shell IBus">
  </a>
</p>

---

## Vì sao Fcitx5 là mặc định

`install.sh` cài gói **Fcitx5 trên mọi desktop**, kể cả GNOME. Trước đây nó đoán theo
`XDG_CURRENT_DESKTOP` — KDE thì Fcitx5, còn lại IBus — và điều đó trao cho phần lớn người dùng
đúng cái shell **không với tới nổi** một client kiểu WPS Office.

Lý do nằm ở một năng lực mà IBus không có: khi client tự giấu preedit, Fcitx5 còn **panel
preedit của riêng nó** để vẽ thay. IBus thì hết đường — ForwardKeyEvent đã bị bỏ, surrounding
text không bao giờ tới. Xem [Client tự giấu preedit](#client-tự-giấu-preedit).

| | Fcitx5 | IBus |
|---|---|---|
| Panel preedit riêng | **Có** | Không |
| Chạy với WPS Office | **Có** | Không |
| Được session nối sẵn | Chỉ KDE | GNOME · Ubuntu |
| Gói | `funput` | `funput-ibus` |

## Cài đặt

Xem [README Linux](../README.md#cài-đặt). Ngắn gọn:

```bash
curl -fsSL https://raw.githubusercontent.com/Funput/Funput/main/platforms/linux/install.sh | bash
```

Rồi bật trong **Fcitx5 Configuration → `+` → bỏ chọn "Only Show Current Language" → Funput**.

> [!IMPORTANT]
> **Ngoài KDE, cài xong vẫn chưa gõ được ngay.** Phiên desktop của bạn đang nối vào IBus, nên
> Fcitx5 không nhận được gì cho tới khi bạn đặt biến môi trường cho session rồi **đăng xuất
> đăng nhập lại**.
>
> | Session | Cần đặt |
> |---|---|
> | **X11** | Bộ ba `XMODIFIERS` + `GTK_IM_MODULE` + `QT_IM_MODULE` |
> | **Wayland · GNOME** *(và sway)* | `XMODIFIERS=@im=fcitx` và `QT_IM_MODULE=fcitx` — hoặc `QT_IM_MODULES=wayland;fcitx` trên Qt 6.8.2+ — **để trống** `GTK_IM_MODULE` |
> | **Wayland · KDE** | Chỉ `XMODIFIERS=@im=fcitx` |
>
> Dưới Wayland, GTK 3/4 tự nói chuyện với compositor qua `text-input-v3`, nên đặt cả bộ ba
> khiến KWin nhấp nháy cửa sổ gợi ý. `install.sh` đọc `$XDG_SESSION_TYPE` và chỉ in đúng khối
> áp dụng cho bạn.

> [!CAUTION]
> **Trên KDE, đừng chạy `fcitx5 -r`.** KWin khởi động Fcitx5 từ Virtual Keyboard KCM và trao cho
> nó một socket mà tiến trình thay thế **không kế thừa được**. Khởi động lại daemon sẽ làm hỏng
> việc nhập liệu của mọi client Wayland cho tới lần đăng nhập kế tiếp. Đăng xuất rồi đăng nhập
> lại mới là cách đúng.

## Client tự giấu preedit

Một số client **khai là có** năng lực Preedit rồi **không vẽ gì cả**. Từ đang gõ trở nên vô
hình cho tới khi bạn nhấn Space. WPS Office là trường hợp điển hình.

Chế độ không preedit cũng không cứu được: WPS không hiện thực `Qt::ImSurroundingText`, và ép
xoá-rồi-commit thì làm nát chữ — `nguyen64` trả về `nguyenênễn`.

Nên shell Fcitx5 **tự vẽ panel preedit** cho mọi tên tiến trình nằm trong allowlist ở
[`src/hidden_preedit.cpp`](src/hidden_preedit.cpp):

```cpp
constexpr std::string_view kHiddenPreedit[] = {
    "wps",    // WPS Writer
    "wpp",    // WPS Presentation
    "et",     // WPS Spreadsheets
    "wpspdf", // WPS PDF
};
```

Khớp theo **basename của tiến trình**, không phân biệt hoa thường. Thêm một client kiểu này chỉ
là thêm một cái tên vào danh sách — `updatePreedit()` đã vẽ panel cho bất kỳ ai trong đó.

> [!WARNING]
> **Đừng khớp theo cờ capability hay theo việc thiếu surrounding text.** Chúng nói dối theo cả
> hai chiều, và sẽ kéo vào cả những client vốn đang chạy tốt. Một cái tên trong danh sách là
> có chủ ý; một điều kiện suy đoán thì không.

## Dành cho developer

### Vai trò

Addon này là **adapter mỏng** trên [`funput::Composer`](../common/compose/) — nơi giữ luật gõ và
dùng chung với shell IBus. File ở đây **chỉ dịch**: `fcitx::KeyEvent` vào,
`funput::ComposePlan` ra, rồi thi hành plan đó lên input context.

Hình dạng composition giống **shell IMKit trên macOS**, không phải đường tiêm backspace của
Windows: từ đang gõ hiện dưới dạng preedit gạch chân, và commit khi gặp ranh giới từ, phím điều
hướng, hoặc lúc bật/tắt VI/EN.

| File | Dòng | Việc |
|---|---|---|
| [`src/funput_engine.h`](src/funput_engine.h) | 105 | Kiểu addon, config, và mọi thứ nửa còn lại cần |
| [`src/funput_engine.cpp`](src/funput_engine.cpp) | 110 | Vòng đời: watcher, nạp lại settings, bật/tắt chế độ |
| [`src/funput_input.cpp`](src/funput_input.cpp) | 69 | Một phím: chuẩn hoá, hỏi composer |
| [`src/funput_client.cpp`](src/funput_client.cpp) | 99 | Thi hành plan lên client — preedit, commit, sửa tài liệu |
| [`src/hidden_preedit.cpp`](src/hidden_preedit.cpp) | 49 | Allowlist client tự giấu preedit |

### Một phím đi qua đâu

```text
fcitx::KeyEvent
      │
      ▼
toKeyEvent() ............. keysym X11 đi thẳng qua (IBus dùng cùng giá trị);
      │                    chỉ mods và keysym→Unicode là của riêng Fcitx5
      ▼
toggleChord_.feed() ...... phím bật/tắt VI/EN — nuốt phím, xong
      │
      ▼   (bỏ qua mọi key release: luật gõ không phụ thuộc chúng,
      │    và mỗi framework báo release một kiểu khác nhau)
      │
applyNonPreeditMode() .... hỏi lại GIỮA CÁC TỪ xem client này có nhận
      │                    được sửa tài liệu không
      ▼
textBeforeCaret() ........ MỘT lần đọc tài liệu, dùng cho HAI việc
      │
      ▼
Composer::onKey() ──▶ ComposePlan ──▶ applyPlan()
      │
      ▼
adoptWordBeforeBackspace()  chỉ khi Backspace mở lại một từ đã commit
```

Vài chi tiết đắt giá:

- **Đọc tài liệu một lần, dùng hai việc.** Nó vừa để composer kiểm xem lần sửa trước có thật sự
  đáp xuống không, vừa là đúng đoạn text mà một Backspace cần.
- **`reopen` hỏi `classify()` chứ không so keysym.** Nhờ vậy `Ctrl+Backspace` (xoá cả từ) không
  lọt vào — đó là phím tắt, không phải Backspace.
- **`adoptWordBeforeBackspace()` chạy *sau* plan và không tự viết gì.** Phím đi qua, app tự xoá
  ký tự của nó, engine chỉ nhận quyền sở hữu phần còn lại để phím kế tiếp sửa được.

### Preedit, và ai commit khi mất focus

`updatePreedit()` **luôn** publish client preedit, kể cả cho client không vẽ được nó. Đây không
phải thừa: Fcitx5 tự commit `clientPreedit()` khi input context mất focus (watcher focus-out
`ReservedFirst` của `Instance`), và **đó chính là thứ giữ cho một từ gõ dở không biến mất** khi
bạn bấm sang ô khác.

Hiển thị thì không bị ảnh hưởng — `InputContext::updatePreedit()` trả về sớm nếu thiếu năng lực
Preedit, nên những client đó vẫn nhận panel preedit do Fcitx5 vẽ.

```cpp
panel.setClientPreedit(preedit);              // luôn luôn — để focus-out flush được
if (!context->capabilityFlags().test(fcitx::CapabilityFlag::Preedit) ||
    hidesClientPreedit(context)) {
    panel.setPreedit(preedit);                // panel do Fcitx5 vẽ
}
```

> [!NOTE]
> `deactivate()` **không được commit lần nữa** — Fcitx5 đã commit client preedit trước khi gọi
> nó. Commit lại là nhân đôi từ.

### Chế độ không preedit phía Fcitx5

Toàn bộ lý lẽ và số liệu đo nằm ở
[README Linux — Chế độ không preedit](../README.md#chế-độ-không-preedit). Phần thuộc riêng
shell này:

- **`lastSurroundingOk_`** bật bởi sự kiện `InputContextSurroundingTextUpdated`, xoá ở
  `activate()`. Nó kiểm `surroundingText().isValid()` chứ **không** kiểm
  `CapabilityFlag::SurroundingText` — trên GNOME/Wayland cờ đó nói dối cả hai chiều. Text rỗng
  với con trỏ 0 **vẫn** là hợp lệ: đó là một ô GTK trống, và là tín hiệu "client đã lên tiếng".
- **`applyNonPreeditMode()` không bao giờ lật giữa từ.** Hai chế độ bất đồng về chỗ từ đang gõ
  nằm ở đâu, nên lật khi đang soạn dở sẽ để engine và client mô tả hai thứ khác nhau. Cùng một
  cổng gác với `applyNonPreeditMode()` bên IBus.
- **Có selection thì bỏ luôn cả sửa chữa lẫn composition.** Xoá lúc đó sẽ nuốt đoạn người dùng
  đang bôi đen. Bỏ mỗi lệnh xoá sẽ nhân đôi từ; bỏ im lặng sẽ để engine tin rằng nó sở hữu một
  từ mà nó không sửa được, khiến lệnh xoá của phím kế tiếp nhắm vào chữ Funput chưa từng viết.
- **`deleteSurroundingText` tính theo ký tự.** Engine phát ra ký tự, API nhận ký tự — không có
  chỗ nào chuyển đổi dọc đường. Riêng `textBeforeCaret()` thì phải hoà giải: Fcitx5 báo con trỏ
  theo **ký tự** trong khi text là **UTF-8**, nên cắt theo con trỏ như thể nó là byte offset sẽ
  chặt đôi chữ tiếng Việt.

### Cấu hình

Fcitx5 chỉ thấy **đúng một** tuỳ chọn: một nút mở app Cài đặt GTK.

```cpp
FCITX_CONFIGURATION(
    FunputEngineConfig,
    fcitx::ExternalOption openSettings{
        this, "OpenSettings", "Open Funput Settings", "funput-settings"};);
```

Cố ý như vậy. Thiết lập thật nằm ở `~/.config/Funput/settings.json`, và `settings-gtk` **ghi đè
nguyên file** từ struct của chính nó — nên nhân bản chúng thành `Option` của Fcitx5 là tự tạo ra
hai nguồn sự thật, với một bên xoá bên kia.

`getConfigForInputMethod()` mặc định trả về `getConfig()`, nên nút Configure của input method
trong `fcitx5-configtool` cũng dùng chính cái này. Từ configtool 5.1.6+, khi `ExternalOption` là
trường duy nhất thì nó chạy thẳng lệnh luôn.

Nạp lại settings là **tức thì**: một fd `inotify` (`SettingsWatcher`) được nối vào chính event
loop của Fcitx5 qua `addIOEvent`.

### Build và cài

```bash
FUNPUT_FRAMEWORK=fcitx5 ../build.sh    # từ platforms/linux/fcitx5
```

| | |
|---|---|
| Chuẩn C++ | **20** — header công khai của Fcitx5 5.1+ dùng `std::span` và `string_view::starts_with`, nên 17 không compile nổi trên Fedora |
| Install prefix | Ép về **`/usr`**, không phải `/usr/local` mặc định |
| Sản phẩm | `libfunput.so` + `libfunput_ffi.so` đặt cạnh nhau trong addon dir |
| Gói | `funput` |

> [!IMPORTANT]
> Prefix `/usr` là **bắt buộc**, không phải sở thích: Fcitx5 chỉ quét addon dir hệ thống
> (`/usr/lib/<triplet>/fcitx5`, `/usr/share/fcitx5`) và **bỏ qua `/usr/local`**. Prefix này cũng
> khiến `GNUInstallDirs` dùng đúng libdir multiarch của Debian, nhờ đó
> `FCITX_INSTALL_ADDONDIR` phân giải chính xác. Nó phải được đặt **trước**
> `find_package(Fcitx5Utils)`, vì đó là chỗ các đường dẫn cài được tính ra.

`libfunput_ffi.so` được cài **cạnh** `libfunput.so` và phân giải qua `INSTALL_RPATH "$ORIGIN"`,
nên không cần thêm mục nào vào `ldconfig` toàn hệ thống.

Metadata addon nằm ở [`data/`](data): `funput-addon.conf.in` đăng ký addon
(`Library=libfunput`, `Category=InputMethod`), còn `funput.conf.in` đăng ký input method
(`Label=FU`, `LangCode=vi`).

## Giấy phép

MIT — [`LICENSE`](../../../LICENSE).

<p align="center">
  <sub>
    <a href="https://funput.app">funput.app</a>
    ·
    <a href="../README.md">README Linux</a>
    ·
    Made with ♥ by Funput
  </sub>
</p>
