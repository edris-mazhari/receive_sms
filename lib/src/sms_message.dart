class SmsMessage {
  final String address;
  final String body;
  final String timestamp;

  const SmsMessage({
    required this.address,
    required this.body,
    required this.timestamp,
  });

  factory SmsMessage.fromMap(Map<String, dynamic> map) {
    return SmsMessage(
      address: map['address'] as String? ?? '',
      body: map['body'] as String? ?? '',
      timestamp: map['timestamp'] as String? ?? '',
    );
  }

  @override
  String toString() =>
      'SmsMessage(address: $address, body: $body, timestamp: $timestamp)';
}
