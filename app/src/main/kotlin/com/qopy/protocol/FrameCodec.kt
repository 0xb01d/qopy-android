package com.qopy.protocol

import com.qopy.proto.Qopy.Envelope
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object FrameCodec {
    const val MAX_CONTENT_BYTES = 10 * 1024 * 1024 // 10 MiB
    const val MAX_FRAME_BYTES = MAX_CONTENT_BYTES + 64 * 1024 // 10 MiB + 64 KiB header overhead

    fun encodeFrame(envelope: Envelope): ByteArray {
        val payloadBytes = envelope.toByteArray()
        if (payloadBytes.size > MAX_FRAME_BYTES) {
            throw IllegalArgumentException("Payload size ${payloadBytes.size} exceeds maximum allowed frame size $MAX_FRAME_BYTES")
        }

        val buffer = ByteBuffer.allocate(4 + payloadBytes.size)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(payloadBytes.size)
        buffer.put(payloadBytes)
        return buffer.array()
    }

    fun writeFrame(out: OutputStream, envelope: Envelope) {
        val frame = encodeFrame(envelope)
        out.write(frame)
        out.flush()
    }

    fun readFrame(input: InputStream): Envelope? {
        val lengthHeader = ByteArray(4)
        var totalRead = 0
        while (totalRead < 4) {
            val read = input.read(lengthHeader, totalRead, 4 - totalRead)
            if (read == -1) {
                if (totalRead == 0) return null
                throw IllegalStateException("Unexpected EOF while reading frame length header")
            }
            totalRead += read
        }

        val lengthBuf = ByteBuffer.wrap(lengthHeader).order(ByteOrder.BIG_ENDIAN)
        val payloadLen = lengthBuf.getInt()

        if (payloadLen <= 0) {
            throw IllegalStateException("Invalid empty or negative frame length: $payloadLen")
        }
        if (payloadLen > MAX_FRAME_BYTES) {
            throw IllegalStateException("Frame length $payloadLen exceeds maximum limit $MAX_FRAME_BYTES")
        }

        val payload = ByteArray(payloadLen)
        var payloadRead = 0
        while (payloadRead < payloadLen) {
            val read = input.read(payload, payloadRead, payloadLen - payloadRead)
            if (read == -1) {
                throw IllegalStateException("Unexpected EOF while reading frame body")
            }
            payloadRead += read
        }

        return Envelope.parseFrom(payload)
    }
}
