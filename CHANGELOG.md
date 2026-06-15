## [1.4.0] - 2026-06-09
### Added
- `isDefaultSmsApp` getter — check if this app is the device's default SMS handler
- `requestDefaultSmsApp()` — open system dialog to request being set as default SMS app (uses `RoleManager` on Android 10+)
- `SendSmsResult` now includes `viaFallback` (`bool`) when the SMS was handled by opening the system SMS app

### Changed
- **`sendSms()` now auto-falls back to `openSmsApp()`** when direct sending fails due to device restrictions (Android 10+ OEMs). The user sees the system SMS app open with the message pre-filled instead of getting an error.
- `openSmsApp()` exposed in the Dart API (was native-only before)

### Fixed
- `getSmsManager` now uses correct API cutoff (`S`/31 instead of `M`/23) — `SmsManager.getDefault()` was deprecated in API 31, not 23
- Unused Google-specific MMS extras removed from the sent broadcast receiver
- Input validation (empty address/body) added in native `sendSms()`

## [1.3.1] - 2026-06-09
### Fixed
- Updated `PendingIntent` flags to use `FLAG_IMMUTABLE` for Android 34+ compatibility, fixing SMS send failures.
- Replaced deprecated `SmsManager.getDefault()` with `context.getSystemService(SmsManager::class.java)` on API 23+ for better Android 12+ compatibility.
- `sendSms()` now uses a `PendingIntent` with a `BroadcastReceiver` to confirm actual SMS delivery. Previously returned `success: true` without verifying the modem sent the message. Now reports real error codes (`NO_SERVICE`, `RADIO_OFF`, `NULL_PDU`).
- Permission request no longer includes unnecessary `READ_SMS` — only `RECEIVE_SMS` is requested (the plugin never reads SMS from the content provider).
- Improved `shouldShowRequestPermissionRationale()` handling for Samsung One UI, MIUI, and EMUI where the API returns inconsistent values on first request.

### Removed
- `READ_SMS` permission removed from plugin manifest and app manifest.

## 1.2.0

- **Fixed:** Simplified SEND_SMS permission request logic to avoid premature "permanently denied" handling on devices where `shouldShowRequestPermissionRationale` behaves inconsistently (e.g., Samsung One UI).
- **Fixed:** Added verification of SEND_SMS permission after the permission request callback.
- **Fixed:** `sendSmsInternal` now catches `SecurityException` and provides a clear error message with guidance.
- **Added:** New `openSmsApp` method (native and Dart) to open the default SMS app with address and body pre‑filled, serving as a reliable fallback when direct SMS sending is blocked.
- **Improved:** Error messages now explain why sending may fail on certain devices.

## 1.1.0

- **New:** Send SMS messages via `ReceiveSms.sendSms()` with automatic `SEND_SMS` permission handling
- **New:** `SendSmsResult` class with `success` and `error` fields
- **New:** Public API now includes sending capability alongside the existing receive functionality

## 1.0.1

- **Fixed:** Permission dialog now shows on first request (previously `shouldShowRequestPermissionRationale` blocked the dialog on first call)
- **Fixed:** `canRequestPermission` now correctly returns `true` on first request instead of `false`
- **Fixed:** Permission result listener now properly detects permanent denial ("never ask again")

## 1.0.0

- Initial release.
- Listen for incoming SMS messages on Android.
- Request SMS permissions at runtime.
- Open App Settings for manual permission enablement.
- Stream-based API via Dart streams.
