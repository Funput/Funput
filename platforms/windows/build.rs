//! Build script: compile the Slint UI with the Cupertino (macOS-like) style, and on
//! Windows embed the app icon + a DPI-aware, `asInvoker` application manifest.

fn main() {
    // Compile `ui/app.slint` into Rust. Cupertino gives the macOS-style widgets that
    // match Funput's liquid-glass look (vs. Fluent's native Win11 widgets).
    let config = slint_build::CompilerConfiguration::new().with_style("cupertino".into());
    slint_build::compile_with_config("ui/app.slint", config).expect("Slint build failed");

    // Embed icon + manifest only when targeting Windows (the canonical build host).
    if std::env::var("CARGO_CFG_TARGET_OS").as_deref() == Ok("windows") {
        let mut res = winresource::WindowsResource::new();
        res.set_icon("icons/icon.ico");
        res.set_manifest(MANIFEST);
        // Version-info strings shown by Windows. Without these, winresource derives
        // them from the crate name ("funput-windows"); Task Manager surfaces
        // FileDescription as the process "Name". Pin them to "Funput" so the
        // displayed name matches the macOS/Linux apps.
        res.set("FileDescription", "Funput");
        res.set("ProductName", "Funput");
        res.set("InternalName", "Funput");
        res.set("OriginalFilename", "Funput.exe");
        if let Err(e) = res.compile() {
            println!("cargo:warning=winresource failed: {e}");
        }
    }
}

/// PerMonitorV2 DPI awareness + run as the invoking user (no UAC elevation — note
/// that an elevated foreground app can't be typed into, as the onboarding warns).
const MANIFEST: &str = r#"<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<assembly xmlns="urn:schemas-microsoft-com:asm.v1" manifestVersion="1.0">
  <trustInfo xmlns="urn:schemas-microsoft-com:asm.v3">
    <security>
      <requestedPrivileges>
        <requestedExecutionLevel level="asInvoker" uiAccess="false" />
      </requestedPrivileges>
    </security>
  </trustInfo>
  <application xmlns="urn:schemas-microsoft-com:asm.v3">
    <windowsSettings>
      <dpiAwareness xmlns="http://schemas.microsoft.com/SMI/2016/WindowsSettings">PerMonitorV2</dpiAwareness>
    </windowsSettings>
  </application>
  <compatibility xmlns="urn:schemas-microsoft-com:compatibility.v1">
    <application>
      <supportedOS Id="{8e0f7a12-bfb3-4fe8-b9a5-48fd50a15a9a}" />
    </application>
  </compatibility>
</assembly>
"#;
