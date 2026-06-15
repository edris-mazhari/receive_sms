export 'src/sms_message.dart';

import 'dart:async';
import 'package:flutter/services.dart';
import 'src/sms_message.dart';

class PermissionResult {
  final bool granted;
  final bool canRequest;

  const PermissionResult({
    required this.granted,
    required this.canRequest,
  });
}

class SendSmsResult {
  final bool success;
  final String? error;

  const SendSmsResult({required this.success, this.error});
}

class ReceiveSms {
  static final ReceiveSms _instance = ReceiveSms._();
  factory ReceiveSms() => _instance;
  ReceiveSms._();

  static const EventChannel _smsEventChannel =
      EventChannel('com.greenhouse.greenhouse/sms_events');
  static const MethodChannel _smsChannel =
      MethodChannel('com.greenhouse.greenhouse/permission');

  Stream<SmsMessage>? _smsStream;

  Stream<SmsMessage> get incomingSmsStream {
    _smsStream ??= _smsEventChannel
        .receiveBroadcastStream()
        .map((data) => SmsMessage.fromMap(Map<String, dynamic>.from(data as Map)));
    return _smsStream!;
  }

  Future<PermissionResult> requestPermission() async {
    final result =
        await _smsChannel.invokeMethod<Map>('requestSmsPermission');
    return PermissionResult(
      granted: result?['granted'] as bool? ?? false,
      canRequest: result?['canRequest'] as bool? ?? true,
    );
  }

  Future<bool> get hasPermission async {
    final result = await requestPermission();
    return result.granted;
  }

  Future<bool> get canRequestPermission async {
    final result =
        await _smsChannel.invokeMethod<bool>('canRequestPermission');
    return result ?? true;
  }

  Future<void> openAppSettings() async {
    await _smsChannel.invokeMethod('openAppSettings');
  }

  Future<SendSmsResult> sendSms({
    required String address,
    required String body,
  }) async {
    final result = await _smsChannel.invokeMethod<Map>('sendSms', {
      'address': address,
      'body': body,
    });
    final map = Map<String, dynamic>.from(result as Map);
    return SendSmsResult(
      success: map['success'] as bool? ?? false,
      error: map['error'] as String?,
    );
  }

  Future<bool> openSmsApp({
    required String address,
    required String body,
  }) async {
    final result = await _smsChannel.invokeMethod<bool>('openSmsApp', {
      'address': address,
      'body': body,
    });
    return result ?? false;
  }

  Future<bool> get isDefaultSmsApp async {
    final result = await _smsChannel.invokeMethod<bool>('isDefaultSmsApp');
    return result ?? false;
  }

  Future<bool> requestDefaultSmsApp() async {
    final result = await _smsChannel.invokeMethod<bool>('requestDefaultSmsApp');
    return result ?? false;
  }
}
