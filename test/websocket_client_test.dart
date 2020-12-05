import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:websocket_client/websocket_client.dart';

void main() {
  const MethodChannel channel = MethodChannel('websocket_client');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await WebsocketClient.platformVersion, '42');
  });
}
