package com.pebblentn.app.pebble

import com.pebblentn.app.protocol.AppMessage
import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem

/**
 * Converts between the transport-agnostic [AppMessage] and PebbleKit Android 2's [PebbleDictionary]
 * (a map of unsigned key to a typed [PebbleDictionaryItem]). Integer values are sent as `Int32`
 * (our protocol keys are int32/string tuples); inbound integers of any width are normalized back to
 * a plain [Int].
 */
object PebbleAppMessageMapper {

    fun toDictionary(message: AppMessage): PebbleDictionary {
        val dict = LinkedHashMap<UInt, PebbleDictionaryItem>()
        for ((key, value) in message.fields) {
            dict[key.toUInt()] = when (value) {
                is AppMessage.Value.IntValue -> PebbleDictionaryItem.Int32(value.value)
                is AppMessage.Value.StrValue -> PebbleDictionaryItem.Text(value.value)
            }
        }
        return dict
    }

    fun fromDictionary(dict: PebbleDictionary): AppMessage {
        val fields = LinkedHashMap<Int, AppMessage.Value>()
        for ((key, item) in dict) {
            val value: AppMessage.Value? = when (item) {
                is PebbleDictionaryItem.Int8 -> AppMessage.Value.IntValue(item.value.toInt())
                is PebbleDictionaryItem.UInt8 -> AppMessage.Value.IntValue(item.value.toInt())
                is PebbleDictionaryItem.Int16 -> AppMessage.Value.IntValue(item.value.toInt())
                is PebbleDictionaryItem.UInt16 -> AppMessage.Value.IntValue(item.value.toInt())
                is PebbleDictionaryItem.Int32 -> AppMessage.Value.IntValue(item.value)
                is PebbleDictionaryItem.UInt32 -> AppMessage.Value.IntValue(item.value.toInt())
                is PebbleDictionaryItem.Text -> AppMessage.Value.StrValue(item.value)
                is PebbleDictionaryItem.Bytes -> null // no byte-array tuples in this protocol
            }
            if (value != null) fields[key.toInt()] = value
        }
        return AppMessage(fields)
    }
}
