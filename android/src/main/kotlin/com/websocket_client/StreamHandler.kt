package com.websocket_client

import android.os.Handler

import io.flutter.plugin.common.EventChannel

class StreamHandler : EventChannel.StreamHandler {
    var handler: Handler? = null
        private set

    override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink?) {
        handler = Handler {
            eventSink?.success(it.obj)

            false
        }
    }

    override fun onCancel(arguments: Any?) {
        handler = null
    }
}