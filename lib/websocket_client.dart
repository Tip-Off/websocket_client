import 'dart:async';

import 'package:flutter/services.dart';

enum Event { on_open, on_close, on_error, on_message }

class WebsocketClient {
  static const MethodChannel _channel = const MethodChannel('websocket_client');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');

    return version;
  }

  static Future<String> create(String name, String path,
      {Map<String, String> httpHeaders, bool trustAllHost, String keyStorePath, String keyPassword, String storePassword, String keyStoreType}) async {
    return await _channel.invokeMethod('create', {
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

  static Future<void> connect(String name) async {
    await _channel.invokeMethod('connect', {'name': name});
  }

  static Future<bool> send(String name, String message) async {
    return await _channel.invokeMethod('send', {'name': name, 'message': message});
  }

  // static sendByteMsg(Uint8List msg) {
  //   _channel.invokeMethod('sendByteMsg', <String, Uint8List>{'msg': msg});
  // }

  static Future<bool> close(String name) async {
    return await _channel.invokeMethod('close', {'name': name});
  }

  static void addListeners(String name, {Function onOpen, Function onMessage, Function onError, Function onClose}) {
    EventChannel eventChannel = EventChannel("better_socket/$name/event");

    eventChannel.receiveBroadcastStream().listen((data) {
      var event = data["event"];
      print(event);

      if (event == Event.on_open.index && onOpen != null) {
        var httpStatus = data["httpStatus"];
        var httpStatusMessage = data["httpStatusMessage"];

        onOpen(httpStatus, httpStatusMessage);
      } else if (event == Event.on_close.index && onClose != null) {
        var code = data["code"];
        var reason = data["reason"];
        var remote = data["remote"];

        onClose(code, reason, remote);
      } else if (event == Event.on_message.index && onMessage != null) {
        var message = data["message"];
        onMessage(message);
      } else if (event == Event.on_error.index && onError != null) {
        var message = data["message"];
        onError(message);
      }
    });
  }
}
