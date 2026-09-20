// Stamp CARGO_PKG_VERSION into the Windows shell's manifest *and* its lockfile.
// Release jobs rewrite Cargo.toml from the git tag, then `cargo --locked` refuses
// to run if Cargo.lock still records the placeholder version (1.2026.1).
"use strict";

const fs = require("fs");
const version = process.argv[2];
if (!version) {
  console.error("usage: node stamp-version.js <version>");
  process.exit(1);
}

const toml = "platforms/windows/Cargo.toml";
let manifest = fs.readFileSync(toml, "utf8");
const stamped = manifest.replace(/^version = ".*"/m, `version = "${version}"`);
if (stamped === manifest) {
  console.error(`${toml}: no package version line`);
  process.exit(1);
}
fs.writeFileSync(toml, stamped);

const lockPath = "platforms/windows/Cargo.lock";
let lock = fs.readFileSync(lockPath, "utf8");
const next = lock.replace(
  /(name = "funput-windows"\r?\nversion = ")[^"]+(")/,
  `$1${version}$2`,
);
if (next === lock) {
  console.error(`${lockPath}: funput-windows version not found`);
  process.exit(1);
}
fs.writeFileSync(lockPath, next);
