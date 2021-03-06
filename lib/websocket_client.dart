import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:websocket_client/listeners_builder.dart';

final MethodChannel channel = const MethodChannel('websocket_client')..invokeMethod<void>('initialize');

class WebsocketClient {
  static Future<String> get platformVersion async {
    final String version = await channel.invokeMethod('getPlatformVersion');

    return version;
  }

  static Future<String> create(String name, String path,
      {Map<String, String>? httpHeaders, bool? trustAllHost, String? keyStorePath, String? keyPassword, String? storePassword, String? keyStoreType}) async {
    return await channel.invokeMethod('create', {
      'name': name,
      'path': path,
      'httpHeaders': httpHeaders,
      "keyStorePath": keyStorePath,
      "trustAllHost": trustAllHost,
      "keyPassword": keyPassword,
      "storePassword": storePassword,
      "keyStoreType": keyStoreType
    });
  }

  static Future<void> addHeader(String name, String key, String value) async {
    await channel.invokeMethod('addHeader', {'name': name, 'key': key, 'value': value});
  }

  static Future<String> removeHeader(String name, String key) async {
    return await channel.invokeMethod('removeHeader', {'name': name, 'key': key});
  }

  static Future<void> clearHeaders(String name) async {
    await channel.invokeMethod('clearHeaders', {'name': name});
  }

  static Future<void> connect(String name) async {
    await channel.invokeMethod('connect', {'name': name});
  }

  static Future<void> connectBlocking(String name) async {
    await channel.invokeMethod('connectBlocking', {'name': name});
  }

  static Future<void> reconnect(String name) async {
    await channel.invokeMethod('reconnect', {'name': name});
  }

  static Future<void> reconnectBlocking(String name) async {
    await channel.invokeMethod('reconnectBlocking', {'name': name});
  }

  static Future<void> send(String name, String message) async {
    await channel.invokeMethod('send', {'name': name, 'message': message});
  }

  static Future<void> sendByte(String name, Uint8List message) async {
    await channel.invokeMethod('send', {'name': name, 'message': message});
  }

  static Future<void> sendPing(String name) async {
    await channel.invokeMethod('sendPing', {'name': name});
  }

  static Future<void> sendPong(String name) async {
    await channel.invokeMethod('sendPong', {'name': name});
  }

  static Future<void> close(String name) async {
    await channel.invokeMethod('close', {'name': name});
  }

  static Future<void> closeBlocking(String name) async {
    await channel.invokeMethod('closeBlocking', {'name': name});
  }

  static void addListeners(String name, ListenersBuilder builder) {
    var eventChannel = EventChannel("better_socket/$name/event");

    eventChannel.receiveBroadcastStream().listen(builder.build());
  }
}
