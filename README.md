# receive_sms

A Flutter plugin that listens for incoming SMS messages and sends SMS messages on Android using native APIs.

## Features

- Listen for incoming SMS messages in real-time via a Dart `Stream`
- Send SMS messages with real delivery confirmation (not just "no exception thrown")
- Auto-fallback to system SMS app when direct sending is restricted (Android 10+ OEMs)
- Request `RECEIVE_SMS` and `SEND_SMS` permissions at runtime
- Detect when permission has been permanently denied and guide users to App Settings
- Open system App Settings page for manual permission enablement
- Check if app is the default SMS handler and request default status
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
  receive_sms: ^1.3.1
```

Then run:

```sh
flutter pub get
```

No additional Android configuration is needed — the plugin handles permissions, sending, and the broadcast receiver automatically.

## Usage

### Receive SMS

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

### Send SMS

```dart
final result = await receiveSms.sendSms(
  address: '+1234567890',
  body: 'Hello from Flutter!',
);

if (result.success) {
  if (result.viaFallback) {
    print('System SMS app opened with message pre-filled');
  } else {
    print('SMS sent directly');
  }
} else {
  print('Failed: ${result.error}');
}
```

The plugin will automatically request `SEND_SMS` permission if it hasn't been granted yet.

On devices where direct SMS sending is restricted (some Android 10+ OEM ROMs), `sendSms()` automatically falls back to opening the system SMS app with the address and body pre-filled. The `viaFallback` field on `SendSmsResult` indicates when this fallback was used.

## API

### `ReceiveSms`

| Method / Property | Returns | Description |
|-------------------|---------|-------------|
| `requestPermission()` | `Future<PermissionResult>` | Requests `RECEIVE_SMS` permission. Returns `PermissionResult` with `granted` and `canRequest` fields. |
| `hasPermission` | `Future<bool>` | Checks whether SMS permission is already granted. |
| `canRequestPermission` | `Future<bool>` | Checks if the system permission dialog can be shown (returns `false` when permanently denied). |
| `openAppSettings()` | `Future<void>` | Opens the system App Settings page for the app. |
| `incomingSmsStream` | `Stream<SmsMessage>` | A broadcast stream that emits an `SmsMessage` for every incoming SMS. |
| `sendSms({address, body})` | `Future<SendSmsResult>` | Sends an SMS with delivery confirmation. Automatically falls back to `openSmsApp()` when direct sending is restricted. |
| `openSmsApp({address, body})` | `Future<bool>` | Opens the default SMS app with address and body pre-filled. |
| `isDefaultSmsApp` | `Future<bool>` | Checks whether this app is the device's default SMS handler. |
| `requestDefaultSmsApp()` | `Future<bool>` | Opens system dialog to set this app as the default SMS handler. |

### `SmsMessage`

| Field | Type | Description |
|-------|------|-------------|
| `address` | `String` | Sender phone number (incoming) or recipient (outgoing). |
| `body` | `String` | SMS message body. |
| `timestamp` | `String` | Timestamp in milliseconds as a string. |

### `PermissionResult`

| Field | Type | Description |
|-------|------|-------------|
| `granted` | `bool` | Whether the permission was granted. |
| `canRequest` | `bool` | Whether the system permission dialog can be shown (false on MIUI and when "Don't ask again" was checked). |

### `SendSmsResult`

| Field | Type | Description |
|-------|------|-------------|
| `success` | `bool` | Whether the SMS was sent or the system SMS app was opened as fallback. |
| `error` | `String?` | Error message if sending failed and no fallback was possible. |
| `viaFallback` | `bool?` | `true` when the system SMS app was opened as fallback instead of direct sending. |

## Notes for Xiaomi / MIUI & Other Devices

Some OEMs (Xiaomi MIUI, Huawei EMUI, etc.) may suppress or block the runtime permission dialog. If `requestPermission()` returns `PermissionResult(granted: false, canRequest: false)`, the user must manually enable SMS permission in **Settings → Apps → Your App → Permissions**.

The `openAppSettings()` method provides a shortcut to this screen.

## License

MIT
