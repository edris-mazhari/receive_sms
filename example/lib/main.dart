import 'dart:async';
import 'package:flutter/material.dart';
import 'package:receive_sms/receive_sms.dart';

void main() {
  runApp(const ReceiveSmsExample());
}

class ReceiveSmsExample extends StatefulWidget {
  const ReceiveSmsExample({super.key});

  @override
  State<ReceiveSmsExample> createState() => _ReceiveSmsExampleState();
}

class _ReceiveSmsExampleState extends State<ReceiveSmsExample> {
  final ReceiveSms _receiveSms = ReceiveSms();
  final List<SmsMessage> _messages = [];
  StreamSubscription? _subscription;
  bool _hasPermission = false;
  bool _canRequest = true;
  bool _isListening = false;

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  Future<void> _requestPermission() async {
    final result = await _receiveSms.requestPermission();
    setState(() {
      _hasPermission = result.granted;
      _canRequest = result.canRequest;
    });
    if (result.granted) {
      _startListening();
    }
  }

  void _startListening() {
    _subscription = _receiveSms.incomingSmsStream.listen((message) {
      setState(() => _messages.insert(0, message));
    });
    setState(() => _isListening = true);
  }

  void _stopListening() {
    _subscription?.cancel();
    _subscription = null;
    setState(() => _isListening = false);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Receive SMS Example'),
          backgroundColor: Colors.green,
          foregroundColor: Colors.white,
        ),
        body: Column(
          children: [
            if (!_hasPermission)
              Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    const Icon(Icons.warning_amber,
                        size: 48, color: Colors.orange),
                    const SizedBox(height: 8),
                    const Text('SMS permission is required',
                        style: TextStyle(fontSize: 16)),
                    const SizedBox(height: 12),
                    if (_canRequest)
                      ElevatedButton.icon(
                        onPressed: _requestPermission,
                        icon: const Icon(Icons.sms),
                        label: const Text('Grant Permission'),
                      ),
                    ElevatedButton.icon(
                      onPressed: () => _receiveSms.openAppSettings(),
                      icon: const Icon(Icons.settings),
                      label: const Text('Open Settings'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.grey.shade200,
                      ),
                    ),
                  ],
                ),
              ),
            if (_hasPermission)
              Padding(
                padding: const EdgeInsets.all(8),
                child: Text(
                  _isListening ? 'Listening for SMS...' : 'Ready',
                  style: TextStyle(color: Colors.grey.shade600),
                ),
              ),
            const Divider(height: 1),
            Expanded(
              child: _messages.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.sms,
                              size: 64, color: Colors.grey.shade300),
                          const SizedBox(height: 16),
                          Text('No SMS messages received yet',
                              style:
                                  TextStyle(color: Colors.grey.shade500)),
                        ],
                      ),
                    )
                  : ListView.builder(
                      itemCount: _messages.length,
                      itemBuilder: (context, index) {
                        final msg = _messages[index];
                        return Card(
                          margin: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 4),
                          child: ListTile(
                            leading: const Icon(Icons.message,
                                color: Colors.green),
                            title: Text(msg.body),
                            subtitle: Text(
                              'From: ${msg.address}',
                              style: const TextStyle(fontSize: 11),
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
        floatingActionButton: _hasPermission
            ? FloatingActionButton(
                onPressed:
                    _isListening ? _stopListening : _startListening,
                backgroundColor:
                    _isListening ? Colors.red : Colors.green,
                child: Icon(
                  _isListening ? Icons.stop : Icons.play_arrow,
                  color: Colors.white,
                ),
              )
            : null,
      ),
    );
  }
}
