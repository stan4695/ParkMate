<<<<<<< HEAD
# ParkMate — Native Android Parking Assistant

ParkMate is a native Android application (Kotlin, Jetpack Compose, Material 3, `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`) that helps drivers save where they parked manually or automatically when their car's Bluetooth disconnects, attach annotated photos and quick tags/notes, track paid parking timers with reminders, and navigate back to their parked car.

---

## 1. Setup & Build Steps

### Prerequisites
- **Android Studio** Ladybug / Meerkat (or newer) with **JDK 17+**.
- **Android SDK Platform 36** (Android 16) and Build-Tools installed via SDK Manager.
- No external API keys are required (`osmdroid` uses OpenStreetMap tiles out of the box).

### Building from the Command Line
1. Open a terminal in the project root directory (`E:\MASTER_CMOB\AN 2\SEMESTRUL 1\ASTM`).
2. Ensure `ANDROID_HOME` (or `local.properties` with `sdk.dir=...`) points to your Android SDK installation.
3. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
5. Run instrumented Room & Compose UI tests on a connected device/emulator:
   ```bash
   ./gradlew connectedDebugAndroidTest
   ```

---

## 2. Permissions Used

| Permission | Purpose | When Requested & Fallback |
|---|---|---|
| `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION` | High-accuracy GPS coordinates when saving a parking spot and live distance/bearing updates in **Guide me**. | Requested when tapping **Park here** / **Save new spot** or as Step 1 of enabling **Auto-save spot**. Falls back to last-known/default coordinates if denied. |
| `ACCESS_BACKGROUND_LOCATION` | Allows `BluetoothDisconnectReceiver` to read your location when the car Bluetooth disconnects while the app is in the background. | Requested as Step 2 (after foreground location is granted) with an in-app rationale dialog and deep link to system settings. Manual parking always works without it. |
| `BLUETOOTH_CONNECT` (API 31+) / `BLUETOOTH` (API ≤30) | Reads bonded Bluetooth devices so you can select your car's audio system and verifies `ACL_DISCONNECTED` events. | Requested as Step 3 when enabling Auto-save or opening the **Choose another device** picker in Settings. |
| `CAMERA` | Captures photos of parking pillars, levels, or entrances using CameraX. | Dedicated **Allow camera access** rationale screen (`CameraPermissionScreen`) before opening CameraX. Users can click **Not now** or pick from the system photo picker without granting camera access. |
| `POST_NOTIFICATIONS` (API 33+) | Displays 10-minute warning alerts, timer expiry alerts, and auto-saved spot notifications. | Requested when starting a paid parking timer (`+15 min` / `+30 min`) or toggling **Notifications** in Settings. |
| `SCHEDULE_EXACT_ALARM` | Fires paid parking timer alerts at the exact warning and expiry timestamps. | Checked dynamically via `AlarmManager.canScheduleExactAlarms()`; automatically falls back to `AlarmManager.setWindow()` / `setAndAllowWhileIdle()` if exact alarms are not permitted. |

---

## 3. How to Test Each Feature on a Real Device

1. **Home Map & Manual Save (`HomeScreen` -> `QuickSaveScreen`)**
   - Launch ParkMate. The map displays the active car pin, your current location dot, and a dashed walking route line.
   - Tap **Save new spot** (or **Park here** when no spot is active). Grant foreground location permission when prompted.
   - The **Spot saved** quick-save sheet appears with your reverse-geocoded street address (or coordinates if offline) and three actions: **Add photo**, **Add note**, and **Skip for now**.

2. **CameraX Capture, Gallery Picker & Photo Annotations (`CameraCaptureScreen` -> `PhotoReviewScreen`)**
   - From the quick-save sheet (or Spot Details), tap **Add photo** -> **Allow**.
   - Test **Flash toggle** (top-right), **Camera flip** (bottom-right), or **Gallery picker** (bottom-left thumbnail using the Android system photo picker).
   - Tap the shutter button to capture a photo.
   - On **Review photo**, select the **Arrow**, **Circle**, or **Text** tool in the floating toolbar and tap anywhere on the photo to place annotations. Tap **Use photo** to burn the annotations into the JPEG file in app-specific storage (`filesDir/spot_photos`).

3. **Quick Tags, Notes & Full-Screen Photo Viewer (`AddDetailsScreen` -> `SpotReadScreen` -> `PhotoViewerScreen`)**
   - Select any combination of the 8 quick tags (`Level -1`, `Level -2`, `Zone A`, `Zone B`, `Row 14`, `Pillar C4`, `Near elevator`, `Near exit`) and enter a note (up to 200 characters). Tap **Save**.
   - On **Spot details**, tap the photo carousel to open the full-screen **PhotoViewerScreen** and test pinch-to-zoom, double-tap zoom, horizontal swipe between up to 4 photos, and the **Share** button.
   - Use the top-right overflow menu (`⋮`) to **Edit** or **Delete** the spot (with confirmation dialog).

4. **Paid Parking Timer & Notifications (`PaidParkingTimerCard`)**
   - Open **Spot details** (either via the Home map bottom sheet summary or from History).
   - Tap **+15 min** or **+30 min** on the amber **Paid parking** card. Watch the circular progress ring and expiry timestamp update.
   - Toggle **Remind me (10 minutes before expiry)**. Notifications are scheduled on the `parkmate_timer_alerts` channel.

5. **Guide Me Compass & Google Maps Walking Directions (`GuideMeScreen`)**
   - Tap **Guide me** on the Home sheet or Spot details screen.
   - Rotate your phone in a figure-8 and walk toward/away from the saved spot: the compass dial arrow rotates smoothly using `Sensor.TYPE_ROTATION_VECTOR` to point toward your car, showing live distance, walking time, and cardinal bearing.
   - Tap **Open walking directions in Google Maps** to launch turn-by-turn walking navigation.

6. **Bluetooth Disconnect Auto-Save (`SettingsScreen` & `BluetoothDisconnectReceiver`)**
   - Open the **Settings** tab and tap **Auto-save spot**. Follow the 3-step sequential permission flow (Foreground Location -> Background Location "All the time" -> Nearby Devices / Bluetooth Connect).
   - Tap **Choose another device** and select your paired car Bluetooth system.
   - Turn off your car ignition (or disconnect the Bluetooth device) — ParkMate automatically captures your GPS location, reverse-geocodes the address, saves the new active spot in Room, and posts a **"Spot saved automatically"** notification.
   - You can also simulate a disconnect event at any time via `adb`:
     ```bash
     adb shell am broadcast -a com.parkmate.app.ACTION_SIMULATE_BT_DISCONNECT -n com.parkmate.app/.system.bluetooth.BluetoothDisconnectReceiver
     ```

7. **Romanian Localization & Dark Theme**
   - Switch your Android device language to **Română (Romanian)** in System Settings: all UI copy, tags, and notifications translate via `values-ro/strings.xml`.
   - Toggle Android **Dark theme** in System Display Settings to verify the dark token palette.
=======
# ParkMate
>>>>>>> 967cec606be8c6de5498ddee4a844c4f5db37654
