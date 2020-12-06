import 'dart:collection';

import 'package:websocket_client/event.dart';

class ListenersBuilder {
  void Function(String message) onMessage;
  void Function() onPing;
  void Function() onPong;
  void Function(String httpStatus, String httpsStatusMessage) onOpen;
  void Function(int code, String reason, String remote) onClose;
  void Function(String error) onError;

  void addOnOpen(void Function(String httpStatus, String httpsStatusMessage) onOpen) {
    this.onOpen = onOpen;
  }

  void addOnClose(void Function(int code, String reason, String remote) onClose) {
    this.onClose = onClose;
  }

  void addOnError(void Function(String error) onError) {
    this.onError = onError;
  }

  void addOnMessage(void Function(String message) onMessage) {
    this.onMessage = onMessage;
  }

  void addOnPing(void Function() onPing) {
    this.onPing = onPing;
  }

  void addOnPong(void Function() onPong) {
    this.onPong = onPong;
  }

  Function build() {
    return (data) {
      var event = Event.values[data["event"]];

      switch (event) {
        case Event.on_message:
          handleOnMessage(data);
          break;
        case Event.on_ping:
          handleOnPing();
          break;
        case Event.on_pong:
          handleOnPong();
          break;
        case Event.on_open:
          handleOnOpen(data);
          break;
        case Event.on_close:
          handleOnClose(data);
          break;
        case Event.on_error:
          handleOnError(data);
          break;
      }
    };
  }

  void handleOnMessage(LinkedHashMap<dynamic, dynamic> data) {
    if (onMessage != null) {
      final message = data["message"];

      onMessage(message);
    }
  }

  void handleOnPing() {
    if (onPing != null) {
      onPing();
    }
  }

  void handleOnPong() {
    if (onPong != null) {
      onPong();
    }
  }

  void handleOnOpen(LinkedHashMap<dynamic, dynamic> data) {
    if (onOpen != null) {
      var httpStatus = data["httpStatus"];
      var httpStatusMessage = data["httpStatusMessage"];

      onOpen(httpStatus, httpStatusMessage);
    }
  }

  void handleOnClose(LinkedHashMap<dynamic, dynamic> data) {
    if (onClose != null) {
      var code = data["code"];
      var reason = data["reason"];
      var remote = data["remote"];

      onClose(code, reason, remote);
    }
  }

  void handleOnError(LinkedHashMap<dynamic, dynamic> data) {
    if (onError != null) {
      final error = data["error"];

      onError(error);
    }
  }
}
