# R8 / ProGuard rules for the release build.
#
# The critical rule for this app — keeping the JNI bridge so the Rust engine keeps
# linking after obfuscation — lives in :ime (ime/consumer-rules.pro) and is applied
# here automatically. Add only app-module-specific keeps below.
#
# AndroidX, Compose, and DataStore ship their own consumer rules, so no extra keeps
# are needed for them. Keep this file even if empty: it is the documented place for
# release-only keep rules, and it is referenced from app/build.gradle.kts.
