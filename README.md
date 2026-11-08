# RamGuard — RAM Cleaner + Background Killer (Kotlin / Compose / MVVM)

Open the folder in Android Studio (Hedgehog+), let it sync (it generates the Gradle wrapper), run on a device.

## Architecture
- `data/` SettingsRepository (DataStore: white list, mode, auto-clean, unlock window), AppRepository, CleanerEngine (sweep / RAM info / DND)
- `service/` BootReceiver -> CleanerService (foreground, dynamic SCREEN_OFF receiver) + NotificationBlockerService
- `ui/` MainViewModel (StateFlow) + Compose screens: Home, Running (search + web search), White list, Modes
- `res/` AMOLED theme, EN/AR strings, adaptive icon (shield + bolt + RAM pins, with Android 13 monochrome layer)

## Special permissions the user must grant (Home screen shows shortcuts)
1. Usage access (PACKAGE_USAGE_STATS) -> last-used times for sorting
2. Notification access -> Eco/Ultra notification filtering
3. Do Not Disturb access -> Ultra mode (alarms only)
4. Recommended: Settings > Battery > exclude RamGuard from optimization, and on Samsung "Never sleeping apps" so the service survives.

## Real platform limits (Android 13 / One UI)
- Non-root apps can only call `killBackgroundProcesses()` on cached/background processes. Apps may restart themselves.
- Android has no API to "free RAM" globally; the freed amount is whatever the kernel reclaims after the kills.
- `getRunningAppProcesses()` returns only your own app, so the Running list uses a heuristic (launchable apps not in the "stopped" state).
- **Data blocking (Ultra mode, opt-in switch):** `FirewallVpnService` is a local black-hole VPN. Only non-whitelisted launchable apps are routed into it and every packet is dropped; white-listed/protected apps, Google Play services and non-launcher system services stay online. The 5-minute "allow communication" button reopens the internet.
  Caveats: Android allows only ONE active VPN (this replaces the user's real VPN); apps not in the launcher are not blocked; it blocks foreground use too, so use the unlock button when you need the web.
- Stronger per-app limits via ADB: `adb shell cmd appops set <pkg> RUN_ANY_IN_BACKGROUND ignore`.

## Icon
Vector adaptive icon is already included. For a Play Store PNG: Android Studio > right-click res > New > Image Asset > use `ic_launcher_foreground`.
