package com.mrtdk.liquid_glass.listentogether

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

class MessageCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Suppress("UNCHECKED_CAST")
    fun encode(msgType: String, payload: Any?): String {
        val payloadElement: JsonElement? = payload?.let {
            val serializer = serializer(it::class.java) as kotlinx.serialization.KSerializer<Any>
            json.encodeToJsonElement(serializer, it)
        }
        val msg = Message(type = msgType, payload = payloadElement)
        return json.encodeToString(Message.serializer(), msg)
    }

    fun decode(jsonString: String): Message {
        return json.decodeFromString(Message.serializer(), jsonString)
    }

    fun <T> decodePayload(payload: JsonElement, deserializer: DeserializationStrategy<T>): T {
        return json.decodeFromJsonElement(deserializer, payload)
    }

    fun jsonInstance(): Json = json
}
