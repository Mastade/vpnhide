# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## v0.6.1

### Added
- Detect gb.sweetlifecl as a Russian-vendor package in the app picker filter, so it shows up under the Russian-only filter alongside other domestic apps.

### Fixed
- Magisk versions before v28 failed to install vpnhide-ports and vpnhide-kmod with an unpack error — restored the META-INF/com/google/android/{update-binary,updater-script} entries the older managers expect.

## v0.6.0

### Added
- App hiding mode in Protection — hide selected apps from selected observer apps at the PackageManager level. Observer apps can no longer list, resolve, or query hidden apps.
- Ports hiding mode plus new vpnhide-ports.zip module — block selected apps from reaching 127.0.0.1 / ::1 ports to hide locally running VPN/proxy daemons (Clash, sing-box, V2Ray, Happ, etc.).
- Ports module integration in the dashboard — shows install state, active rules, observer count, and version mismatch/update warnings.

### Changed
- The old Apps tab is now Protection, split into three modes: Tun, Apps, and Ports.
- Ports rules apply immediately on Save and are restored automatically on boot.
- vpnhide-ports.zip is now included in the release/update pipeline with Magisk/KernelSU update metadata.

### Fixed
- Fixed LinkProperties filtering so VPN routes are stripped more reliably from app-visible network snapshots.
- Fixed SIOCGIFCONF filtering on some Android 12/13 5.10 kernels where the previous hook could succeed but never fire.
- Fixed debug log collection so app logcat entries are captured reliably on devices where logcat via su misses them.

## v0.5.3

### Added
- Debug log export — open the Diagnostics tab and tap "Collect debug log" at the bottom. The app gathers dmesg, check results, device info, module status, kernel symbols, targets, interfaces, routing tables, and logcat into a zip. Save to disk or share directly.
- Kernel module debug logging toggle — all 6 kretprobe hooks now log detailed info (UID, target status, interface name, filter decisions) when debug mode is active. Enabled automatically during debug log collection.

## v0.5.2

### Fixed
- Fixed SIOCGIFCONF filtering on kernel 5.10 (tun0 was visible in interface enumeration)
- Fixed zygisk first-launch race: dashboard no longer shows false "inactive" status
- Added recv hook in zygisk for netlink filtering on Android 10
- Fixed hardcoded v0.1.0 in module installer messages

## v0.5.1

### Fixed
- Fixed false "LSPosed/Vector not installed" warning when LSPosed uses non-standard module path (e.g. zygisk_lsposed)
- Fixed false LSPosed config warnings when hooks are already active at runtime
- "No target apps configured" now checks all modules, not just LSPosed

## v0.5.0

### Added
- Built-in diagnostics merged into the main app (separate test app removed)
- New app icon and chameleon mascot branding
- Dashboard with module status, LSPosed config validation, version checks, and live protection verification
- Native module install recommendation — the app detects your kernel and tells you exactly which module to download
- Per-app protection layer toggles (L/K/Z) — control LSPosed, kernel module, and Zygisk independently for each app
- Magisk/KSU auto-update for kmod and zygisk modules
- App update check via GitHub Releases
- Changelog with version history

### Changed
- Replaced WebUI with native Compose UI for module management
- Zygisk API lowered from v5 to v2 for Magisk v27 compatibility
- Apps tab: search in top bar, filter menu, fast scroll with letter indicator, Russian apps filter

### Fixed
- Fixed potential system_server crash caused by race condition in writeToParcel hooks
- App no longer removes itself from target lists when saving

## v0.4.2

### Added
- ioctl SIOCGIFMTU detection and filtering
- LinkProperties routes filtering in LSPosed hooks

### Fixed
- Fixed VPN LinkProperties deserialization crash

## v0.4.1

### Added
- Russian translation
- Root access error screen with clear instructions

## v0.4.0

### Added
- VPN Hide app with target picker UI — select apps to hide VPN from
- Built-in diagnostics — 26 checks covering all detection vectors
- Auto-detect VPN and auto-add self to target list

### Changed
- Replaced WebUI with native Compose UI

### Fixed
- Zygisk targets reading on Magisk (SELinux blocking /data/adb/)
