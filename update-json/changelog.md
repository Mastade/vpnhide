## v0.6.2

### Added
- Full system logcat recording on the Diagnostics screen — Start/Stop an unfiltered capture of all logcat buffers (main/system/crash/events/radio), then Save to a user-picked location via the Storage Access Framework or Share through the system sheet. Useful for submitting diagnostic logs straight from the app without running adb logcat by hand.

### Changed
- Module zips and APK now include git provenance in the version string. Official release builds from a tag stay at the clean version (e.g. 0.6.2); intermediate builds from main or a feature branch show up as 0.6.2-5-gabc1234 (or -dirty) in the Magisk/KSU manager and Android Settings, so debug-log submissions identify the exact commit.

### Fixed
- Target apps (Ozon, Home Assistant, Megafon, Chrome, etc.) no longer hang on infinite load when vpnhide-zygisk is installed. Our recv/recvmsg hooks used to run the netlink-dump filter on every socket regardless of type — on TCP/UDP/Unix the first bytes are arbitrary user data (TLS ciphertext, HTTP body) and occasionally matched RTM_NEWLINK/RTM_NEWADDR by chance, causing the filter to mangle the buffer in-place and corrupt the TLS stream. Gated both hooks on AF_NETLINK so non-netlink traffic passes through untouched.
- Target apps no longer fail Java-level hooks on cold boot. service.sh in the kmod and zygisk modules used to resolve package names to UIDs as soon as pm list packages started responding — but PackageManager responds early with only the system-package snapshot, so user-installed targets (including the vpnhide app itself) got resolved to empty. /data/system/vpnhide_uids.txt was written with at most one UID, the LSPosed hook cached that empty set on its first writeToParcel call, and the Java-level filtering silently no-opped until the next lucky boot. Now service.sh waits until the vpnhide package itself is visible in pm list packages -U, so the full user-package index is ready before resolving.
- Ozon and other apps with root-detection scanning /proc/self/fd no longer hang when vpnhide-zygisk is installed. The module's zygote-side on_load was leaking the module-dir fd, which every forked app process inherited — root-tamper scans detected a descriptor pointing inside /data/adb/modules/ and refused to continue. The fd is now explicitly closed before any app fork.

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
