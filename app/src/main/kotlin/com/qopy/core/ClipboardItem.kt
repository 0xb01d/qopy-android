package com.qopy.core

sealed class ClipboardPayload {
    data class Text(val text: String) : ClipboardPayload()
    data class Image(val width: Int, val height: Int, val pngBytes: ByteArray) : ClipboardPayload()

    fun canonicalBytes(): ByteArray = when (this) {
        is Text -> text.toByteArray(Charsets.UTF_8)
        is Image -> pngBytes
    }
}

data class ClipboardItem(
    val eventId: String,
    val sourceDeviceId: String,
    val payload: ClipboardPayload,
    val timestampMs: Long,
    val contentHash: ByteArray,
    val isSensitive: Boolean
)
