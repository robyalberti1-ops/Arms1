package com.arms.ke3

import java.nio.charset.Charset
import java.util.UUID

object Ke3Protocol {
    val SERVICE_56FF: UUID = UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb")
    val CHAR_34F1_WRITE: UUID = UUID.fromString("000034f1-0000-1000-8000-00805f9b34fb")
    val CHAR_34F2_NOTIFY: UUID = UUID.fromString("000034f2-0000-1000-8000-00805f9b34fb")
    val BATTERY_SERVICE: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    private val utf16be: Charset = Charset.forName("UTF-16BE")

    fun buildNotificationPackets(text: String): List<ByteArray> {
        val clean = text
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(220)
        val bytes = clean.toByteArray(utf16be)
        val maxPayload = 16 // 4 header + 16 payload = 20 byte BLE default MTU
        val packets = mutableListOf<ByteArray>()
        var index = 0
        var offset = 0
        while (offset < bytes.size && index < 0xFD) {
            val len = minOf(maxPayload, bytes.size - offset)
            val payload = bytes.copyOfRange(offset, offset + len)
            packets += byteArrayOf(0xC5.toByte(), index.toByte(), 0x07, 0x0C) + payload
            offset += len
            index++
        }
        packets += byteArrayOf(0xC5.toByte(), 0xFD.toByte())
        packets += byteArrayOf(0xAB.toByte(), 0x00, 0x00, 0x00, 0x01, 0x01, 0x07, 0x01)
        return packets
    }

    fun bytesToHex(value: ByteArray): String = value.joinToString("-") { "%02X".format(it) }
}
