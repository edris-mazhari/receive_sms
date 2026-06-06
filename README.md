# receive_sms

A Flutter plugin that listens for incoming SMS messages on Android using a native `BroadcastReceiver`.

## Features

- Listen for incoming SMS messages in real-time via a Dart `Stream`
- Request `RECEIVE_SMS` and `READ_SMS` permissions at runtime
- Detect when permission has been permanently denied and guide users to App Settings
- Open system App Settings page for manual permission enablement
- No third-party dependencies — pure Kotlin + Dart

## Platform Support

| Android |
| :-----: |
|   ✅    |

> iOS does not allow third-party SMS access due to platform restrictions.

## Requirements

- Dart `>=3.3.0`
- Flutter `>=3.3.0`
- Android `minSdkVersion 23` (Android 6.0) or higher

## Installation

Add this to your `pubspec.yaml`:

```yaml
dependencies:
  receive_sms: ^1.0.0
```

Then run:

```sh
flutter pub get
```

No additional Android configuration is needed — the plugin handles permissions and the broadcast receiver automatically.

## Usage

```dart
import 'package:receive_sms/receive_sms.dart';

final receiveSms = ReceiveSms();

// 1. Request permission
final permission = await receiveSms.requestPermission();
if (!permission.granted) {
  if (!permission.canRequest) {
    // Permission permanently denied — open settings
    receiveSms.openAppSettings();
  }
  return;
}

// 2. Listen for incoming SMS
receiveSms.incomingSmsStream.listen((SmsMessage message) {
  print('From: ${message.address}');
  print('Body: ${message.body}');
});
```

## API

### `ReceiveSms`

| Method / Property | Returns | Description |
|-------------------|---------|-------------|
| `requestPermission()` | `Future<PermissionResult>` | Requests `RECEIVE_SMS` + `READ_SMS` permissions. Returns `PermissionResult` with `granted` and `canRequest` fields. |
| `hasPermission` | `Future<bool>` | Checks whether SMS permission is already granted. |
| `canRequestPermission` | `Future<bool>` | Checks if the system permission dialog can be shown (`shouldShowRequestPermissionRationale`). |
| `openAppSettings()` | `Future<void>` | Opens the system App Settings page for the app. |
| `incomingSmsStream` | `Stream<SmsMessage>` | A broadcast stream that emits an `SmsMessage` for every incoming SMS. |

### `SmsMessage`

| Field | Type | Description |
|-------|------|-------------|
| `address` | `String` | Sender phone number. |
| `body` | `String` | SMS message body. |
| `timestamp` | `String` | Timestamp in milliseconds as a string. |

### `PermissionResult`

| Field | Type | Description |
|-------|------|-------------|
| `granted` | `bool` | Whether the permission was granted. |
| `canRequest` | `bool` | Whether the system permission dialog can be shown (false on MIUI and when "Don't ask again" was checked). |

## Notes for Xiaomi / MIUI Devices

Xiaomi's MIUI may silently block the `RECEIVE_SMS` permission dialog. If `requestPermission()` returns `PermissionResult(granted: false, canRequest: false)`, the user must manually enable SMS permission in **Settings → Apps → Your App → Permissions**.

The `openAppSettings()` method provides a shortcut to this screen.

## License

MIT
