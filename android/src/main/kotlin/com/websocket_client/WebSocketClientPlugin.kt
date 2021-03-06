package com.websocket_client

import android.content.Context
import android.os.Handler
import android.os.Looper

import androidx.annotation.NonNull

import arrow.core.Either
import arrow.core.None

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.EventChannel
import org.java_websocket.framing.PongFrame

import java.net.URI
import java.util.concurrent.TimeUnit

class WebSocketClientPlugin : FlutterPlugin, MethodCallHandler {
    private val webSockets: HashMap<String, Pair<WebSocketClient, EventChannel>> = HashMap()

    private lateinit var context: Context
    private lateinit var channel: MethodChannel
    private lateinit var messenger: BinaryMessenger

    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "websocket_client")
        channel.setMethodCallHandler(this)

        context = binding.applicationContext
        messenger = binding.binaryMessenger
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull rawResult: MethodChannel.Result) {
        val result = MethodResultWrapper(rawResult)

        when (call.method) {
            "getPlatformVersion" -> runInThread(result) { platformVersion() }
            "initialize" -> runInThread(result) { initialize() }
            "send" -> runInThread(result) { send(call) }
            "sendByte" -> runInThread(result) { sendByte(call) }
            "sendPing" -> runInThread(result) { sendPing(call) }
            "sendPong" -> runInThread(result) { sendPong(call) }
            "create" -> runInThread(result) { create(call) }
            "addHeader" -> runInThread(result) { addHeader(call) }
            "removeHeader" -> runInThread(result) { removeHeader(call) }
            "clearHeaders" -> runInThread(result) { clearHeaders(call) }
            "connect" -> runInThread(result) { connect(call) }
            "connectBlocking" -> runInThread(result) { connectBlocking(call) }
            "reconnect" -> runInThread(result) { reconnect(call) }
            "reconnectBlocking" -> runInThread(result) { reconnectBlocking(call) }
            "close" -> runInThread(result) { close(call) }
            "closeBlocking" -> runInThread(result) { closeBlocking(call) }
            else -> runInThread(result) { Either.left(Pair("-1", "Method not implemented")) }
        }
    }

    private fun runInThread(result: MethodChannel.Result, fn: () -> Either<Pair<String, String>, Any>) {
        Thread{
            when (val output = fn()) {
                is Either.Left -> result.error(output.a.first, output.a.second, null)
                is Either.Right -> when (output.b) {
                    is None -> result.success(null)
                    else -> result.success(output.b)
                }
            }
        }.start()
    }

    private fun platformVersion(): Either<Pair<String, String>, String> {
        return Either.right("Android ${android.os.Build.VERSION.RELEASE}")
    }

    private fun initialize(): Either<Pair<String, String>, None> {
        webSockets.forEach{(_, value) ->
            val (webSocket, _) = value

            if (webSocket.isOpen) {
                webSocket.cancelHandler()
                webSocket.close()
            }
        }

        webSockets.clear()

        return Either.right(None)
    }

    private fun create(call: MethodCall): Either<Pair<String, String>, String> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        if (webSockets.containsKey(name)) {
            return Either.left(Pair("-3", "Name already in use"))
        }

        val path = call.argument<String>("path")
                ?: return Either.left(Pair("-4", "Missing path parameter"))
        val httpHeaders = call.argument<Map<String, String>>("httpHeaders")

        val webSocketUri = URI.create(path)

        val streamHandler = StreamHandler()
        val webSocket = WebSocketClient(webSocketUri, streamHandler, httpHeaders = httpHeaders)

        val storePassword = call.argument<String>("storePassword")
        val keyStorePath = call.argument<String>("keyStorePath")
        val keyStoreType = call.argument<String>("keyStoreType")
        val keyPassword = call.argument<String>("keyPassword")

        val trustAllHost = call.argument<Boolean>("trustAllHost") ?: false

        if (keyStorePath?.isNotEmpty() == true && keyPassword?.isNotEmpty() == true && storePassword?.isNotEmpty() == true && keyStoreType?.isNotEmpty() == true) {
            val sslFactory = SSL.getContextFromAndroidKeystore(context, storePassword, keyPassword, keyStorePath, keyStoreType).socketFactory

            webSocket.setSocketFactory(sslFactory)
        }

        if (trustAllHost) {
            webSocket.setSocketFactory(SSL.getContext().socketFactory)
        }

        val channel = EventChannel(messenger, "better_socket/$name/event")

        channel.setStreamHandler(streamHandler)

        webSockets[name] = Pair(webSocket, channel)

        return Either.right(name)
    }

    private fun addHeader(call: MethodCall) : Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val key = call.argument<String>("key")
                ?: return Either.left(Pair("-3", "Missing key parameter"))

        val value = call.argument<String>("value")
                ?: return Either.left(Pair("-4", "Missing value parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-5", "Web socket name not found"))

        webSocket.addHeader(key, value)

        return Either.right(None)
    }

    private fun removeHeader(call: MethodCall) : Either<Pair<String, String>, String> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val key = call.argument<String>("key")
                ?: return Either.left(Pair("-3", "Missing key parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-4", "Web socket name not found"))

        val value = webSocket.removeHeader(key)

        return Either.right(value)
    }

    private fun clearHeaders(call: MethodCall) : Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-3", "Web socket name not found"))

        webSocket.clearHeaders()

        return Either.right(None)
    }

    private fun connect(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-3", "Web socket name not found"))

        webSocket.connect()

        return Either.right(None)
    }

    private fun connectBlocking(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val timeout = call.argument<Long>("timeout")

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-3", "Web socket name not found"))

        if (timeout == null) {
            webSocket.connectBlocking()
        } else {
            webSocket.connectBlocking(timeout, TimeUnit.SECONDS)
        }

        return Either.right(None)
    }

    private fun reconnect(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-3", "Web socket name not found"))

        webSocket.reconnect()

        return Either.right(None)
    }

    private fun reconnectBlocking(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-3", "Web socket name not found"))

            webSocket.reconnectBlocking()

        return Either.right(None)
    }

    private fun send(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))
        val message = call.argument<String>("message")
                ?: return Either.left(Pair("-3", "Missing message parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-4", "Web socket name not found"))

        return if (webSocket.isOpen) {
            webSocket.send(message)

            Either.right(None)
        } else {
            Either.left(Pair("-5", "Web socket is not open"))
        }
    }

    private fun sendByte(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))
        val message = call.argument<ByteArray>("message")
                ?: return Either.left(Pair("-3", "Missing message parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-4", "Web socket name not found"))

        return if (webSocket.isOpen) {
            webSocket.send(message)

            Either.right(None)
        } else {
            Either.left(Pair("-5", "Web socket is not open"))
        }
    }

    private fun sendPing(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-3", "Web socket name not found"))

        return if (webSocket.isOpen) {
            webSocket.sendPing()

            Either.right(None)
        } else {
            Either.left(Pair("-4", "Web socket is not open"))
        }
    }

    private fun sendPong(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val (webSocket, _) = webSockets[name]
                ?: return Either.left(Pair("-3", "Web socket name not found"))

        return if (webSocket.isOpen) {
            val frame = PongFrame()

            webSocket.sendFrame(frame)

            Either.right(None)
        } else {
            Either.left(Pair("-4", "Web socket is not open"))
        }
    }

    private fun close(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val (webSocket, _) = webSockets.remove(name) ?: return Either.right(None)

        if (webSocket.isOpen) {
            webSocket.close()
        }

        return Either.right(None)
    }

    private fun closeBlocking(call: MethodCall): Either<Pair<String, String>, None> {
        val name = call.argument<String>("name")
                ?: return Either.left(Pair("-2", "Missing name parameter"))

        val (webSocket, _) = webSockets.remove(name) ?: return Either.right(None)

        if (webSocket.isOpen) {
            webSocket.closeBlocking()
        }

        return Either.right(None)
    }

    private class MethodResultWrapper(private val methodResult: MethodChannel.Result) : MethodChannel.Result {
        private val handler: Handler = Handler(Looper.getMainLooper())

        override fun success(result: Any?) {
            handler.post { methodResult.success(result) }
        }

        override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
            handler.post { methodResult.error(errorCode, errorMessage, errorDetails) }
        }

        override fun notImplemented() {
            handler.post { methodResult.notImplemented() }
        }
    }
}
