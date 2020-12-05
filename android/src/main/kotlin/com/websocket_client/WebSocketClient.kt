package com.websocket_client

import android.os.Message
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.framing.Framedata
import org.java_websocket.framing.PongFrame
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer

class WebSocketClient @JvmOverloads
constructor(serverUri: URI, private var streamHandler: StreamHandler, protocolDraft: Draft = Draft_6455(), httpHeaders: Map<String, String>? = null, connectTimeout: Int = 0) : WebSocketClient(serverUri, protocolDraft, httpHeaders, connectTimeout) {
    override fun onOpen(handshakeData: ServerHandshake?) {
        val eventResult = HashMap<String, Any>()

        eventResult["event"] = Event.ON_OPEN.ordinal
        eventResult["httpStatus"] = handshakeData?.httpStatus.toString()
        eventResult["httpStatusMessage"] = handshakeData?.httpStatusMessage.toString()

        val m = Message()
        eventResult.also { m.obj = it }

        streamHandler.handler?.sendMessage(m)
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        val eventResult = HashMap<String, Any>()

        eventResult["event"] = Event.ON_CLOSE.ordinal
        eventResult["code"] = code
        eventResult["reason"] = reason.toString()
        eventResult["remote"] = remote

        val m = Message()
        eventResult.also { m.obj = it }

        streamHandler.handler?.sendMessage(m)
    }

    override fun onMessage(message: String?) {
        val eventResult = HashMap<String, Any>()
        eventResult["event"] = Event.ON_MESSAGE.ordinal
        eventResult["message"] = message.toString()

        val m = Message()
        eventResult.also { m.obj = it }

        streamHandler.handler?.sendMessage(m)
    }

    override fun onMessage(bytes: ByteBuffer?) {
        val eventResult = HashMap<String, Any>()
        eventResult["event"] = Event.ON_MESSAGE.ordinal
        eventResult["message"] = bytes?.array()!!

        val m = Message()
        eventResult.also { m.obj = it }

        streamHandler.handler?.sendMessage(m)
    }

    override fun onError(ex: Exception?) {
        val eventResult = HashMap<String, Any>()
        eventResult["event"] = Event.ON_ERROR.ordinal
        eventResult["message"] = ex?.message.toString()

        val m = Message()
        eventResult.also { m.obj = it }

        streamHandler.handler?.sendMessage(m)
    }

    override fun onWebsocketPing(conn: WebSocket?, frameData: Framedata?) {
        val f = PongFrame()
        f.setPayload(frameData?.payloadData)
        conn?.sendFrame(f)
    }
}