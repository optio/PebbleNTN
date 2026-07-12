package com.pebblentn.app.protocol

/**
 * A transport-agnostic AppMessage payload: a map of numeric protocol key to a typed value.
 *
 * This mirrors a Pebble AppMessage dictionary (int32 and UTF-8 string tuples) without depending on
 * any Pebble/Android type, so the codec and reducer stay pure and unit-testable. The real
 * [WatchTransport] implementation (M7) converts between this and the SDK dictionary.
 */
data class AppMessage(val fields: Map<Int, Value>) {

    sealed interface Value {
        data class IntValue(val value: Int) : Value
        data class StrValue(val value: String) : Value
    }

    fun intOrNull(key: Int): Int? = (fields[key] as? Value.IntValue)?.value
    fun stringOrNull(key: Int): String? = (fields[key] as? Value.StrValue)?.value

    /** Builder that ignores null values so optional fields are simply omitted. */
    class Builder {
        private val fields = LinkedHashMap<Int, Value>()

        fun putInt(key: Int, value: Int?): Builder {
            if (value != null) fields[key] = Value.IntValue(value)
            return this
        }

        fun putString(key: Int, value: String?): Builder {
            if (value != null) fields[key] = Value.StrValue(value)
            return this
        }

        fun build(): AppMessage = AppMessage(fields.toMap())
    }

    companion object {
        fun builder(): Builder = Builder()
    }
}
