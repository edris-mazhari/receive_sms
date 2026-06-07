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
