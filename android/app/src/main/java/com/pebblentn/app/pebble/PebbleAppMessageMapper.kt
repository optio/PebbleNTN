package com.pebblentn.app.pebble

import com.getpebble.android.kit.util.PebbleDictionary
import com.pebblentn.app.protocol.AppMessage

/** Converts between the transport-agnostic [AppMessage] and PebbleKit's [PebbleDictionary]. */
object PebbleAppMessageMapper {

    fun toDictionary(message: AppMessage): PebbleDictionary {
        val dict = PebbleDictionary()
        for ((key, value) in message.fields) {
            when (value) {
                is AppMessage.Value.IntValue -> dict.addInt32(key, value.value)
                is AppMessage.Value.StrValue -> dict.addString(key, value.value)
            }
        }
        return dict
    }

    fun fromDictionary(dict: PebbleDictionary): AppMessage {
        val fields = LinkedHashMap<Int, AppMessage.Value>()
        for (tuple in dict) {
            val key = tuple.key.toInt()
            when (val value = tuple.value) {
                is Long -> fields[key] = AppMessage.Value.IntValue(value.toInt())
                is Int -> fields[key] = AppMessage.Value.IntValue(value)
                is String -> fields[key] = AppMessage.Value.StrValue(value)
                else -> Unit // ignore unsupported tuple types
            }
        }
        return AppMessage(fields)
    }
}
