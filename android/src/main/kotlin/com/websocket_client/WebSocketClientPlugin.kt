package com.websocket_client

import android.content.Context

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.EventChannel

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth

import java.net.URI

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

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        val output = when (call.method) {
          "getPlatformVersion" -> platformVersion()
          "create" -> create(call)
          "connect" -> connect(call)
          "send" -> send(call)
          "close" -> close(call)
            else -> Err(Pair("-1", "Method not implemented"))
        }

        output.mapBoth(
                { value -> result.success(value) },
                { error -> result.error(error.first, error.second, null) }
        )
    }

    private fun platformVersion(): Result<String, Pair<String, String>> {
        return Ok("Android ${android.os.Build.VERSION.RELEASE}")
    }

    private fun create(call: MethodCall): Result<String, Pair<String, String>> {
        val name = call.argument<String>("name") ?: return Err(Pair("-2", "Missing name parameter"))

        if (webSockets.containsKey(name)) {
            return Err(Pair("-3", "Name already in use"))
        }

        val path = call.argument<String>("path") ?: return Err(Pair("-4", "Missing path parameter"))
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

        return Ok(name)
    }

    private fun connect(call: MethodCall): Result<String, Pair<String, String>> {
        val name = call.argument<String>("name") ?: return Err(Pair("-2", "Missing name parameter"))

      val (webSocket, _) = webSockets[name] ?: return Err(Pair("-3", "Web socket name not found"))

        webSocket.connect()

        return Ok("")
    }

    private fun send(call: MethodCall): Result<String, Pair<String, String>> {
        val name = call.argument<String>("name") ?: return Err(Pair("-2", "Missing name parameter"))
        val message = call.argument<String>("message")
                ?: return Err(Pair("-3", "Missing message parameter"))

      val (webSocket, _) = webSockets[name] ?: return Err(Pair("-4", "Web socket name not found"))

        return if (webSocket.isOpen) {
            webSocket.send(message)

            return Ok("ok")
        } else {
            return Err(Pair("-5", "Web socket is not open"))
        }
    }

    private fun close(call: MethodCall): Result<String, Pair<String, String>> {
        val name = call.argument<String>("name") ?: return Err(Pair("-2", "Missing name parameter"))

      val (webSocket, _) = webSockets.remove(name) ?: return Ok("ok")

        if (webSocket.isOpen) {
            webSocket.close()
        }

        return Ok("ok")
    }
}
